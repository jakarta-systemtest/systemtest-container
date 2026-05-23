package io.systemtest.container.cases.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.config.LifecycleFailureMode;
import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.extension.SystemTestExtension;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Test support type for SystemTestExtensionSystemTest.
 *
 * @author Mustapha Zouari
 */
class SystemTestExtensionSystemTest {

  @BeforeEach
  void resetRecorder() {
    Recorder.clear();
  }

  @Test
  void runsPostConstructOnResolvedContextBeforeTestMethod() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(PostConstructTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));
  }

  @Test
  void runsPostConstructAfterBeforeEachMockSetup() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(SessionInitializationTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));

    assertThat(Recorder.events())
        .containsExactly("beforeEach", "postConstruct", "test", "afterEach", "preDestroy");
  }

  @Test
  void runsPreDestroyAfterTestMethod() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(PreDestroyTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));

    assertThat(Recorder.events()).containsExactly("post", "test", "destroy");
  }

  @Test
  void runsBeanHooksInRegistrationOrderAndDestroyHooksInReverseOrder() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(OrderingTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));

    assertThat(Recorder.events())
        .containsExactly("child-post", "root-post", "test", "root-destroy", "child-destroy");
  }

  @Test
  void doesNotRunLifecycleHooksOnExplicitMocks() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(MockLifecycleTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));

    assertThat(Recorder.events()).containsExactly("root-post", "test", "root-destroy");
  }

  @Test
  void runsLifecycleHooksOnceForSharedSpiesInMultiRootContext() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(MultiRootLifecycleTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));

    assertThat(Recorder.events())
        .containsExactly(
            "shared-post",
            "first-post",
            "second-post",
            "test",
            "second-destroy",
            "first-destroy",
            "shared-destroy");
  }

  @Test
  void directBuilderUsageDoesNotRunLifecycleHooks() {
    new SystemTestContextBuilder(PreDestroyRoot.class).build();

    assertThat(Recorder.events()).isEmpty();
  }

  @Test
  void rejectsNullContext() {
    assertThatNullPointerException()
        .isThrownBy(() -> new SystemTestExtension(null))
        .withMessage("context");
  }

  @Test
  void strictLifecycleFailureFailsTestByDefault() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(StrictLifecycleFailureTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(0).failed(1));
  }

  @Test
  void lenientLifecycleFailureIsLoggedAndRecordedInDebugDetails() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(LenientLifecycleFailureTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(1).failed(0));

    assertThat(Recorder.events()).containsExactly("postConstruct", "test", "preDestroy");
    assertThat(LenientLifecycleFailureTest.context.debugDetails().errors())
        .hasSize(2)
        .extracting(error -> error.message())
        .containsExactly(
            "Lifecycle method initialize failed on " + FailingLifecycleRoot.class.getName(),
            "Lifecycle method destroy failed on " + FailingLifecycleRoot.class.getName());
  }

  @Test
  void failsClearlyWhenLifecycleSignatureIsInvalid() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(InvalidLifecycleSignatureTest.class))
        .execute()
        .testEvents()
        .assertStatistics(statistics -> statistics.started(1).succeeded(0).failed(1));
  }

  static class SessionInitializationTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(UserSessionInitializer.class)
            .mock(SessionGateway.class)
            .build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @BeforeEach
    void configureSessionGateway() {
      when(context.mock(SessionGateway.class).getSessionInfo())
          .thenReturn(new SessionInfo("session-42", "zoe"));
      Recorder.add("beforeEach");
    }

    @Test
    void initializesUserSessionFromStubbedGateway() {
      assertThat(context.spy(UserSessionInitializer.class).sessionInfo())
          .isEqualTo(new SessionInfo("session-42", "zoe"));
      Recorder.add("test");
    }

    @org.junit.jupiter.api.AfterEach
    void recordAfterEach() {
      Recorder.add("afterEach");
    }
  }

  static class UserSessionInitializer {
    private final SessionGateway sessionGateway;
    private SessionInfo sessionInfo;

    @Inject
    UserSessionInitializer(SessionGateway sessionGateway) {
      this.sessionGateway = sessionGateway;
    }

    @PostConstruct
    void initializeSession() {
      sessionInfo = sessionGateway.getSessionInfo();
      Recorder.add("postConstruct");
    }

    @PreDestroy
    void destroySession() {
      Recorder.add("preDestroy");
    }

    SessionInfo sessionInfo() {
      return sessionInfo;
    }
  }

  interface SessionGateway {
    SessionInfo getSessionInfo();
  }

  record SessionInfo(String id, String username) {}

  static class StrictLifecycleFailureTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(FailingLifecycleRoot.class).build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void testMethod() {
      Recorder.add("test");
    }
  }

  static class LenientLifecycleFailureTest {
    private static SystemTestContext context;

    private final SystemTestContext testContext = createContext();

    @RegisterExtension
    private final SystemTestExtension systemTest =
        new SystemTestExtension(testContext, LifecycleFailureMode.LENIENT);

    @Test
    void testMethod() {
      Recorder.add("test");
    }

    private static SystemTestContext createContext() {
      context = new SystemTestContextBuilder(FailingLifecycleRoot.class).build();
      return context;
    }
  }

  static class FailingLifecycleRoot {
    @PostConstruct
    void initialize() {
      Recorder.add("postConstruct");
      throw new IllegalStateException("init not needed in this system test");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("preDestroy");
      throw new IllegalStateException("destroy not needed in this system test");
    }
  }

  static class PostConstructTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(PostConstructRoot.class).build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void postConstructAlreadyRan() {
      assertThat(context.spy(PostConstructRoot.class).initialized).isTrue();
    }
  }

  static class PostConstructRoot {
    private boolean initialized;

    @PostConstruct
    void initialize() {
      initialized = true;
    }
  }

  static class PreDestroyTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(PreDestroyRoot.class).build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void testMethod() {
      context.spy(PreDestroyRoot.class);
      Recorder.add("test");
    }
  }

  static class PreDestroyRoot {
    @PostConstruct
    void initialize() {
      Recorder.add("post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("destroy");
    }
  }

  static class OrderingTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(OrderingRoot.class).build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void testMethod() {
      context.spy(OrderingRoot.class);
      Recorder.add("test");
    }
  }

  static class OrderingRoot {
    private final OrderingChild child;

    @Inject
    OrderingRoot(OrderingChild child) {
      this.child = child;
    }

    @PostConstruct
    void initialize() {
      assertThat(child).isNotNull();
      Recorder.add("root-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("root-destroy");
    }
  }

  static class OrderingChild {
    @PostConstruct
    void initialize() {
      Recorder.add("child-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("child-destroy");
    }
  }

  static class MockLifecycleTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(MockLifecycleRoot.class)
            .mock(MockLifecycleDependency.class)
            .build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void testMethod() {
      context.spy(MockLifecycleRoot.class);
      Recorder.add("test");
    }
  }

  static class MockLifecycleRoot {
    @Inject MockLifecycleDependency dependency;

    @PostConstruct
    void initialize() {
      assertThat(dependency).isNotNull();
      Recorder.add("root-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("root-destroy");
    }
  }

  static class MockLifecycleDependency {
    @PostConstruct
    void initialize() {
      Recorder.add("mock-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("mock-destroy");
    }
  }

  static class MultiRootLifecycleTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(FirstLifecycleRoot.class, SecondLifecycleRoot.class).build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void testMethod() {
      assertThat(context.spy(FirstLifecycleRoot.class).shared)
          .isSameAs(context.spy(SecondLifecycleRoot.class).shared);
      Recorder.add("test");
    }
  }

  static class FirstLifecycleRoot {
    private final SharedLifecycleDependency shared;

    @Inject
    FirstLifecycleRoot(SharedLifecycleDependency shared) {
      this.shared = shared;
    }

    @PostConstruct
    void initialize() {
      Recorder.add("first-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("first-destroy");
    }
  }

  static class SecondLifecycleRoot {
    private final SharedLifecycleDependency shared;

    @Inject
    SecondLifecycleRoot(SharedLifecycleDependency shared) {
      this.shared = shared;
    }

    @PostConstruct
    void initialize() {
      Recorder.add("second-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("second-destroy");
    }
  }

  static class SharedLifecycleDependency {
    @PostConstruct
    void initialize() {
      Recorder.add("shared-post");
    }

    @PreDestroy
    void destroy() {
      Recorder.add("shared-destroy");
    }
  }

  static class InvalidLifecycleSignatureTest {
    private final SystemTestContext context =
        new SystemTestContextBuilder(InvalidLifecycleRoot.class).build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);

    @Test
    void testMethod() {}
  }

  static class InvalidLifecycleRoot {
    @PostConstruct
    void initialize(String ignored) {}
  }

  static final class Recorder {
    private static final List<String> EVENTS = new ArrayList<>();

    private Recorder() {}

    static void clear() {
      EVENTS.clear();
    }

    static void add(String event) {
      EVENTS.add(event);
    }

    static List<String> events() {
      return List.copyOf(EVENTS);
    }
  }
}
