package io.systemtest.container.cases.circular;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test support type for CircularDependencySystemTest.
 *
 * @author Mustapha Zouari
 */
class CircularDependencySystemTest {

  @Test
  void resolvesFieldInjectedCircularDependencies() {
    SystemTestContext context = new SystemTestContextBuilder(CircularFieldA.class).debug().build();

    CircularFieldA a = context.spy(CircularFieldA.class);
    CircularFieldB b = context.spy(CircularFieldB.class);

    assertThat(a.callB()).isEqualTo("A->B");
    assertThat(b.callA()).isEqualTo("B->A");
    assertThat(a.b).isSameAs(b);
    assertThat(b.a).isSameAs(a);
    assertThat(mockingDetails(a).isSpy()).isTrue();
    assertThat(mockingDetails(b).isSpy()).isTrue();
    assertThat(context.debugDetailsJson()).contains("\"kind\":\"circular-reuse\"");
  }

  @Test
  void resolvesMixedCircularDependencyWhenFieldInjectionBreaksTheCycle() {
    SystemTestContext context = new SystemTestContextBuilder(MixedCircularA.class).build();

    MixedCircularA a = context.spy(MixedCircularA.class);
    MixedCircularB b = context.spy(MixedCircularB.class);

    assertThat(a.callB()).isEqualTo("A->B");
    assertThat(b.callA()).isEqualTo("B->A");
    assertThat(a.b).isSameAs(b);
  }
}
