package io.systemtest.container.cases.implementation;

/**
 * Test support type for SecondMultipleImplementation.
 *
 * @author Mustapha Zouari
 */
final class SecondMultipleImplementation extends MultipleImplementationDependency {

  @Override
  String value() {
    return "second";
  }
}
