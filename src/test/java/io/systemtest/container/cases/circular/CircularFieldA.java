package io.systemtest.container.cases.circular;

import jakarta.inject.Inject;

/**
 * Test support type for CircularFieldA.
 *
 * @author Mustapha Zouari
 */
final class CircularFieldA {

  @Inject CircularFieldB b;

  String callB() {
    return "A->" + b.name();
  }

  String name() {
    return "A";
  }
}
