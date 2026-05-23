# Roadmap

This roadmap tracks known improvement areas. It is not a release commitment.

## Dependency Resolution

- [ ] Add explicit spy or instance overrides.
- [ ] Add proxy or lazy-reference support for constructor circular dependencies.
- [ ] Support package scan customization.
- [ ] Improve implementation discovery for jar and classpath edge cases.
- [ ] Decide whether implementation discovery should scan from the root package, dependency package,
      or a configured package set.
- [ ] Add qualifier-aware resolution.

## Injection Support

- [ ] Add setter injection.
- [ ] Add `@EJB` method injection.
- [ ] Interpret optional `@EJB` metadata such as `lookup`, `name`, `beanName`, and `beanInterface`.
- [ ] Support `@Named`.
- [ ] Support custom Jakarta qualifier annotations.
- [ ] Support optional dependencies.
- [ ] Support collection injection where practical.

## JUnit Integration

- [ ] Add configurable per-class JUnit extension scope.
- [ ] Add annotation-based JUnit registration.
- [ ] Add automatic context discovery for JUnit tests.
- [ ] Decide whether lifecycle callbacks should run once per context, once per test method, or by
      configurable scope.

## Diagnostics

- [ ] Define compatibility rules for debug JSON fields.
- [ ] Improve failure messages for complex implementation-resolution cases.
- [ ] Add more examples for debug report usage.

## Project Maintenance

- [ ] Add a changelog.
- [ ] Add release notes for published versions.
- [ ] Add issue templates.
- [ ] Add pull request templates.
- [ ] Add `SECURITY.md`.
- [ ] Review whether framework-inspired test annotations fit without adding runtime coupling.
