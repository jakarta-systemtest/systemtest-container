package io.systemtest.container.cases.complex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test support type for ComplexOrderWorkflowSystemTest.
 *
 * @author Mustapha Zouari
 */
class ComplexOrderWorkflowSystemTest {

  @Test
  void resolvesComplicatedOrderWorkflowGraph() {
    SystemTestContext context =
        new SystemTestContextBuilder(OrderWorkflow.class)
            .mock(InventoryGateway.class)
            .mock(CurrencyRateProvider.class)
            .debug()
            .build();

    when(context.mock(InventoryGateway.class).isAvailable("SKU-1")).thenReturn(true);
    when(context.mock(PromotionClient.class).extraDiscount(42L)).thenReturn(3L);
    when(context.mock(CurrencyRateProvider.class).convert(84L)).thenReturn(168L);

    OrderReceipt receipt = context.spy(OrderWorkflow.class).checkout(42L, "SKU-1", 100L);

    assertThat(receipt).isEqualTo(new OrderReceipt("SKU-1", 168L));
    assertThat(context.debugDetails().graph()).hasSizeGreaterThanOrEqualTo(9);
    assertThat(context.debugDetails().steps()).isNotEmpty();
    assertThat(Mockito.mockingDetails(context.spy(OrderWorkflow.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.spy(PricingService.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.spy(DiscountPolicy.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.spy(DefaultCustomerSegmentService.class)).isSpy())
        .isTrue();
    assertThat(Mockito.mockingDetails(context.mock(PromotionClient.class)).isMock()).isTrue();
    assertThat(Mockito.mockingDetails(context.mock(NotificationPort.class)).isMock()).isTrue();

    verify(context.mock(NotificationPort.class)).notifyCustomer(42L, "order accepted");
  }
}
