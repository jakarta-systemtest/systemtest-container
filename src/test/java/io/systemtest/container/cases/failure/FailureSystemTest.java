package io.systemtest.container.cases.failure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.exception.SystemTestContainerException;
import org.junit.jupiter.api.Test;

/**
 * Test support type for FailureSystemTest.
 *
 * @author Mustapha Zouari
 */
class FailureSystemTest {

  @Test
  void rejectsConstructorOnlyCircularDependencies() {
    assertThatThrownBy(() -> new SystemTestContextBuilder(CircularA.class).build())
        .isInstanceOf(SystemTestContainerException.class)
        .hasMessageContaining("Unsupported constructor circular dependency detected")
        .hasMessageContaining("CircularA -> CircularB -> CircularA");
  }
}
