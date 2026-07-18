# ADR-001 — Module boundaries enforced by Spring Modulith

**Status:** accepted (2026-07-18)

## Context

AETHER is a modular monolith. Module boundaries kept only by convention degrade
silently: one convenient import at a time, until every module depends on the
internals of every other. We want a violation to break the build, not to depend
on someone noticing it in code review.

## Decision

- Every feature is a Spring Modulith module: a top-level package under
  `com.aether` (e.g. `habits`, `finances`). `shared/` holds the minimal
  cross-cutting concerns (errors, security, config).
- A module's public API lives in its root package. Everything else —
  controllers, services, repositories, entities — lives in `internal/` and is
  inaccessible to other modules.
- `ModularityTests` runs `ApplicationModules.verify()`: the build fails on any
  access to another module's internals or on a dependency cycle.

Alternative considered: ArchUnit with hand-written rules. It can enforce the
same constraints but with more code of our own and without the generated module
documentation. Spring Modulith is purpose-built for exactly this shape.

## Consequences

- Boundaries are verified automatically on every build; module documentation
  can be generated from the model.
- One extra dependency (lightweight; verification happens in tests — the
  runtime remains a plain monolith).
- Package discipline is mandatory: new code must consciously choose between
  the module's public root and `internal/`.
