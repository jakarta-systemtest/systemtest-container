package io.systemtest.container.api.extension;

import io.systemtest.container.api.config.LifecycleFailureMode;
import io.systemtest.container.api.config.LogLevel;
import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.internal.lifecycle.BeanLifecycleInvoker;
import io.systemtest.container.internal.util.LoggingSupport;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that runs Jakarta EE bean lifecycle callbacks around each test method for an
 * already resolved {@link SystemTestContext}.
 *
 * @author Mustapha Zouari
 */
public final class SystemTestExtension implements BeforeTestExecutionCallback, AfterEachCallback {

  private static final Logger LOGGER = System.getLogger(SystemTestExtension.class.getName());
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(SystemTestExtension.class);
  private static final String BEANS_KEY = "beans";

  private final SystemTestContext context;
  private final LifecycleFailureMode failureMode;
  private final BeanLifecycleInvoker lifecycleInvoker = new BeanLifecycleInvoker();

  /**
   * Creates a JUnit extension for an already resolved system test context.
   *
   * @param context resolved system test context
   */
  public SystemTestExtension(SystemTestContext context) {
    this(context, LifecycleFailureMode.STRICT);
  }

  /**
   * Creates a JUnit extension for an already resolved system test context.
   *
   * @param context resolved system test context
   * @param failureMode lifecycle failure handling mode
   */
  public SystemTestExtension(SystemTestContext context, LifecycleFailureMode failureMode) {
    this.context = Objects.requireNonNull(context, "context");
    this.failureMode = Objects.requireNonNull(failureMode, "failureMode");
  }

  @Override
  public void beforeTestExecution(ExtensionContext extensionContext) {
    final var beans = lifecycleBeans(context);
    extensionContext.getStore(NAMESPACE).put(BEANS_KEY, beans);
    recordFailures(lifecycleInvoker.postConstruct(beans, failureMode));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterEach(ExtensionContext extensionContext) {
    List<BeanLifecycleInvoker.LifecycleBean> beans =
        extensionContext.getStore(NAMESPACE).remove(BEANS_KEY, List.class);
    if (beans == null) {
      return;
    }
    recordFailures(lifecycleInvoker.preDestroy(reverse(beans), failureMode));
  }

  private void recordFailures(List<BeanLifecycleInvoker.LifecycleFailure> failures) {
    for (final var failure : failures) {
      final var error = failure.error();
      LoggingSupport.log(
          LOGGER,
          context.logLevel(),
          LogLevel.WARN,
          "Ignored lenient lifecycle failure for {0} @{1} method {2}: {3}",
          failure.bean().type().getName(),
          failure.lifecycleAnnotation(),
          failure.methodName(),
          error.getMessage());
      context.recordLifecycleError(
          error,
          List.of(
              "lifecycle",
              failure.lifecycleAnnotation(),
              failure.bean().type().getName(),
              failure.methodName()));
    }
  }

  private List<BeanLifecycleInvoker.LifecycleBean> lifecycleBeans(SystemTestContext context) {
    final var beans = new ArrayList<BeanLifecycleInvoker.LifecycleBean>();
    final var seenInstances = Collections.newSetFromMap(new IdentityHashMap<>());
    for (final var entry : context.spies().entrySet()) {
      if (seenInstances.add(entry.getValue())) {
        beans.add(new BeanLifecycleInvoker.LifecycleBean(entry.getKey(), entry.getValue()));
      }
    }
    return List.copyOf(beans);
  }

  private List<BeanLifecycleInvoker.LifecycleBean> reverse(
      List<BeanLifecycleInvoker.LifecycleBean> beans) {
    final var reverseBeans = new ArrayList<>(beans);
    Collections.reverse(reverseBeans);
    return reverseBeans;
  }
}
