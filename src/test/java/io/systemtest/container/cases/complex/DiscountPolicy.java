package io.systemtest.container.cases.complex;

/**
 * Test support type for DiscountPolicy.
 *
 * @author Mustapha Zouari
 */
interface DiscountPolicy {

  long discount(Long customerId);
}
