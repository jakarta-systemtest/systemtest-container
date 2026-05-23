package io.systemtest.container.cases.injection;

import static org.assertj.core.api.Assertions.assertThat;

import io.systemtest.container.api.context.SystemTestContextBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test support type for FieldInjectionSystemTest.
 *
 * @author Mustapha Zouari
 */
class FieldInjectionSystemTest {

  @Test
  void resolvesFieldInjectedBeans() {
    AuditEndpoint endpoint =
        new SystemTestContextBuilder(AuditEndpoint.class).build().spy(AuditEndpoint.class);

    assertThat(endpoint.isReady()).isTrue();
  }
}
