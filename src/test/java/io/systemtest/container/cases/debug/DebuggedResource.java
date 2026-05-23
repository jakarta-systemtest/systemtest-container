package io.systemtest.container.cases.debug;

import jakarta.inject.Inject;

/**
 * Test support type for DebuggedResource.
 *
 * @author Mustapha Zouari
 */
final class DebuggedResource {

  private final DebuggedDependency dependency;

  @Inject
  DebuggedResource(DebuggedDependency dependency) {
    this.dependency = dependency;
  }

  String value() {
    return dependency.value();
  }
}
