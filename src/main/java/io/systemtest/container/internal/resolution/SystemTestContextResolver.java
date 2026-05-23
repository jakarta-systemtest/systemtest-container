package io.systemtest.container.internal.resolution;

import io.systemtest.container.api.config.DebugLevel;
import io.systemtest.container.api.config.LogLevel;
import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.debug.ResolutionDebugDetails;
import io.systemtest.container.api.exception.AmbiguousImplementationException;
import io.systemtest.container.api.exception.SystemTestContainerException;
import io.systemtest.container.internal.resolution.BeanRegistry.BeanState;
import io.systemtest.container.internal.util.LoggingSupport;
import io.systemtest.container.internal.util.ReflectionSupport;
import java.lang.System.Logger;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mockito.Mockito;

/**
 * Internal support type for SystemTestContextResolver.
 *
 * @author Mustapha Zouari
 */
public final class SystemTestContextResolver {

  private static final Logger LOGGER = System.getLogger(SystemTestContextResolver.class.getName());

  private final List<Class<?>> rootTypes;
  private final Set<Class<?>> mockTypes;
  private final DebugLevel debugLevel;
  private final LogLevel logLevel;
  private final BeanRegistry registry = new BeanRegistry();
  private final Map<Class<?>, Object> mocks = new LinkedHashMap<>();
  private final ResolutionPath resolutionPath = new ResolutionPath();
  private final ImplementationScanner implementationScanner;
  private final ResolutionDebugDetails.Builder debugDetailsBuilder;
  private ResolutionDebugDetails debugDetails;

  public SystemTestContextResolver(
      List<Class<?>> rootTypes, Set<Class<?>> mockTypes, DebugLevel debugLevel, LogLevel logLevel) {
    this.rootTypes = List.copyOf(rootTypes);
    this.mockTypes = Set.copyOf(mockTypes);
    this.debugLevel = Objects.requireNonNull(debugLevel, "debugLevel");
    this.logLevel = Objects.requireNonNull(logLevel, "logLevel");
    this.implementationScanner =
        new ImplementationScanner(this.rootTypes.get(0).getClassLoader(), this.logLevel);
    this.debugDetailsBuilder = ResolutionDebugDetails.builder(this.rootTypes, this.debugLevel);
    this.debugDetails = ResolutionDebugDetails.empty(this.rootTypes, this.debugLevel);
  }

  public SystemTestContext resolve() {
    final var startedAt = System.nanoTime();
    log(LogLevel.INFO, "Resolving system test graph for roots {0}", rootTypeNames());
    try {
      mockTypes.forEach(this::createMock);
      rootTypes.forEach(this::resolveType);
      recordSuccessfulResolution(startedAt);
      return new SystemTestContext(registry.spiesSnapshot(), mocks, logLevel, debugDetails);
    } catch (RuntimeException e) {
      recordFailedResolution(startedAt, e);
      throw e;
    }
  }

  public ResolutionDebugDetails debugDetails() {
    return debugDetails;
  }

  private void createMock(Class<?> type) {
    final var startedAt = System.nanoTime();
    log(LogLevel.DEBUG, "Creating explicit mock for {0}", type.getName());
    final var mock = Mockito.mock(type);
    final var durationNanos = elapsedSince(startedAt);
    registerMock(type, mock, "explicit-mock", durationNanos);
    debugDetailsBuilder.step(
        "DEBUG",
        "createExplicitMock",
        type,
        "Created explicit mock",
        millis(durationNanos),
        tracePath());
    log(
        LogLevel.DEBUG,
        "Created explicit mock for {0} in {1} ms",
        type.getName(),
        millis(durationNanos));
  }

  private void registerMock(Class<?> type, Object mock, String kind, long durationNanos) {
    mocks.put(type, mock);
    registry.putMock(type, mock);
    log(LogLevel.TRACE, "Registered mock for {0}", type.getName());
    debugDetailsBuilder.graphNode(type, type, kind, millis(durationNanos), tracePath());
  }

  private <T> T resolveType(Class<T> type) {
    final var startedAt = System.nanoTime();
    log(LogLevel.TRACE, "Resolving {0}", type.getName());
    if (registry.contains(type)) {
      return reuseRegisteredType(type, startedAt);
    }
    if (resolutionPath.contains(type)) {
      throw constructorCycle(type, startedAt);
    }
    if (isAbstractType(type)) {
      final var resolved = resolveAbstractType(type);
      debugDetailsBuilder.step(
          "DEBUG",
          "resolveAbstractType",
          type,
          "Resolved abstract dependency",
          millis(elapsedSince(startedAt)),
          tracePath());
      return resolved;
    }
    return resolveConcreteType(type, startedAt);
  }

  private <T> T reuseRegisteredType(Class<T> type, long startedAt) {
    final var complete = registry.isComplete(type);
    final var message =
        complete
            ? "Using already registered instance"
            : "Using early registered instance for circular field dependency";
    final var logLevel = complete ? LogLevel.TRACE : LogLevel.DEBUG;
    final var action = complete ? "reuseRegisteredInstance" : "reuseCircularInstance";
    final var graphKind = complete ? "reused" : "circular-reuse";
    final var durationNanos = elapsedSince(startedAt);

    log(logLevel, "{0} for {1}", message, type.getName());
    debugDetailsBuilder.step(
        complete ? "TRACE" : "DEBUG", action, type, message, millis(durationNanos), tracePath());
    debugDetailsBuilder.graphNode(type, type, graphKind, millis(durationNanos), tracePath());
    return registry.get(type);
  }

  private SystemTestContainerException constructorCycle(Class<?> type, long startedAt) {
    log(
        LogLevel.ERROR,
        "Unsupported constructor circular dependency detected while resolving {0}",
        type.getName());
    final var exception =
        new SystemTestContainerException(
            "Unsupported constructor circular dependency detected: "
                + resolutionPath.formatCycle(type));
    debugDetailsBuilder.error(exception, errorPathWith(type));
    debugDetailsBuilder.step(
        "ERROR",
        "unsupportedConstructorCycle",
        type,
        "Constructor-only circular dependencies are not supported",
        millis(elapsedSince(startedAt)),
        errorPathWith(type));
    return exception;
  }

  private <T> T resolveConcreteType(Class<T> type, long startedAt) {
    resolutionPath.push(type);
    registry.markCreating(type);
    try {
      final var spy = createEarlyRegisteredSpy(type, startedAt);
      injectFields(spy, type);
      completeSpy(type, startedAt);
      return spy;
    } catch (RuntimeException e) {
      if (!registry.contains(type) && registry.state(type) == BeanState.CREATING) {
        registry.removeState(type);
      }
      debugDetailsBuilder.error(e, errorPath());
      throw e;
    } finally {
      resolutionPath.pop();
    }
  }

  private <T> T createEarlyRegisteredSpy(Class<T> type, long startedAt) {
    final var instance = instantiate(type);
    final var spy = Mockito.spy(instance);
    final var durationNanos = elapsedSince(startedAt);

    registry.put(type, spy, BeanState.REGISTERED);
    log(LogLevel.DEBUG, "Early registered spy for {0}", type.getName());
    debugDetailsBuilder.step(
        "DEBUG",
        "earlyRegisterSpy",
        type,
        "Registered spy before field injection",
        millis(durationNanos),
        tracePath());
    debugDetailsBuilder.graphNode(type, type, "early-spy", millis(durationNanos), tracePath());
    return spy;
  }

  private void completeSpy(Class<?> type, long startedAt) {
    registry.markComplete(type);
    log(LogLevel.DEBUG, "Completed field injection for {0}", type.getName());
    final var durationNanos = elapsedSince(startedAt);
    debugDetailsBuilder.step(
        "DEBUG",
        "resolveConcreteType",
        type,
        "Instantiated, registered spy, and completed field injection",
        millis(durationNanos),
        tracePath());
    debugDetailsBuilder.graphNode(type, type, "complete-spy", millis(durationNanos), tracePath());
    log(
        LogLevel.DEBUG,
        "Resolved concrete type {0} as spy in {1} ms",
        type.getName(),
        millis(durationNanos));
  }

  private <T> T resolveAbstractType(Class<T> type) {
    final var startedAt = System.nanoTime();
    final var implementations = implementationScanner.findImplementations(type);
    final var scanDurationNanos = elapsedSince(startedAt);
    debugDetailsBuilder.step(
        "TRACE",
        "scanImplementations",
        type,
        "Found " + implementations.size() + " implementation(s)",
        millis(scanDurationNanos),
        tracePath());
    log(
        LogLevel.TRACE,
        "Scanned implementations for {0} in {1} ms",
        type.getName(),
        millis(scanDurationNanos));
    if (implementations.isEmpty()) {
      return createAutomaticMock(type, startedAt);
    }
    if (implementations.size() > 1) {
      throw ambiguousImplementations(type, implementations);
    }

    final var implementationType = implementations.get(0);
    log(
        LogLevel.INFO,
        "Resolved {0} to implementation {1}",
        type.getName(),
        implementationType.getName());
    final var implementation = resolveType(implementationType);
    registry.putSpyAlias(type, implementation);
    final var durationNanos = elapsedSince(startedAt);
    debugDetailsBuilder.graphNode(
        type, implementationType, "implementation-spy", millis(durationNanos), tracePath());
    log(
        LogLevel.INFO,
        "Resolved {0} to {1} in {2} ms",
        type.getName(),
        implementationType.getName(),
        millis(durationNanos));
    return type.cast(implementation);
  }

  private <T> T createAutomaticMock(Class<T> type, long startedAt) {
    log(LogLevel.INFO, "No implementation found for {0}; creating automatic mock", type.getName());
    final var mock = Mockito.mock(type);
    final var durationNanos = elapsedSince(startedAt);

    registerMock(type, mock, "automatic-mock", durationNanos);
    debugDetailsBuilder.step(
        "INFO",
        "createAutomaticMock",
        type,
        "No implementation found; created automatic mock",
        millis(durationNanos),
        tracePath());
    log(
        LogLevel.INFO,
        "Created automatic mock for {0} in {1} ms",
        type.getName(),
        millis(durationNanos));
    return mock;
  }

  private AmbiguousImplementationException ambiguousImplementations(
      Class<?> type, List<Class<?>> implementations) {
    log(
        LogLevel.ERROR,
        "Multiple implementations found for {0}: {1}",
        type.getName(),
        implementations.stream().map(Class::getName).toList());
    final var exception = new AmbiguousImplementationException(type, implementations);
    debugDetailsBuilder.error(exception, errorPath());
    return exception;
  }

  private <T> T instantiate(Class<T> type) {
    final var startedAt = System.nanoTime();
    log(LogLevel.DEBUG, "Instantiating {0}", type.getName());
    final var constructor = ReflectionSupport.selectConstructor(type);
    final var arguments = constructorArguments(type, constructor);

    try {
      constructor.setAccessible(true);
      final var instance = constructor.newInstance(arguments);
      debugDetailsBuilder.step(
          "DEBUG",
          "instantiate",
          type,
          "Instantiated with " + constructor.getParameterCount() + " constructor parameter(s)",
          millis(elapsedSince(startedAt)),
          tracePath());
      log(
          LogLevel.DEBUG,
          "Instantiated {0} in {1} ms",
          type.getName(),
          millis(elapsedSince(startedAt)));
      return instance;
    } catch (ReflectiveOperationException e) {
      final var exception =
          new SystemTestContainerException("Cannot instantiate " + type.getName(), e);
      debugDetailsBuilder.error(exception, errorPath());
      throw exception;
    }
  }

  private Object[] constructorArguments(Class<?> type, Constructor<?> constructor) {
    final var parameterTypes = constructor.getParameterTypes();
    final var arguments = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      log(
          LogLevel.TRACE,
          "Resolving constructor parameter {0} for {1}",
          parameterTypes[i].getName(),
          type.getName());
      arguments[i] = resolveType(parameterTypes[i]);
    }
    return arguments;
  }

  private void injectFields(Object instance, Class<?> type) {
    for (final var field : ReflectionSupport.injectableFields(type)) {
      injectField(instance, type, field);
    }
  }

  private void injectField(Object instance, Class<?> ownerType, Field field) {
    final var startedAt = System.nanoTime();
    log(LogLevel.DEBUG, "Injecting field {0} on {1}", field.getName(), ownerType.getName());
    final var dependency = resolveType(field.getType());
    try {
      field.setAccessible(true);
      field.set(instance, dependency);
      debugDetailsBuilder.step(
          "DEBUG",
          "injectField",
          field.getType(),
          "Injected field " + field.getName() + " on " + ownerType.getName(),
          millis(elapsedSince(startedAt)),
          tracePath());
      log(
          LogLevel.DEBUG,
          "Injected field {0} on {1} in {2} ms",
          field.getName(),
          ownerType.getName(),
          millis(elapsedSince(startedAt)));
    } catch (IllegalAccessException e) {
      log(LogLevel.ERROR, "Cannot inject field {0} on {1}", field.getName(), ownerType.getName());
      final var exception =
          new SystemTestContainerException(
              "Cannot inject field " + field.getName() + " on " + ownerType.getName(), e);
      debugDetailsBuilder.error(exception, errorPath());
      throw exception;
    }
  }

  private ResolutionDebugDetails finishDebugDetails(boolean success, long startedAt) {
    return debugDetailsBuilder.build(
        success, millis(elapsedSince(startedAt)), registry.spyTypes(), List.copyOf(mocks.keySet()));
  }

  private void recordSuccessfulResolution(long startedAt) {
    debugDetails = finishDebugDetails(true, startedAt);
    log(
        LogLevel.INFO,
        "Resolved system test graph for roots {0} in {1} ms",
        rootTypeNames(),
        debugDetails.totalTimeMillis());
  }

  private void recordFailedResolution(long startedAt, RuntimeException error) {
    debugDetailsBuilder.error(error, errorPath());
    debugDetails = finishDebugDetails(false, startedAt);
    log(
        LogLevel.ERROR,
        "Failed to resolve system test graph for roots {0} in {1} ms: {2}",
        rootTypeNames(),
        debugDetails.totalTimeMillis(),
        error.getMessage());
  }

  private long elapsedSince(long startedAt) {
    return System.nanoTime() - startedAt;
  }

  private double millis(long durationNanos) {
    return durationNanos / 1_000_000.0;
  }

  private List<String> currentPath() {
    return resolutionPath.typeNames();
  }

  private List<String> tracePath() {
    if (debugLevel != DebugLevel.TRACE) {
      return List.of();
    }
    return currentPath();
  }

  private List<String> errorPath() {
    if (debugLevel == DebugLevel.OFF) {
      return List.of();
    }
    return currentPath();
  }

  private List<String> errorPathWith(Class<?> type) {
    if (debugLevel == DebugLevel.OFF) {
      return List.of();
    }
    return resolutionPath.typeNamesWith(type);
  }

  private List<String> rootTypeNames() {
    return rootTypes.stream().map(Class::getName).toList();
  }

  private boolean isAbstractType(Class<?> type) {
    return type.isInterface() || Modifier.isAbstract(type.getModifiers());
  }

  private void log(LogLevel eventLevel, String message, Object... args) {
    LoggingSupport.log(LOGGER, logLevel, eventLevel, message, args);
  }
}
