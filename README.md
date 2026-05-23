# System Test Container

> Test-time object graph resolver for Jakarta module-level system tests with JUnit 5 and Mockito.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jakarta-systemtest/systemtest-container.svg)](https://central.sonatype.com/artifact/io.github.jakarta-systemtest/systemtest-container)
[![Build](https://github.com/jakarta-systemtest/systemtest-container/actions/workflows/ci.yml/badge.svg)](https://github.com/jakarta-systemtest/systemtest-container/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](pom.xml)

System Test Container builds a `SystemTestContext` from one or more root classes. Classes inside
the tested boundary run as real objects wrapped in Mockito spies. External dependencies can be
declared as mocks, and unresolved abstract dependencies can become automatic mocks.

Despite the project name, this library is not a runtime container. It does not start CDI, EJB,
servlet infrastructure, or an application server.

## 🧭 At a Glance

| Need | Use |
| --- | --- |
| 🎯 Run a real application slice | Root classes resolved as Mockito spies |
| 🔌 Replace external systems | `.mock(ExternalType.class)` |
| 🔗 Share one graph across roots | `new SystemTestContextBuilder(rootA, rootB)` |
| 🔁 Run Jakarta lifecycle hooks | `SystemTestExtension` |
| 🔍 Inspect graph failures | `context.debugDetailsJson()` |

## ✨ Why Use It

A module-level system test runs a real slice of application code without deploying the application:

```text
HTTP/resource/boundary class
  -> real service
      -> real mapper
      -> mocked external port
```

This gives a practical middle ground between isolated unit tests and full end-to-end tests:

- ⚡ Fast feedback without server startup.
- 🎯 Real workflow behavior across several classes.
- 🧪 Standard Mockito stubbing and verification.
- 🔍 Structured diagnostics when graph resolution fails.

For background on this testing style, see the
[module-level system testing article](https://mustapha-zouari.com/blog/1/).

## 📌 Status

Current version: `0.1.0-beta`.

This is a first public beta. It is usable for experiments and feedback, but public APIs may still
change before `1.0.0`.

Published on Maven Central:
[io.github.jakarta-systemtest:systemtest-container](https://central.sonatype.com/artifact/io.github.jakarta-systemtest/systemtest-container).

## ✅ Requirements

- Java 17 or newer.
- JUnit Jupiter when using `SystemTestExtension`.
- Mockito 5.x.
- Jakarta `Inject`, `Annotation`, and `EJB` APIs.

Only Jakarta APIs are used. No Jakarta EE runtime is required.

## 🚀 Installation

Maven:

```xml
<dependency>
    <groupId>io.github.jakarta-systemtest</groupId>
    <artifactId>systemtest-container</artifactId>
    <version>0.1.0-beta</version>
    <scope>test</scope>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
testImplementation("io.github.jakarta-systemtest:systemtest-container:0.1.0-beta")
```

For local development on this repository:

```bash
mvn install
```

## 📘 Quick Start

Declare the root class and the dependencies that should be mocked:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import org.junit.jupiter.api.Test;

class ReservationEndpointSystemTest {

    @Test
    void reservesSeat() {
        SystemTestContext context =
            new SystemTestContextBuilder(ReservationEndpoint.class)
                .mock(PaymentGateway.class)
                .build();

        when(context.mock(PaymentGateway.class).authorize("A-101"))
            .thenReturn("authorized");

        String result = context.spy(ReservationEndpoint.class).reserve("A-101");

        assertEquals("A-101:authorized", result);
    }
}
```

- `spy(type)` returns a real resolved object wrapped by Mockito.
- `mock(type)` returns an explicit or automatic Mockito mock.

Result: the test exercises real application behavior while keeping external systems controlled by
Mockito.

## 🔗 Multiple Roots

Use multiple roots when one test should share the same resolved graph:

```java
SystemTestContext context =
    new SystemTestContextBuilder(ReservationEndpoint.class, AuditEndpoint.class)
        .mock(PaymentGateway.class)
        .build();

ReservationEndpoint reservation = context.spy(ReservationEndpoint.class);
AuditEndpoint audit = context.spy(AuditEndpoint.class);
PaymentGateway paymentGateway = context.mock(PaymentGateway.class);
```

Registered spies and mocks are shared across all roots in the context.

## 🔁 JUnit Extension

`SystemTestExtension` runs supported Jakarta lifecycle callbacks for resolved spies:

- `@PostConstruct` runs after JUnit `@BeforeEach` and before the test method.
- `@PreDestroy` runs after JUnit `@AfterEach`.
- Hooks run once per unique spy instance.
- Mocks do not receive lifecycle callbacks.
- Lifecycle failures are strict by default.

```java
import io.systemtest.container.api.context.SystemTestContext;
import io.systemtest.container.api.context.SystemTestContextBuilder;
import io.systemtest.container.api.extension.SystemTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

class ReservationEndpointSystemTest {

    private final SystemTestContext context =
        new SystemTestContextBuilder(ReservationEndpoint.class)
            .mock(PaymentGateway.class)
            .build();

    @RegisterExtension
    private final SystemTestExtension systemTest = new SystemTestExtension(context);
}
```

Use lenient mode when lifecycle failures should be recorded in debug details instead of failing the
test immediately:

```java
import io.systemtest.container.api.config.LifecycleFailureMode;

@RegisterExtension
private final SystemTestExtension systemTest =
    new SystemTestExtension(context, LifecycleFailureMode.LENIENT);
```

## ⚖️ What It Replaces

Without this library, tests often repeat manual Mockito wiring and lifecycle setup. With this
library, the test declares the root classes and external mocks, then uses the resolved context.

| Manual setup | System Test Container |
| --- | --- |
| `@Mock`, `@Spy`, and `@InjectMocks` setup grows with the graph. | The graph starts from root classes. |
| Internal services must often be added to test setup by hand. | Supported internal dependencies are resolved automatically. |
| Lifecycle callbacks need custom setup and teardown code. | `SystemTestExtension` runs supported lifecycle callbacks. |
| Failures can be hidden inside setup code. | Resolution and lifecycle failures include debug details. |

## 🔍 Diagnostics

Structured debug details are enabled at `DebugLevel.BASIC` by default:

```java
String json = context.debugDetailsJson();
```

Use trace mode when you need to inspect the resolver path:

```java
SystemTestContext context =
    new SystemTestContextBuilder(OrderWorkflow.class)
        .mock(InventoryGateway.class)
        .debug(DebugLevel.TRACE)
        .build();
```

Debug details answer questions such as:

- 🧭 Which root classes were resolved?
- 🕵️ Which dependencies became spies?
- 🎭 Which dependencies became explicit or automatic mocks?
- 🧩 Which dependency path caused a failure?

See the [Technical reference](docs/TECHNICAL.md) for resolver rules, debug levels, and lifecycle
details.

## 🧱 Supported Model

- ✅ Concrete classes become real instances wrapped as Mockito spies.
- ✅ Explicit `.mock(type)` dependencies are Mockito mocks.
- ✅ Interfaces or abstract classes with no implementation become automatic mocks.
- ✅ Interfaces or abstract classes with one implementation resolve to a spy.
- ✅ Interfaces or abstract classes with many implementations fail fast.
- ✅ Constructor injection supports `@Inject`.
- ✅ Field injection supports `@Inject` and field-level `@EJB`.
- ✅ Field-injected cycles can be resolved by early spy registration.

## 🚧 Current Limits

This beta intentionally supports a narrow Jakarta wiring model. It does not yet support:

- Setter injection.
- Constructor-only circular dependencies.
- Qualified bean resolution, including `@Named` and custom Jakarta qualifier annotations.
- `@EJB` metadata such as `lookup`, `name`, `beanName`, and `beanInterface`.
- `@EJB` method injection.
- Optional dependencies.
- Collection injection.
- Explicit spy or instance overrides.
- Advanced classpath and package scan customization.

See the [Roadmap](docs/ROADMAP.md) for planned improvements.

## 📚 Documentation

| Document | Purpose |
| --- | --- |
| [Technical reference](docs/TECHNICAL.md) | Resolver rules, debug levels, and lifecycle details |
| [Architecture](docs/ARCHITECTURE.md) | Components and design decisions |
| [Roadmap](docs/ROADMAP.md) | Planned improvements and known gaps |
| [Contributing](CONTRIBUTING.md) | Local development and contribution guide |

## 📄 License

Released under the [MIT License](LICENSE).
