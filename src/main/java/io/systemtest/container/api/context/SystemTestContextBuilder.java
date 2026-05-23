package io.systemtest.container.api.context;

import io.systemtest.container.api.config.DebugLevel;
import io.systemtest.container.api.config.LogLevel;
import io.systemtest.container.api.debug.ResolutionDebugDetails;
import io.systemtest.container.api.exception.SystemTestContainerException;
import io.systemtest.container.internal.resolution.SystemTestContextResolver;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds a {@link SystemTestContext} for one or more root classes.
 *
 * <p>Use it directly from tests:
 *
 * <pre>{@code
 * SystemTestContext context =
 *     new SystemTestContextBuilder(OrderWorkflow.class)
 *         .mock(InventoryGateway.class)
 *         .logLevel(LogLevel.WARN)
 *         .debug()
 *         .build();
 * }</pre>
 *
 * <p>A successful resolution is cached by this builder instance.
 *
 * @author Mustapha Zouari
 */
public final class SystemTestContextBuilder {

  private final List<Class<?>> rootTypes;
  private final Set<Class<?>> mocks = new LinkedHashSet<>();
  private DebugLevel debugLevel = DebugLevel.BASIC;
  private LogLevel logLevel = LogLevel.WARN;
  private ResolutionDebugDetails lastDebugDetails;
  private SystemTestContext resolvedContext;

  /**
   * Creates a builder for root classes to resolve.
   *
   * @param rootTypes root object types under test
   */
  public SystemTestContextBuilder(Class<?>... rootTypes) {
    this(Arrays.asList(rootTypes));
  }

  /**
   * Creates a builder for root classes to resolve.
   *
   * @param rootTypes root object types under test
   */
  public SystemTestContextBuilder(Collection<Class<?>> rootTypes) {
    this.rootTypes = validateRootTypes(rootTypes);
  }

  /**
   * Declares a dependency as an explicit Mockito mock.
   *
   * <p>The builder cannot be reconfigured after a successful {@link #build()}.
   *
   * @param dependencyType dependency type to mock
   * @return this builder
   */
  public SystemTestContextBuilder mock(Class<?> dependencyType) {
    ensureNotResolved();
    mocks.add(Objects.requireNonNull(dependencyType, "dependencyType"));
    return this;
  }

  /**
   * Enables trace debug details.
   *
   * @return this builder
   */
  public SystemTestContextBuilder debug() {
    return debug(DebugLevel.TRACE);
  }

  /**
   * Sets the debug detail collection level.
   *
   * <p>The builder cannot be reconfigured after a successful {@link #build()}.
   *
   * @param debugLevel level to use for this build
   * @return this builder
   */
  public SystemTestContextBuilder debug(DebugLevel debugLevel) {
    ensureNotResolved();
    this.debugLevel = Objects.requireNonNull(debugLevel, "debugLevel");
    return this;
  }

  /**
   * Sets the library log level.
   *
   * <p>The default is {@link LogLevel#WARN}, which keeps normal test runs quiet while still logging
   * warnings and errors. This setting controls console logging only; structured debug details are
   * controlled separately with {@link #debug(DebugLevel)}.
   *
   * <p>The builder cannot be reconfigured after a successful {@link #build()}.
   *
   * @param logLevel log level to use while resolving this context
   * @return this builder
   */
  public SystemTestContextBuilder logLevel(LogLevel logLevel) {
    ensureNotResolved();
    this.logLevel = Objects.requireNonNull(logLevel, "logLevel");
    return this;
  }

  /**
   * Resolves the graph and returns the context.
   *
   * <p>A successful resolution is cached. Later calls return the same context and do not resolve
   * the graph again.
   *
   * @return resolved test context
   */
  public SystemTestContext build() {
    if (resolvedContext != null) {
      return resolvedContext;
    }
    final var resolver = new SystemTestContextResolver(rootTypes, mocks, debugLevel, logLevel);
    try {
      resolvedContext = resolver.resolve();
      lastDebugDetails = resolvedContext.debugDetails();
      return resolvedContext;
    } catch (RuntimeException e) {
      lastDebugDetails = resolver.debugDetails();
      throw e;
    }
  }

  /**
   * Returns debug details from the last build attempt.
   *
   * <p>If no build has run, this returns an empty report for the configured debug level.
   *
   * @return last resolution debug details
   */
  public ResolutionDebugDetails debugDetails() {
    if (lastDebugDetails == null) {
      return ResolutionDebugDetails.empty(rootTypes, debugLevel);
    }
    return lastDebugDetails;
  }

  /**
   * Returns {@link #debugDetails()} as JSON.
   *
   * @return JSON debug report
   */
  public String debugDetailsJson() {
    return debugDetails().toJson();
  }

  private static List<Class<?>> validateRootTypes(Collection<Class<?>> rootTypes) {
    Objects.requireNonNull(rootTypes, "rootTypes");
    if (rootTypes.isEmpty()) {
      throw new SystemTestContainerException("At least one root type is required");
    }
    final var uniqueRootTypes = new LinkedHashSet<Class<?>>();
    for (final var rootType : rootTypes) {
      if (rootType == null) {
        throw new SystemTestContainerException("Root type cannot be null");
      }
      if (!uniqueRootTypes.add(rootType)) {
        throw new SystemTestContainerException("Duplicate root type: " + rootType.getName());
      }
    }
    return List.copyOf(uniqueRootTypes);
  }

  private void ensureNotResolved() {
    if (resolvedContext != null) {
      throw new SystemTestContainerException("Cannot reconfigure builder after build()");
    }
  }
}
