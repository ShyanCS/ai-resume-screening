# Changelog

All notable changes to this project are documented in this file.
The format follows Keep a Changelog; versioning follows SemVer.

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
