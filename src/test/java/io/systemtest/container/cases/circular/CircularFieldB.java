package io.systemtest.container.cases.circular;

import jakarta.inject.Inject;

/**
 * Test support type for CircularFieldB.
 *
 * @author Mustapha Zouari
 */
final class CircularFieldB {

  @Inject CircularFieldA a;

  String callA() {
    return "B->" + a.name();
  }

  String name() {
    return "B";
  }
}
