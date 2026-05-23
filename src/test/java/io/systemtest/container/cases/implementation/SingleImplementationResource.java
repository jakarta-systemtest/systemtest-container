package io.systemtest.container.cases.implementation;

import jakarta.inject.Inject;

/**
 * Test support type for SingleImplementationResource.
 *
 * @author Mustapha Zouari
 */
final class SingleImplementationResource {

  private final SingleImplementationDependency dependency;

  @Inject
  SingleImplementationResource(SingleImplementationDependency dependency) {
    this.dependency = dependency;
  }

  String value() {
    return dependency.value();
  }
}
