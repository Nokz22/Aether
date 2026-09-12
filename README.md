# AETHER

[![CI](https://github.com/Nokz22/Aether/actions/workflows/ci.yml/badge.svg)](https://github.com/Nokz22/Aether/actions/workflows/ci.yml)

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

## The assistant

The `ai` module orchestrates the others through their public APIs (ADR-002).
It can read and write on your behalf — and deliberately cannot delete
anything.

Its key comes from the environment and is never committed. Without it the
application still starts and only `POST /api/ai/chat` refuses, with
`ai_not_configured`:

```bash
export ANTHROPIC_API_KEY=...   # required for the assistant, nothing else
export AI_MODEL=claude-sonnet-5             # optional, this is the default
export AI_BASE_URL=https://api.anthropic.com # optional, this is the default
```

## Status

Early foundation — repository scaffolding in progress.

## Author

Nokz22 — [github.com/Nokz22](https://github.com/Nokz22)
