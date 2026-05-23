package io.systemtest.container.cases.debug;

/**
 * Test support type for FirstAmbiguousDebugService.
 *
 * @author Mustapha Zouari
 */
final class FirstAmbiguousDebugService implements AmbiguousDebugDependency {

  @Override
  public String value() {
    return "first";
  }
}
