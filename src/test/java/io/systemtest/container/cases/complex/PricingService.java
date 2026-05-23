package io.systemtest.container.cases.complex;

import jakarta.inject.Inject;

/**
 * Test support type for PricingService.
 *
 * @author Mustapha Zouari
 */
final class PricingService {

  private final DiscountPolicy discountPolicy;
  private final TaxCalculator taxCalculator;
  private final CurrencyRateProvider currencyRateProvider;

  @Inject
  PricingService(
      DiscountPolicy discountPolicy,
      TaxCalculator taxCalculator,
      CurrencyRateProvider currencyRateProvider) {
    this.discountPolicy = discountPolicy;
    this.taxCalculator = taxCalculator;
    this.currencyRateProvider = currencyRateProvider;
  }

  long price(Long customerId, long basePrice) {
    long discount = discountPolicy.discount(customerId);
    long tax = taxCalculator.tax(basePrice - discount);
    return currencyRateProvider.convert(basePrice - discount + tax);
  }
}
