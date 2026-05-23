package io.systemtest.container.cases.ejb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.exception.SystemTestContainerException;
import jakarta.ejb.EJB;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test support type for EjbFieldInjectionSystemTest.
 *
 * @author Mustapha Zouari
 */
class EjbFieldInjectionSystemTest {

  @Test
  void injectsConcreteEjbFieldAsSpy() {
    SystemTestContext context = new SystemTestContextBuilder(ConcreteEjbResource.class).build();

    assertThat(context.spy(ConcreteEjbResource.class).value()).isEqualTo("resource:concrete");
    assertThat(Mockito.mockingDetails(context.spy(ConcreteEjbService.class)).isSpy()).isTrue();
  }

  @Test
  void createsAutomaticMockForEjbInterfaceWhenNoImplementationExists() {
    SystemTestContext context = new SystemTestContextBuilder(AutoMockEjbResource.class).build();

    when(context.mock(RemotePaymentPort.class).authorize()).thenReturn("authorized");

    assertThat(context.spy(AutoMockEjbResource.class).value()).isEqualTo("authorized");
    assertThat(Mockito.mockingDetails(context.mock(RemotePaymentPort.class)).isMock()).isTrue();
  }

  @Test
  void resolvesSingleImplementationForEjbInterfaceAsSpyAlias() {
    SystemTestContext context =
        new SystemTestContextBuilder(SingleImplementationEjbResource.class).build();

    assertThat(context.spy(SingleImplementationEjbResource.class).value()).isEqualTo("single");
    assertThat(context.spy(InventoryPort.class)).isSameAs(context.spy(DefaultInventoryBean.class));
    assertThat(Mockito.mockingDetails(context.spy(InventoryPort.class)).isSpy()).isTrue();
  }

  @Test
  void explicitMockWinsForEjbField() {
    SystemTestContext context =
        new SystemTestContextBuilder(ExplicitMockEjbResource.class)
            .mock(ExplicitShippingPort.class)
            .build();

    when(context.mock(ExplicitShippingPort.class).quote()).thenReturn("mocked");

    assertThat(context.spy(ExplicitMockEjbResource.class).value()).isEqualTo("mocked");
    assertThat(Mockito.mockingDetails(context.mock(ExplicitShippingPort.class)).isMock()).isTrue();
  }

  @Test
  void rejectsStaticOrFinalEjbFields() {
    assertThatThrownBy(() -> new SystemTestContextBuilder(StaticEjbFieldResource.class).build())
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("injection fields cannot be static or final");

    assertThatThrownBy(() -> new SystemTestContextBuilder(FinalEjbFieldResource.class).build())
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("injection fields cannot be static or final");
  }

  static final class ConcreteEjbResource {
    @EJB ConcreteEjbService service;

    String value() {
      return "resource:" + service.value();
    }
  }

  static final class ConcreteEjbService {
    String value() {
      return "concrete";
    }
  }

  static final class AutoMockEjbResource {
    @EJB RemotePaymentPort paymentPort;

    String value() {
      return paymentPort.authorize();
    }
  }

  interface RemotePaymentPort {
    String authorize();
  }

  static final class SingleImplementationEjbResource {
    @EJB InventoryPort inventoryPort;

    String value() {
      return inventoryPort.status();
    }
  }

  interface InventoryPort {
    String status();
  }

  static final class DefaultInventoryBean implements InventoryPort {
    @Override
    public String status() {
      return "single";
    }
  }

  static final class ExplicitMockEjbResource {
    @EJB ExplicitShippingPort shippingPort;

    String value() {
      return shippingPort.quote();
    }
  }

  interface ExplicitShippingPort {
    String quote();
  }

  static final class StaticEjbFieldResource {
    @EJB static ConcreteEjbService service;
  }

  static final class FinalEjbFieldResource {
    @EJB final ConcreteEjbService service = null;
  }
}
