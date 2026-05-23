package io.systemtest.container.cases.circular;

import jakarta.inject.Inject;

/**
 * Test support type for MixedCircularB.
 *
 * @author Mustapha Zouari
 */
final class MixedCircularB {

  private final MixedCircularA a;

  @Inject
  MixedCircularB(MixedCircularA a) {
    this.a = a;
  }

  String callA() {
    return "B->" + a.name();
  }

  String name() {
    return "B";
  }
}
