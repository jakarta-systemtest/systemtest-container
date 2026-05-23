package io.systemtest.container.cases.implementation;

/**
 * Test support type for SingleImplementationService.
 *
 * @author Mustapha Zouari
 */
final class SingleImplementationService implements SingleImplementationDependency {

  @Override
  public String value() {
    return "real implementation";
  }
}
