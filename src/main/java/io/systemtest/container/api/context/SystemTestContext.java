package io.systemtest.container.api.context;

import io.systemtest.container.api.config.LogLevel;
import io.systemtest.container.api.debug.ResolutionDebugDetails;
import io.systemtest.container.api.exception.SystemTestContainerException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolved system test graph.
 *
 * <p>The context gives access to resolved spies, explicit or automatic mocks, and debug details.
 *
 * <pre>{@code
 * OrderWorkflow workflow = context.spy(OrderWorkflow.class);
 * InventoryGateway inventory = context.mock(InventoryGateway.class);
 * PricingService pricing = context.spy(PricingService.class);
 * }</pre>
 *
 * @author Mustapha Zouari
 */
public final class SystemTestContext {

  private final Map<Class<?>, Object> spies;
  private final Map<Class<?>, Object> mocks;
  private final LogLevel logLevel;
  private ResolutionDebugDetails debugDetails;

  /**
   * Creates a resolved context.
   *
   * @param spies resolved spies by exact type or alias type
   * @param mocks registered mocks by exact type
   * @param logLevel configured library log level
   * @param debugDetails resolution debug details
   */
  public SystemTestContext(
      Map<Class<?>, Object> spies,
      Map<Class<?>, Object> mocks,
      LogLevel logLevel,
      ResolutionDebugDetails debugDetails) {
    this.spies = Collections.unmodifiableMap(new LinkedHashMap<>(spies));
    this.mocks = Collections.unmodifiableMap(new LinkedHashMap<>(mocks));
    this.logLevel = logLevel;
    this.debugDetails = debugDetails;
  }

  /**
   * Returns an explicit or automatic Mockito mock.
   *
   * @param mockType mock type
   * @param <M> mock type
   * @return registered mock
   */
  public <M> M mock(Class<M> mockType) {
    final var mock = mocks.get(mockType);
    if (mock == null) {
      throw new SystemTestContainerException("No mock registered for " + mockType.getName());
    }
    return mockType.cast(mock);
  }

  /**
   * Returns a resolved real object wrapped as a Mockito spy.
   *
   * @param spyType spy type
   * @param <S> spy type
   * @return registered spy
   */
  public <S> S spy(Class<S> spyType) {
    final var spy = spies.get(spyType);
    if (spy == null) {
      throw new SystemTestContainerException("No spy registered for " + spyType.getName());
    }
    return spyType.cast(spy);
  }

  /**
   * Returns all registered spies in deterministic registration order.
   *
   * @return registered spies by exact or alias type
   */
  public Map<Class<?>, Object> spies() {
    return spies;
  }

  /**
   * Returns all registered explicit and automatic mocks in deterministic registration order.
   *
   * @return registered mocks by exact type
   */
  public Map<Class<?>, Object> mocks() {
    return mocks;
  }

  /**
   * Returns the log level configured for this context.
   *
   * <p>This level controls library log emission only. It does not change structured debug details
   * returned by {@link #debugDetails()}.
   *
   * @return configured log level
   */
  public LogLevel logLevel() {
    return logLevel;
  }

  /**
   * Returns structured debug details for this resolution.
   *
   * @return debug details
   */
  public ResolutionDebugDetails debugDetails() {
    return debugDetails;
  }

  /**
   * Records a lifecycle error captured by a test integration.
   *
   * @param error lifecycle error
   * @param path diagnostic path
   */
  public void recordLifecycleError(Throwable error, List<String> path) {
    debugDetails = debugDetails.withError(error, path);
  }

  /**
   * Returns {@link #debugDetails()} as JSON.
   *
   * @return JSON debug report
   */
  public String debugDetailsJson() {
    return debugDetails.toJson();
  }
}
