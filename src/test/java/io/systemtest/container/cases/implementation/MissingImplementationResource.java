package io.systemtest.container.cases.implementation;

import jakarta.inject.Inject;

/**
 * Test support type for MissingImplementationResource.
 *
 * @author Mustapha Zouari
 */
final class MissingImplementationResource {

  private final MissingImplementationDependency dependency;

  @Inject
  MissingImplementationResource(MissingImplementationDependency dependency) {
    this.dependency = dependency;
  }

  String value() {
    return dependency.value();
  }
}
