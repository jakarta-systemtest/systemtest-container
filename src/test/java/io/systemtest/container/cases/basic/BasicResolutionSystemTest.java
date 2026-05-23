package io.systemtest.container.cases.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test support type for BasicResolutionSystemTest.
 *
 * @author Mustapha Zouari
 */
class BasicResolutionSystemTest {

  @Test
  void resolvesConstructorGraphWithExplicitExternalMock() {
    SystemTestContext context =
        new SystemTestContextBuilder(ReservationEndpoint.class).mock(PaymentGateway.class).build();

    when(context.mock(PaymentGateway.class).authorize("A-101")).thenReturn("authorized");

    assertThat(context.spy(ReservationEndpoint.class).reserve("A-101"))
        .isEqualTo("A-101:authorized");
    assertThat(Mockito.mockingDetails(context.spy(ReservationEndpoint.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.spy(ReservationService.class)).isSpy()).isTrue();
  }

  @Test
  void automaticallyMocksInterfaceDependencyWhenNoImplementationExists() {
    SystemTestContext context = new SystemTestContextBuilder(ReservationEndpoint.class).build();

    when(context.mock(PaymentGateway.class).authorize("B-202")).thenReturn("auto-authorized");

    assertThat(context.spy(ReservationEndpoint.class).reserve("B-202"))
        .isEqualTo("B-202:auto-authorized");
    assertThat(Mockito.mockingDetails(context.mock(PaymentGateway.class)).isMock()).isTrue();
  }

  @Test
  void failsClearlyWhenAccessingUnknownContextEntries() {
    SystemTestContext context =
        new SystemTestContextBuilder(ReservationEndpoint.class).mock(PaymentGateway.class).build();

    assertThatThrownBy(() -> context.mock(UnusedGateway.class))
        .hasMessageContaining("No mock registered");
    assertThatThrownBy(() -> context.spy(UnusedService.class))
        .hasMessageContaining("No spy registered");
    assertThatThrownBy(() -> context.spy(PaymentGateway.class))
        .hasMessageContaining("No spy registered");
  }

  interface UnusedGateway {}

  static final class UnusedService {}
}
