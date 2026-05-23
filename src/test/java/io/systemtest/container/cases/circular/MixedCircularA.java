package io.systemtest.container.cases.circular;

import jakarta.inject.Inject;

/**
 * Test support type for MixedCircularA.
 *
 * @author Mustapha Zouari
 */
final class MixedCircularA {

  @Inject MixedCircularB b;

  String callB() {
    return "A->" + b.name();
  }

  String name() {
    return "A";
  }
}
