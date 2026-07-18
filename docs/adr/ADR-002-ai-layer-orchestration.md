# ADR-002 — AI layer as orchestration over public module APIs

**Status:** accepted (2026-07-18)

## Context

The AI layer crosses data from every module (habits, finances, calendar,
notes…). If it reached directly into other modules' repositories, entities or
tables, it would become a super-module coupled to everything — defeating the
boundaries established in ADR-001 and making every module change a potential
AI breakage.

## Decision

- The `ai/` module is an orchestrator: it consumes **exclusively the public
  APIs** of the other modules — never their repositories, entities or tables.
- It owns persistence only for its own state (e.g. conversations,
  preferences). It has no access to the domain tables of other modules.
- The boundary rules of ADR-001 apply to `ai/` without exception; the same
  `ModularityTests` enforce this decision.

## Consequences

- Modules evolve freely without breaking the AI layer: the contract between
  them is explicit and compiler-checked.
- What the AI layer can "see" is auditable — it is exactly the sum of the
  public module APIs.
- Deliberate friction: sometimes a module must first expose a new public
  method before the AI layer can use it. That friction is the price of real
  boundaries, and it is intended.
