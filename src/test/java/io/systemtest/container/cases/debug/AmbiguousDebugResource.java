package io.systemtest.container.cases.debug;

import jakarta.inject.Inject;

/**
 * Test support type for AmbiguousDebugResource.
 *
 * @author Mustapha Zouari
 */
final class AmbiguousDebugResource {

  @Inject
  AmbiguousDebugResource(AmbiguousDebugDependency dependency) {}
}
