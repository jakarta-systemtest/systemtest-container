package io.systemtest.container.cases.complex;

/**
 * Test support type for CurrencyRateProvider.
 *
 * @author Mustapha Zouari
 */
interface CurrencyRateProvider {

  long convert(long amount);
}
