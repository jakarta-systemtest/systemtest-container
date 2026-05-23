package io.systemtest.container.cases.multiroot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.exception.SystemTestContainerException;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test support type for MultiRootContextSystemTest.
 *
 * @author Mustapha Zouari
 */
class MultiRootContextSystemTest {

  @Test
  void resolvesMultipleRootsIntoOneSharedContext() {
    SystemTestContext context =
        new SystemTestContextBuilder(BookingResource.class, BillingResource.class)
            .mock(ExplicitGateway.class)
            .build();

    when(context.mock(ExplicitGateway.class).value()).thenReturn("explicit");
    when(context.mock(AutoGateway.class).value()).thenReturn("auto");

    assertThat(context.spy(BookingResource.class).value())
        .isEqualTo("booking:shared:explicit:auto");
    assertThat(context.spy(BillingResource.class).value())
        .isEqualTo("billing:shared:explicit:auto");
    assertThat(context.spy(SharedService.class))
        .isSameAs(context.spy(BookingResource.class).shared);
    assertThat(context.spy(SharedService.class))
        .isSameAs(context.spy(BillingResource.class).shared);
    assertThat(context.mock(ExplicitGateway.class))
        .isSameAs(context.spy(BookingResource.class).explicitGateway);
    assertThat(context.mock(ExplicitGateway.class))
        .isSameAs(context.spy(BillingResource.class).explicitGateway);
    assertThat(context.mock(AutoGateway.class))
        .isSameAs(context.spy(BookingResource.class).autoGateway);
    assertThat(context.mock(AutoGateway.class))
        .isSameAs(context.spy(BillingResource.class).autoGateway);
    assertThat(Mockito.mockingDetails(context.spy(SharedService.class)).isSpy()).isTrue();
    assertThat(Mockito.mockingDetails(context.mock(AutoGateway.class)).isMock()).isTrue();
  }

  @Test
  void supportsCollectionRootTypesAndDebugRootTypes() {
    SystemTestContext context =
        new SystemTestContextBuilder(List.of(BookingResource.class, BillingResource.class))
            .debug()
            .build();

    assertThat(context.debugDetails().rootTypes())
        .containsExactly(BookingResource.class.getName(), BillingResource.class.getName());
    assertThat(context.debugDetailsJson())
        .contains(
            "\"rootTypes\":[\""
                + BookingResource.class.getName()
                + "\",\""
                + BillingResource.class.getName()
                + "\"]");
  }

  @Test
  void rejectsInvalidRootTypeInput() {
    assertThatThrownBy(() -> new SystemTestContextBuilder(List.of()))
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("At least one root type is required");

    assertThatThrownBy(
            () -> new SystemTestContextBuilder(BookingResource.class, BookingResource.class))
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("Duplicate root type");
  }

  static final class BookingResource {
    private final SharedService shared;
    private final ExplicitGateway explicitGateway;
    private final AutoGateway autoGateway;

    @Inject
    BookingResource(
        SharedService shared, ExplicitGateway explicitGateway, AutoGateway autoGateway) {
      this.shared = shared;
      this.explicitGateway = explicitGateway;
      this.autoGateway = autoGateway;
    }

    String value() {
      return "booking:"
          + shared.value()
          + ":"
          + explicitGateway.value()
          + ":"
          + autoGateway.value();
    }
  }

  static final class BillingResource {
    private final SharedService shared;
    private final ExplicitGateway explicitGateway;
    private final AutoGateway autoGateway;

    @Inject
    BillingResource(
        SharedService shared, ExplicitGateway explicitGateway, AutoGateway autoGateway) {
      this.shared = shared;
      this.explicitGateway = explicitGateway;
      this.autoGateway = autoGateway;
    }

    String value() {
      return "billing:"
          + shared.value()
          + ":"
          + explicitGateway.value()
          + ":"
          + autoGateway.value();
    }
  }

  static final class SharedService {
    String value() {
      return "shared";
    }
  }

  interface ExplicitGateway {
    String value();
  }

  interface AutoGateway {
    String value();
  }
}
