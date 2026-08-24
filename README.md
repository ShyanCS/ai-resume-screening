# AI-Powered Resume Screening & Interview Assistant

A multi-tenant, SaaS-style recruitment platform: resume parsing, candidate–job
match scoring with transparent breakdowns, skill-gap analysis, and AI-assisted
interview question generation — with a deterministic rule-based fallback so the
product works even when external AI services are unavailable.

> Status: **Phase 0 — project scaffolding in progress.** Not yet deployable.

## Tech Stack

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 21 · Spring Boot 3.x · Spring Security (JWT) · Spring Data JPA |
| Database  | PostgreSQL 16 · Flyway migrations |
| Frontend  | React 18 · Vite · TypeScript · Material UI · TanStack Query |
| Parsing   | Apache Tika (PDF/DOCX text extraction) |
| AI        | Gemini API behind a provider interface; template/rule-based fallback default |
| Storage   | S3-compatible object storage (MinIO locally) |
| Email     | MailDev (dev) / Resend SMTP (prod) |
| CI        | GitHub Actions — lint, typecheck, test, coverage on every push |

## Repository Layout

```
apps/
  api/     Spring Boot REST API
  web/     React SPA
infra/     Docker Compose stack & deployment config
docs/      Architecture notes and ADRs
```

## Development (quickstart coming with Phase 0 completion)

```bash
# full stack via Docker (once infra lands)
docker compose up --build
```

## License

Proprietary — all rights reserved until public release is decided.
