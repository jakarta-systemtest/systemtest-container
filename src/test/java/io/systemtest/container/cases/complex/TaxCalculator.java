package io.systemtest.container.cases.complex;

/**
 * Test support type for TaxCalculator.
 *
 * @author Mustapha Zouari
 */
final class TaxCalculator {

  long tax(long amount) {
    return amount / 10;
  }
}
