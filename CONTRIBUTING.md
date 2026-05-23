# Contributing

Thank you for improving System Test Container. This guide covers code, tests, and documentation
changes.

## Quick Checklist

- [ ] The change is focused.
- [ ] Tests cover new or changed behavior.
- [ ] User-facing behavior is documented.
- [ ] Local checks pass.

## Requirements

- Java 17 or newer.
- Maven.

The project compiles with `maven.compiler.release=17`, so built bytecode targets Java 17.

## Workflow

1. Create a branch from `main`.
2. Make one focused change.
3. Add or update tests for behavior changes.
4. Update documentation for user-visible changes.
5. Run the local checks.
6. Open a pull request.

## Local Checks

Before opening a pull request, run:

```bash
mvn spotless:apply
mvn test
```

For a full local verification:

```bash
mvn verify
```

For public API changes, also check generated Javadocs:

```bash
mvn javadoc:javadoc
```

## Pull Requests

A pull request should include:

- A clear problem statement.
- A short description of the solution.
- Tests for new or changed behavior.
- Documentation updates when behavior, configuration, or limitations change.

Keep one pull request focused on one topic.

Recommended shape:

```text
Problem
Short description of what was missing or broken.

Solution
Short description of the change.

Tests
Commands run locally.
```

## Commit Messages

Use short, specific commit messages:

```text
Add EJB field injection support
Refactor resolver path tracking
Document debug levels
```

Avoid vague messages such as `fix`, `update`, or `changes`.

## Tests

Tests are organized by behavior under `src/test/java/io/systemtest/container/cases`:

- `basic`: core graph resolution.
- `injection`: field injection.
- `ejb`: field-level `@EJB`.
- `implementation`: implementation discovery.
- `circular`: supported field cycles.
- `failure`: unsupported graph failures.
- `debug`: debug details.
- `junit`: JUnit lifecycle extension.
- `multiroot`: shared multi-root contexts.
- `complex`: realistic mixed graph.

Add a new package when a scenario needs its own fixture graph.

## Code Guidelines

- Keep the public API small.
- Prefer clear errors over silent fallback.
- Keep runtime behavior independent from CDI, EJB, servlet runtimes, and application servers.
- Put implementation details under `io.systemtest.container.internal`.
- Use deterministic ordering where it improves diagnostics.
- Keep methods focused and readable.

## Documentation Guidelines

- Keep the README user-focused.
- Put resolver and lifecycle internals in `docs/TECHNICAL.md`.
- Put component relationships and design decisions in `docs/ARCHITECTURE.md`.
- Update `docs/ROADMAP.md` when a limitation becomes supported or a new known gap is added.
- Avoid promising release dates or repository availability before they exist.

## Publishing

Maven Central publishing is manual and restricted to repository admins through the protected
`maven-central` GitHub environment.
