# AETHER

[![CI](https://github.com/Nokz22/Aether/actions/workflows/ci.yml/badge.svg)](https://github.com/Nokz22/Aether/actions/workflows/ci.yml)

A personal **AI Operating System** — a single, calm, premium interface to run your life: projects, calendar, habits, finances, notes, and a personal AI layer that ties them together.

> *The foundation matters more than the feature.*

## Stack

- **Backend** — Java 21, Spring Boot 3.x, Spring Modulith, PostgreSQL, Flyway
- **Frontend** — Next.js (App Router), TypeScript, Tailwind CSS, TanStack Query
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

## Running it

```bash
cp .env.example .env          # then set JWT_SECRET, and the AI key if you want it
docker compose up --build
```

The app is then on <http://localhost:3000>, and that is the **only** published
port: the API is reachable through it, not beside it. `/api/*` is proxied on to
the backend over the internal network, so the browser only ever talks to one
origin — no CORS, and the refresh cookie (`SameSite=Strict`) always travels.
Deployments keep that shape: one public app, the API private behind it.

Registration is closed by default. To create the first account, start once with
`OPEN_REGISTRATION=true`, register, then set it back to `false`.

To work on the code instead, run the two sides directly — `mvn spring-boot:run`
in `backend/` and `npm run dev` in `frontend/`, against a local PostgreSQL. The
frontend proxies to `http://localhost:8080` unless `API_INTERNAL_URL` says
otherwise.

## Status

The six modules — habits, notes, projects, calendar, finances and the
assistant — are built, each with its own screen. Next: deployment.

## Author

Nokz22 — [github.com/Nokz22](https://github.com/Nokz22)
