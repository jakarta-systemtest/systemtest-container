package io.systemtest.container.api.debug;

import io.systemtest.container.api.config.DebugLevel;
import io.systemtest.container.internal.util.JsonSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured debug report for one graph resolution.
 *
 * <p>Use {@link #toJson()} when the report must be logged or attached to a failed test.
 *
 * @author Mustapha Zouari
 */
public final class ResolutionDebugDetails {

  private final List<String> rootTypes;
  private final DebugLevel debugLevel;
  private final boolean success;
  private final double totalTimeMillis;
  private final List<Step> steps;
  private final List<GraphNode> graph;
  private final List<ErrorDetail> errors;
  private final List<String> registeredSpies;
  private final List<String> registeredMocks;

  private ResolutionDebugDetails(
      List<String> rootTypes,
      DebugLevel debugLevel,
      boolean success,
      double totalTimeMillis,
      List<Step> steps,
      List<GraphNode> graph,
      List<ErrorDetail> errors,
      List<String> registeredSpies,
      List<String> registeredMocks) {
    this.rootTypes = List.copyOf(rootTypes);
    this.debugLevel = debugLevel;
    this.success = success;
    this.totalTimeMillis = totalTimeMillis;
    this.steps = List.copyOf(steps);
    this.graph = List.copyOf(graph);
    this.errors = List.copyOf(errors);
    this.registeredSpies = List.copyOf(registeredSpies);
    this.registeredMocks = List.copyOf(registeredMocks);
  }

  /**
   * Creates a details builder with {@link DebugLevel#BASIC}.
   *
   * @param rootType root type
   * @return debug details builder
   */
  public static Builder builder(Class<?> rootType) {
    return builder(List.of(rootType), DebugLevel.BASIC);
  }

  /**
   * Creates a details builder for the requested debug level.
   *
   * @param rootType root type
   * @param debugLevel debug level
   * @return debug details builder
   */
  public static Builder builder(Class<?> rootType, DebugLevel debugLevel) {
    return builder(List.of(rootType), debugLevel);
  }

  /**
   * Creates a details builder for the requested debug level.
   *
   * @param rootTypes root types
   * @param debugLevel debug level
   * @return debug details builder
   */
  public static Builder builder(List<Class<?>> rootTypes, DebugLevel debugLevel) {
    return new Builder(rootTypes.stream().map(Class::getName).toList(), debugLevel);
  }

  /**
   * Returns an empty report with debug level {@link DebugLevel#OFF}.
   *
   * @param rootType root type
   * @return empty debug details
   */
  public static ResolutionDebugDetails empty(Class<?> rootType) {
    return empty(List.of(rootType), DebugLevel.OFF);
  }

  /**
   * Returns an empty report for the requested debug level.
   *
   * @param rootType root type
   * @param debugLevel debug level
   * @return empty debug details
   */
  public static ResolutionDebugDetails empty(Class<?> rootType, DebugLevel debugLevel) {
    return empty(List.of(rootType), debugLevel);
  }

  /**
   * Returns an empty report for the requested debug level.
   *
   * @param rootTypes root types
   * @param debugLevel debug level
   * @return empty debug details
   */
  public static ResolutionDebugDetails empty(List<Class<?>> rootTypes, DebugLevel debugLevel) {
    return builder(rootTypes, debugLevel).build(false, 0.0, List.of(), List.of());
  }

  /**
   * Returns the root type names.
   *
   * @return root type names
   */
  public List<String> rootTypes() {
    return rootTypes;
  }

  /**
   * Returns the debug level used for this report.
   *
   * @return debug level
   */
  public DebugLevel debugLevel() {
    return debugLevel;
  }

  /**
   * Returns whether graph resolution completed successfully.
   *
   * @return true when resolution succeeded
   */
  public boolean success() {
    return success;
  }

  /**
   * Returns total resolution time in milliseconds.
   *
   * @return total duration in milliseconds
   */
  public double totalTimeMillis() {
    return totalTimeMillis;
  }

  /**
   * Returns trace steps.
   *
   * @return trace steps, empty unless TRACE is enabled
   */
  public List<Step> steps() {
    return steps;
  }

  /**
   * Returns graph nodes.
   *
   * @return graph nodes, empty unless TRACE is enabled
   */
  public List<GraphNode> graph() {
    return graph;
  }

  /**
   * Returns captured errors.
   *
   * @return resolution errors
   */
  public List<ErrorDetail> errors() {
    return errors;
  }

  /**
   * Returns registered spy type names.
   *
   * @return registered spy type names
   */
  public List<String> registeredSpies() {
    return registeredSpies;
  }

  /**
   * Returns registered mock type names.
   *
   * @return registered mock type names
   */
  public List<String> registeredMocks() {
    return registeredMocks;
  }

  /**
   * Returns a copy of these debug details with one additional error.
   *
   * @param error error to add
   * @param path diagnostic path
   * @return debug details including the supplied error
   */
  public ResolutionDebugDetails withError(Throwable error, List<String> path) {
    if (debugLevel == DebugLevel.OFF) {
      return this;
    }
    final List<ErrorDetail> updatedErrors = new ArrayList<>(errors);
    updatedErrors.add(
        new ErrorDetail(
            error.getClass().getName(),
            error.getMessage() == null ? "" : error.getMessage(),
            path));
    return new ResolutionDebugDetails(
        rootTypes,
        debugLevel,
        success,
        totalTimeMillis,
        steps,
        graph,
        updatedErrors,
        registeredSpies,
        registeredMocks);
  }

  /**
   * Returns this report as compact JSON.
   *
   * @return compact JSON report
   */
  public String toJson() {
    return "{"
        + "\"rootTypes\":"
        + JsonSupport.stringArray(rootTypes)
        + ","
        + "\"debugLevel\":\""
        + debugLevel
        + "\","
        + "\"success\":"
        + success
        + ","
        + "\"totalTimeMillis\":"
        + totalTimeMillis()
        + ","
        + "\"registeredSpies\":"
        + JsonSupport.stringArray(registeredSpies)
        + ","
        + "\"registeredMocks\":"
        + JsonSupport.stringArray(registeredMocks)
        + ","
        + "\"errors\":"
        + JsonSupport.jsonObjects(errors.stream().map(ErrorDetail::toJson).toList())
        + ","
        + "\"graph\":"
        + JsonSupport.jsonObjects(graph.stream().map(GraphNode::toJson).toList())
        + ","
        + "\"steps\":"
        + JsonSupport.jsonObjects(steps.stream().map(Step::toJson).toList())
        + "}";
  }

  @Override
  public String toString() {
    return toJson();
  }

  /**
   * One timed resolution step. Present only with {@link DebugLevel#TRACE}.
   *
   * @param level log level
   * @param action resolver action
   * @param type related type
   * @param message step message
   * @param durationMillis duration in milliseconds
   * @param path resolution path
   */
  public record Step(
      String level,
      String action,
      String type,
      String message,
      double durationMillis,
      List<String> path) {

    /**
     * Returns this step as compact JSON.
     *
     * @return compact JSON step
     */
    public String toJson() {
      return "{"
          + "\"level\":\""
          + JsonSupport.escape(level)
          + "\","
          + "\"action\":\""
          + JsonSupport.escape(action)
          + "\","
          + "\"type\":\""
          + JsonSupport.escape(type)
          + "\","
          + "\"message\":\""
          + JsonSupport.escape(message)
          + "\","
          + "\"durationMillis\":"
          + durationMillis
          + ","
          + "\"path\":"
          + JsonSupport.stringArray(path)
          + "}";
    }
  }

  /**
   * One node or alias in the resolved graph. Present only with {@link DebugLevel#TRACE}.
   *
   * @param requestedType requested dependency type
   * @param resolvedType concrete resolved type
   * @param kind node kind
   * @param durationMillis duration in milliseconds
   * @param path resolution path
   */
  public record GraphNode(
      String requestedType,
      String resolvedType,
      String kind,
      double durationMillis,
      List<String> path) {

    /**
     * Returns this graph node as compact JSON.
     *
     * @return compact JSON graph node
     */
    public String toJson() {
      return "{"
          + "\"requestedType\":\""
          + JsonSupport.escape(requestedType)
          + "\","
          + "\"resolvedType\":\""
          + JsonSupport.escape(resolvedType)
          + "\","
          + "\"kind\":\""
          + JsonSupport.escape(kind)
          + "\","
          + "\"durationMillis\":"
          + durationMillis
          + ","
          + "\"path\":"
          + JsonSupport.stringArray(path)
          + "}";
    }
  }

  /**
   * One resolution error captured for diagnostics.
   *
   * @param type exception type
   * @param message exception message
   * @param path resolution path
   */
  public record ErrorDetail(String type, String message, List<String> path) {

    /**
     * Returns this error as compact JSON.
     *
     * @return compact JSON error
     */
    public String toJson() {
      return "{"
          + "\"type\":\""
          + JsonSupport.escape(type)
          + "\","
          + "\"message\":\""
          + JsonSupport.escape(message)
          + "\","
          + "\"path\":"
          + JsonSupport.stringArray(path)
          + "}";
    }
  }

  /** Mutable collector used by the resolver. */
  public static final class Builder {

    private final List<String> rootTypes;
    private final DebugLevel debugLevel;
    private final List<Step> steps = new ArrayList<>();
    private final List<GraphNode> graph = new ArrayList<>();
    private final List<ErrorDetail> errors = new ArrayList<>();

    private Builder(List<String> rootTypes, DebugLevel debugLevel) {
      this.rootTypes = List.copyOf(rootTypes);
      this.debugLevel = debugLevel;
    }

    /**
     * Adds a trace step when TRACE is enabled.
     *
     * @param level log level
     * @param action action name
     * @param type related type
     * @param message step message
     * @param durationMillis duration in milliseconds
     * @param path resolution path
     */
    public void step(
        String level,
        String action,
        Class<?> type,
        String message,
        double durationMillis,
        List<String> path) {
      if (debugLevel != DebugLevel.TRACE) {
        return;
      }
      steps.add(new Step(level, action, type.getName(), message, durationMillis, path));
    }

    /**
     * Adds a graph node when TRACE is enabled.
     *
     * @param requestedType requested type
     * @param resolvedType resolved type
     * @param kind node kind
     * @param durationMillis duration in milliseconds
     * @param path resolution path
     */
    public void graphNode(
        Class<?> requestedType,
        Class<?> resolvedType,
        String kind,
        double durationMillis,
        List<String> path) {
      if (debugLevel != DebugLevel.TRACE) {
        return;
      }
      graph.add(
          new GraphNode(
              requestedType.getName(), resolvedType.getName(), kind, durationMillis, path));
    }

    /**
     * Adds an error unless debug is OFF.
     *
     * @param error resolution error
     * @param path resolution path
     */
    public void error(Throwable error, List<String> path) {
      if (debugLevel == DebugLevel.OFF) {
        return;
      }
      errors.add(
          new ErrorDetail(
              error.getClass().getName(),
              error.getMessage() == null ? "" : error.getMessage(),
              path));
    }

    /**
     * Builds immutable debug details.
     *
     * @param success whether resolution succeeded
     * @param totalTimeMillis total resolution time in milliseconds
     * @param registeredSpies registered spy types
     * @param registeredMocks registered mock types
     * @return immutable debug details
     */
    public ResolutionDebugDetails build(
        boolean success,
        double totalTimeMillis,
        List<Class<?>> registeredSpies,
        List<Class<?>> registeredMocks) {
      final List<Class<?>> spies = debugLevel == DebugLevel.OFF ? List.of() : registeredSpies;
      final List<Class<?>> mocks = debugLevel == DebugLevel.OFF ? List.of() : registeredMocks;
      return new ResolutionDebugDetails(
          rootTypes,
          debugLevel,
          success,
          totalTimeMillis,
          steps,
          graph,
          errors,
          spies.stream().map(Class::getName).sorted().toList(),
          mocks.stream().map(Class::getName).sorted().toList());
    }
  }
}
