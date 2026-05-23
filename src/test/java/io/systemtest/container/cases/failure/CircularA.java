package io.systemtest.container.cases.failure;

import jakarta.inject.Inject;

/**
 * Test support type for CircularA.
 *
 * @author Mustapha Zouari
 */
final class CircularA {

  @Inject
  CircularA(CircularB b) {}
}
