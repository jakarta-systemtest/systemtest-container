package io.systemtest.container.cases.failure;

import jakarta.inject.Inject;

/**
 * Test support type for CircularB.
 *
 * @author Mustapha Zouari
 */
final class CircularB {

  @Inject
  CircularB(CircularA a) {}
}
