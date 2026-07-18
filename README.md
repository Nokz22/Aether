# AETHER

A personal **AI Operating System** — a single, calm, premium interface to run your life: projects, calendar, habits, finances, notes, and a personal AI layer that ties them together.

> *The foundation matters more than the feature.*

## Stack

- **Backend** — Java 21, Spring Boot 3.x, PostgreSQL, Flyway
- **Frontend** — Next.js (App Router), TypeScript, Tailwind CSS, Radix UI, Framer Motion, TanStack Query
- **Contract** — OpenAPI; the frontend consumes typed clients generated from it

## Structure

A modular monolith in a single monorepo:

```
aether/
├── backend/    # Java + Spring Boot API
├── frontend/   # Next.js + TypeScript web app
└── docs/       # architecture notes and decision records (ADRs)
```

## Status

Early foundation — repository scaffolding in progress.

## Author

Nokz22 — [github.com/Nokz22](https://github.com/Nokz22)
