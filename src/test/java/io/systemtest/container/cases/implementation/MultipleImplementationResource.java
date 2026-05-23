package io.systemtest.container.cases.implementation;

import jakarta.inject.Inject;

/**
 * Test support type for MultipleImplementationResource.
 *
 * @author Mustapha Zouari
 */
final class MultipleImplementationResource {

  @Inject
  MultipleImplementationResource(MultipleImplementationDependency dependency) {}
}
