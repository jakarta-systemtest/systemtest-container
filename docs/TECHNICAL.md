# Technical Reference

System Test Container is a test-time object graph resolver. It builds one shared context from root
classes, resolves dependencies, wraps real objects as Mockito spies, and uses Mockito mocks for
declared or unresolved external dependencies.

It does not start CDI, EJB, servlet infrastructure, or an application server.

Use this document when you need implementation-level behavior. For installation and first-use
examples, start with the [README](../README.md).

## Public API

```text
io.systemtest.container.api.context      context and builder API
io.systemtest.container.api.config       configuration enums
io.systemtest.container.api.extension    JUnit extension API
io.systemtest.container.api.debug        public debug model
io.systemtest.container.api.exception    public exceptions
```

Only `io.systemtest.container.api` packages are intended for users. Internal packages can change
while the project is in beta.

## Build Flow

```mermaid
sequenceDiagram
    participant Test
    participant Builder as SystemTestContextBuilder
    participant Resolver as SystemTestContextResolver
    participant Registry as BeanRegistry
    participant Context as SystemTestContext

    Test->>Builder: new(rootA, rootB)
    Test->>Builder: mock(ExternalType)
    Test->>Builder: build()
    Builder->>Resolver: resolve()
    Resolver->>Registry: register explicit mocks
    loop each root type
        Resolver->>Registry: resolve or reuse object
    end
    Resolver->>Context: immutable snapshots
    Builder-->>Test: cached context
```

A successful `build()` is cached by the builder instance. Later calls return the same context.

Root classes are only entry points for resolution. After the graph is built, users retrieve root
objects the same way as any other real object:

```java
ReservationEndpoint endpoint = context.spy(ReservationEndpoint.class);
```

## Resolution Rules

```mermaid
flowchart TD
    A["resolve(type)"] --> B{"registered?"}
    B -->|yes| C["reuse spy or mock"]
    B -->|no| D{"interface or abstract?"}
    D -->|no| E["instantiate concrete class"]
    E --> F["wrap as Mockito spy"]
    F --> G["early register spy"]
    G --> H["inject @Inject / @EJB fields"]
    H --> I["mark complete"]
    D -->|yes| J["scan implementations"]
    J --> K{"count"}
    K -->|0| L["automatic mock"]
    K -->|1| M["resolve implementation as spy"]
    M --> N["register alias"]
    K -->|many| O["fail ambiguous"]
```

Resolver behavior:

- Explicit mocks are created before roots are resolved.
- Registered spies and mocks are reused across all roots.
- Concrete classes are instantiated and wrapped as Mockito spies.
- Spies are registered before field injection so field-based cycles can resolve.
- Interfaces and abstract classes use implementation discovery.
- No discovered implementation creates an automatic mock.
- One discovered implementation resolves to a spy and is aliased to the requested type.
- Multiple discovered implementations fail with `AmbiguousImplementationException`.
- Constructor-only cycles fail because no instance exists to register early.

## Constructor Injection

Constructor selection follows this order:

1. Use the single constructor annotated with `jakarta.inject.Inject`.
2. Otherwise, use the default constructor.
3. Otherwise, use the only declared constructor.

Resolution fails when:

- More than one constructor has `@Inject`.
- There is no default constructor and more than one non-annotated constructor exists.
- A constructor dependency creates a cycle before an instance can be registered.

## Field Injection

Field injection supports:

- `jakarta.inject.Inject`.
- Field-level `jakarta.ejb.EJB`.
- Fields declared on superclasses.

Injected fields must not be `static` or `final`.

For `@EJB`, the resolver uses the Java field type. It does not read attributes such as `lookup`,
`name`, `beanName`, or `beanInterface`.

## Implementation Discovery

```mermaid
flowchart TD
    A["abstract dependency"] --> B["scan dependency package"]
    B --> C{"concrete assignable classes"}
    C -->|0| D["automatic mock"]
    C -->|1| E["resolve as spy"]
    C -->|many| F["ambiguous failure"]
```

The scanner is intentionally small in the beta:

- It scans the package of the requested interface or abstract class.
- It walks file-based classpath entries for that package.
- It ignores non-file classpath resources.
- It filters for concrete classes assignable to the requested type.
- It sorts candidates by class name for deterministic diagnostics.

Jar and advanced classpath scanning are planned improvements.

## Registry Model

```mermaid
flowchart LR
    Beans["beans map<br/>spies + mocks"] --> Resolver["resolver reuse"]
    Spies["spies map<br/>real objects + aliases"] --> SpyApi["context.spy(type)"]
    Mocks["mocks map<br/>explicit + automatic"] --> MockApi["context.mock(type)"]
    States["states map<br/>CREATING / REGISTERED / COMPLETE"] --> Resolver
```

The public API keeps spies and mocks separate:

- `spy(type)` returns only real objects wrapped as Mockito spies.
- `mock(type)` returns only Mockito mocks.
- Alias types can point to the same spy instance.
- Lifecycle callbacks run once per unique spy instance.

## Object States

```mermaid
stateDiagram-v2
    [*] --> CREATING: resolution starts
    CREATING --> REGISTERED: spy exists before field injection
    REGISTERED --> COMPLETE: field injection finished
    COMPLETE --> COMPLETE: reused later
```

`REGISTERED` supports field cycles:

```text
A
  -> B
      -> A
```

When `B` asks for `A`, the resolver can reuse the early registered `A` spy.

## Debug Model

```mermaid
flowchart LR
    Resolver["Resolver"] --> Builder["ResolutionDebugDetails.Builder"]
    Builder --> Summary["summary"]
    Builder --> Errors["errors"]
    Builder --> Graph["graph nodes<br/>TRACE"]
    Builder --> Steps["steps<br/>TRACE"]
    Builder --> Details["ResolutionDebugDetails"]
    Details --> Json["debugDetailsJson()"]
```

Debug levels:

- `OFF`: returns the report shape without registered types, graph nodes, steps, or errors.
- `BASIC`: includes success, timing, registered spies, registered mocks, and errors.
- `TRACE`: adds graph nodes, resolver steps, paths, and step timings.

The builder keeps debug details from the last build attempt, including failed builds.

## Logging

The library uses `java.lang.System.Logger`.

```mermaid
flowchart LR
    Code["resolver / scanner / extension"] --> Gate["LogLevel gate"]
    Gate -->|enabled| Logger["System.Logger"]
    Gate -->|disabled| Drop["skip"]
```

`LogLevel` controls emitted logs. `DebugLevel` controls structured diagnostics. The default log
level is `WARN`, which keeps normal test runs quiet.

## Lifecycle Extension

```mermaid
sequenceDiagram
    participant JUnit
    participant Extension as SystemTestExtension
    participant Context as SystemTestContext
    participant Invoker as BeanLifecycleInvoker
    participant Test

    JUnit->>Test: @BeforeEach
    JUnit->>Extension: beforeTestExecution
    Extension->>Context: spies()
    Extension->>Invoker: @PostConstruct once per unique spy
    JUnit->>Test: test method
    JUnit->>Test: @AfterEach
    JUnit->>Extension: afterEach
    Extension->>Invoker: @PreDestroy once per unique spy
```

Lifecycle methods can have any visibility. They must be non-static and have no parameters.

Strict mode throws lifecycle failures. Lenient mode records lifecycle failures in debug details and
allows the test to continue.
