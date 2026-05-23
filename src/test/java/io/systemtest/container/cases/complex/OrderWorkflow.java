package io.systemtest.container.cases.complex;

import jakarta.inject.Inject;

/**
 * Test support type for OrderWorkflow.
 *
 * @author Mustapha Zouari
 */
final class OrderWorkflow {

  private final PricingService pricingService;
  private final InventoryGateway inventoryGateway;

  @Inject private NotificationPort notificationPort;

  @Inject
  OrderWorkflow(PricingService pricingService, InventoryGateway inventoryGateway) {
    this.pricingService = pricingService;
    this.inventoryGateway = inventoryGateway;
  }

  OrderReceipt checkout(Long customerId, String sku, long basePrice) {
    if (!inventoryGateway.isAvailable(sku)) {
      throw new IllegalStateException("Unavailable sku " + sku);
    }
    long finalPrice = pricingService.price(customerId, basePrice);
    notificationPort.notifyCustomer(customerId, "order accepted");
    return new OrderReceipt(sku, finalPrice);
  }
}
