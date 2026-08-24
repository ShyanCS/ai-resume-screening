# AI-Powered Resume Screening & Interview Assistant ("HireSense")

A multi-tenant, SaaS-style recruitment platform: resume parsing, candidate–job
match scoring with transparent breakdowns, skill-gap analysis, and AI-assisted
interview question generation — with a deterministic rule-based fallback so the
product works even when external AI services are unavailable.

> Status: **Phase 0 — engineering harness complete.** Feature phases land next
> (auth → resume pipeline → jobs → matching → AI modules → deploy).

## Tech Stack

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 21 · Spring Boot 3.5 · Spring Security (JWT) · Spring Data JPA |
| Database  | PostgreSQL 16 · Flyway migrations |
| Frontend  | React 19 · Vite · TypeScript · Vitest + Testing Library |
| Parsing   | Apache Tika (PDF/DOCX text extraction) |
| AI        | Gemini API behind a provider interface; rule-based fallback default |
| Storage   | S3-compatible object storage (MinIO locally) |
| Email     | MailPit (dev) / Resend SMTP (prod) |
| CI        | GitHub Actions — lint, typecheck, test, coverage on every push |

## Quickstart (fresh clone)

Prerequisites: **Docker** plus, for running app code outside containers,
**JDK 21** and **Node.js 22+**.

```bash
git clone <your-fork-url> && cd "AI Resume Screening"
docker compose up --build
```

That single command starts everything:

| Service | URL | Notes |
|---|---|---|
| Web app | http://localhost:3000 | nginx; proxies `/api/*` to the API |
| API | http://localhost:8080 | health at `/api/v1/health` and `/actuator/health` |
| PostgreSQL | localhost:**5433** | port shifted to avoid native-install collisions |
| MinIO console | http://localhost:9001 | login `hiresense` / `hiresense-secret`; `resumes` bucket auto-created |
| MailPit | http://localhost:8025 | fake SMTP; all dev email lands here |

No external accounts or API keys are needed — the AI layer runs in its
template/rule mode by default.

## Backend development

```bash
cd apps/api

# Windows: point JAVA_HOME at JDK 21 for this session if your default is older
# $env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-21.0.12.1+1"

./mvnw verify          # spotless check + unit tests + migration integrity tests + coverage gate
./mvnw spring-boot:run # needs Postgres: docker compose up -d postgres first
```

The migration integrity tests run against the compose Postgres on `localhost:5433`
(creds in `apps/api/.env.example`). If it is down they skip with an explanation;
CI always provisions one.

## Frontend development

```bash
cd apps/web
npm ci
cp .env.example .env   # optional today, required once API client lands
npm run dev            # http://localhost:5173
npm run lint           # eslint flat config (react-hooks rules, no-console)
npm run test:coverage  # vitest with 70% line/function gates
npm run build          # tsc typecheck + production bundle
```

## Repository Layout

```
apps/api/     Spring Boot REST API (Flyway migrations under src/main/resources/db/migration)
apps/web/     React SPA served by nginx in Docker
.github/      CI workflow, nightly security audit, Dependabot config
docker-compose.yml   full stack, one command
```

## Engineering Rules Enforced by CI

- Every push: Spotless (Java formatting), JaCoCo ≥70% line coverage (backend),
  ESLint + Prettier + tsc + Vitest ≥70% coverage (frontend), `npm audit --audit-level=high`.
- Nightly: OWASP dependency-check against Maven deps (`NVD_API_KEY` secret optional but recommended).
- Features land as small commits that include their pinning tests.
