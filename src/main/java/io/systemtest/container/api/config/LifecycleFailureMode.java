package io.systemtest.container.api.config;

/**
 * Controls how the JUnit lifecycle extension handles bean lifecycle failures.
 *
 * @author Mustapha Zouari
 */
public enum LifecycleFailureMode {

  /** Fail the test immediately when a bean lifecycle method fails. */
  STRICT,

  /** Log and record lifecycle failures in debug details, then continue the test. */
  LENIENT
}
