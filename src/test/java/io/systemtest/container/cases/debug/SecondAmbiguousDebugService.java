package io.systemtest.container.cases.debug;

/**
 * Test support type for SecondAmbiguousDebugService.
 *
 * @author Mustapha Zouari
 */
final class SecondAmbiguousDebugService implements AmbiguousDebugDependency {

  @Override
  public String value() {
    return "second";
  }
}
