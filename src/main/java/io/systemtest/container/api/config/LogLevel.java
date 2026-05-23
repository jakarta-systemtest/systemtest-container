package io.systemtest.container.api.config;

/**
 * Controls console logging emitted by System Test Container.
 *
 * <p>This level is independent from {@link DebugLevel}. Use {@code LogLevel} to control what is
 * written through the JDK logger, and use {@code DebugLevel} to control structured data available
 * from {@link io.systemtest.container.api.context.SystemTestContext#debugDetails()}.
 *
 * @author Mustapha Zouari
 */
public enum LogLevel {
  /** Disable library logging. */
  OFF(0),
  /** Log only failed operations. */
  ERROR(1),
  /** Log warnings and failed operations. */
  WARN(2),
  /** Log high-level resolution events. */
  INFO(3),
  /** Log detailed resolver activity. */
  DEBUG(4),
  /** Log the most detailed resolver and discovery activity. */
  TRACE(5);

  private final int priority;

  LogLevel(int priority) {
    this.priority = priority;
  }

  /**
   * Returns true when this level includes the supplied event level.
   *
   * @param eventLevel event level to test
   * @return true when the event should be logged
   */
  public boolean includes(LogLevel eventLevel) {
    return this != OFF && this.priority >= eventLevel.priority;
  }
}
