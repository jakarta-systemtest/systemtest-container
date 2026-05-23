package io.systemtest.container.api.config;

/**
 * Controls how much resolution debug data is collected.
 *
 * @author Mustapha Zouari
 */
public enum DebugLevel {
  /** Do not collect details beyond an empty report shape. */
  OFF,
  /** Collect total time, success state, errors, spies, and mocks. */
  BASIC,
  /** Collect BASIC details plus graph nodes, steps, timings, and paths. */
  TRACE
}
