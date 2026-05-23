package io.systemtest.container.cases.complex;

import jakarta.inject.Inject;

/**
 * Test support type for LoyaltyDiscountPolicy.
 *
 * @author Mustapha Zouari
 */
final class LoyaltyDiscountPolicy implements DiscountPolicy {

  private static final long PREMIUM_DISCOUNT = 20L;
  private static final long STANDARD_DISCOUNT = 5L;

  private final CustomerSegmentService customerSegmentService;

  @Inject private PromotionClient promotionClient;

  @Inject
  LoyaltyDiscountPolicy(CustomerSegmentService customerSegmentService) {
    this.customerSegmentService = customerSegmentService;
  }

  @Override
  public long discount(Long customerId) {
    long loyaltyDiscount =
        customerSegmentService.isPremium(customerId) ? PREMIUM_DISCOUNT : STANDARD_DISCOUNT;
    return loyaltyDiscount + promotionClient.extraDiscount(customerId);
  }
}
