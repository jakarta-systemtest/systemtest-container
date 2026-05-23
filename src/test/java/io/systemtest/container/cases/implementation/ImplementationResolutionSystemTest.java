package io.systemtest.container.cases.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.exception.AmbiguousImplementationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test support type for ImplementationResolutionSystemTest.
 *
 * @author Mustapha Zouari
 */
class ImplementationResolutionSystemTest {

  @Test
  void resolvesSingleInterfaceImplementationAsSpy() {
    SystemTestContext context =
        new SystemTestContextBuilder(SingleImplementationResource.class).build();

    assertThat(context.spy(SingleImplementationResource.class).value())
        .isEqualTo("real implementation");
    assertThat(Mockito.mockingDetails(context.spy(SingleImplementationDependency.class)).isSpy())
        .isTrue();
    assertThat(Mockito.mockingDetails(context.spy(SingleImplementationService.class)).isSpy())
        .isTrue();
  }

  @Test
  void automaticallyMocksInterfaceWithNoImplementation() {
    SystemTestContext context =
        new SystemTestContextBuilder(MissingImplementationResource.class).build();

    when(context.mock(MissingImplementationDependency.class).value()).thenReturn("automatic mock");

    assertThat(context.spy(MissingImplementationResource.class).value())
        .isEqualTo("automatic mock");
    assertThat(Mockito.mockingDetails(context.mock(MissingImplementationDependency.class)).isMock())
        .isTrue();
  }

  @Test
  void failsWhenMultipleImplementationsMatchAbstractDependency() {
    assertThatThrownBy(
            () -> new SystemTestContextBuilder(MultipleImplementationResource.class).build())
        .isInstanceOf(AmbiguousImplementationException.class)
        .hasMessageContaining(MultipleImplementationDependency.class.getName())
        .hasMessageContaining(FirstMultipleImplementation.class.getName())
        .hasMessageContaining(SecondMultipleImplementation.class.getName());
  }
}
