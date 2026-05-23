package io.systemtest.container.cases.debug;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.systemtest.container.api.config.DebugLevel;
import io.systemtest.container.api.config.LogLevel;
import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.exception.AmbiguousImplementationException;
import io.systemtest.container.api.exception.SystemTestContainerException;
import org.junit.jupiter.api.Test;

/**
 * Test support type for DebugDetailsSystemTest.
 *
 * @author Mustapha Zouari
 */
class DebugDetailsSystemTest {

  @Test
  void exposesBasicDebugDetailsFromContextAndBuilderByDefault() {
    SystemTestContextBuilder builder = new SystemTestContextBuilder(DebuggedResource.class);

    SystemTestContext context = builder.build();

    assertThat(context.debugDetails().rootTypes())
        .containsExactly(DebuggedResource.class.getName());
    assertThat(context.debugDetails().debugLevel()).isEqualTo(DebugLevel.BASIC);
    assertThat(context.logLevel()).isEqualTo(LogLevel.WARN);
    assertThat(context.debugDetails().success()).isTrue();
    assertThat(context.debugDetails().totalTimeMillis()).isPositive();
    assertThat(context.debugDetails().registeredMocks())
        .contains(DebuggedDependency.class.getName());
    assertThat(context.debugDetails().graph()).isEmpty();
    assertThat(context.debugDetails().steps()).isEmpty();
    assertThat(builder.debugDetailsJson()).isEqualTo(context.debugDetailsJson());
  }

  @Test
  void resolvesOnlyOnceForOneBuilderInstance() {
    SystemTestContextBuilder builder = new SystemTestContextBuilder(DebuggedResource.class);

    SystemTestContext first = builder.build();
    SystemTestContext second = builder.build();

    assertThat(second).isSameAs(first);
    assertThat(second.spy(DebuggedResource.class)).isSameAs(first.spy(DebuggedResource.class));
    assertThat(builder.debugDetailsJson()).isEqualTo(first.debugDetailsJson());
  }

  @Test
  void rejectsReconfigurationAfterSuccessfulResolution() {
    SystemTestContextBuilder builder = new SystemTestContextBuilder(DebuggedResource.class);

    builder.build();

    assertThatThrownBy(() -> builder.mock(DebuggedDependency.class))
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("Cannot reconfigure builder after build()");
    assertThatThrownBy(() -> builder.debug(DebugLevel.TRACE))
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("Cannot reconfigure builder after build()");
    assertThatThrownBy(() -> builder.logLevel(LogLevel.OFF))
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("Cannot reconfigure builder after build()");
  }

  @Test
  void exposesConfiguredLogLevelFromContext() {
    SystemTestContext context =
        new SystemTestContextBuilder(DebuggedResource.class).logLevel(LogLevel.OFF).build();

    assertThat(context.logLevel()).isEqualTo(LogLevel.OFF);
    assertThat(context.debugDetails().debugLevel()).isEqualTo(DebugLevel.BASIC);
  }

  @Test
  void exposesTraceDebugDetailsOnDemand() {
    SystemTestContext context =
        new SystemTestContextBuilder(DebuggedResource.class).debug().build();

    assertThat(context.debugDetails().debugLevel()).isEqualTo(DebugLevel.TRACE);
    assertThat(context.debugDetails().graph()).isNotEmpty();
    assertThat(context.debugDetails().steps()).isNotEmpty();
    assertThat(context.debugDetailsJson())
        .contains("\"rootTypes\":[\"" + DebuggedResource.class.getName() + "\"]")
        .contains("\"debugLevel\":\"TRACE\"")
        .contains("\"createAutomaticMock\"");
  }

  @Test
  void canDisableDebugDetails() {
    SystemTestContext context =
        new SystemTestContextBuilder(DebuggedResource.class).debug(DebugLevel.OFF).build();

    assertThat(context.debugDetails().debugLevel()).isEqualTo(DebugLevel.OFF);
    assertThat(context.debugDetails().registeredSpies()).isEmpty();
    assertThat(context.debugDetails().registeredMocks()).isEmpty();
    assertThat(context.debugDetails().graph()).isEmpty();
    assertThat(context.debugDetails().steps()).isEmpty();
  }

  @Test
  void exposesDebugDetailsFromBuilderAfterResolutionFailure() {
    SystemTestContextBuilder builder = new SystemTestContextBuilder(AmbiguousDebugResource.class);

    assertThatThrownBy(builder::build).isInstanceOf(AmbiguousImplementationException.class);

    assertThat(builder.debugDetails().success()).isFalse();
    assertThat(builder.debugDetails().errors()).isNotEmpty();
    assertThat(builder.debugDetailsJson())
        .contains("\"success\":false")
        .contains(AmbiguousImplementationException.class.getName())
        .contains(AmbiguousDebugDependency.class.getName());
  }
}
