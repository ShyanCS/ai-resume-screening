# Changelog

All notable changes to this project are documented in this file.
The format follows Keep a Changelog; versioning follows SemVer.

## [0.2.0] - 2026-08-25

### Added
- Candidate signup and organization signup endpoints with validation, email normalization, and slug derivation.
- Login issuing HS256 JWT access tokens; refresh-token rotation with reuse detection that revokes the entire session family; logout.
- Email verification flow with single-use hashed tokens; resend never enumerates accounts; best-effort SMTP dispatch via MailPit in development.
- Forgot/reset password with automatic revocation of all active sessions.
- Recruiter invite flow: org-admin-guarded invitation of existing accounts, 7-day expiring tokens carrying organization context (V3 migration), idempotent acceptance.
- JWT authentication filter with 401 entry point, `GET /api/v1/me`, method security enabled.
- Frontend: typed API client with single-flight token refresh on 401, auth context with persisted session, protected routes, zod auth schemas, Material UI login/candidate-signup/organization-signup pages.

### Changed
- Spring Security chain is now default-deny: only health and auth endpoints are public.
- Error responses standardized on RFC 7807 problem details; unexpected errors logged server-side without leaking internals.

## [0.1.0] - 2026-08-24

### Added
- Monorepo scaffold: Spring Boot API (Java 21, Boot 3.5) and React 19 / Vite / TypeScript web app.
- One-command Docker Compose stack: API, web (nginx), PostgreSQL 16, MinIO object storage with auto-created `resumes` bucket, MailPit SMTP catcher.
- Multi-tenant core schema (17 tables: organizations, users, org_members, candidate_profiles, skills, jobs, applications, resumes, audit logs, tokens, notifications, and supporting tables) via Flyway migration V1.
- Database invariant test suite pinning uniqueness, foreign-key, and check-constraint behavior against live Postgres (7 tests).
- Health endpoint (`GET /api/v1/health`) pinned by WebMvcTest slice tests.
- Structured frontend logger and shared ErrorBanner component, each unit-tested; `no-console` enforced everywhere else.
- CI on every push: Spotless format check, JaCoCo 70% line-coverage gate, ESLint flat config, Prettier check, TypeScript typecheck via build, Vitest with 70% coverage thresholds, npm dependency audit.
- Nightly security audit workflow (OWASP dependency-check for Maven, strict npm production audit).
- Dependabot automation for npm, Maven, and GitHub Actions ecosystems.
- Enforced LF line endings repository-wide via .gitattributes.
