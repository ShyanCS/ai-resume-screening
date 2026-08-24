# Database Migrations

Managed by [Flyway](https://documentation.red-gate.com/fd/quickstart-maven-184127578.html),
executed automatically at API startup once a datasource is configured, and
validated independently by dedicated integration tests that pin each critical
invariant against a live PostgreSQL instance (`MigrationIntegrityTest`).

## Conventions

- Location: `apps/api/src/main/resources/db/migration/`
- Naming: `V<n>__<snake_case_description>.sql` — e.g. `V1__hiresense_core_schema.sql`
- **Forward-only.** Never edit an applied migration; add a new one.
- Every migration must be idempotent-safe under failure (Flyway wraps DDL in a transaction where supported).
- Any new table/constraint ships in the same commit as a test asserting it (see `MigrationIntegrityTest`).

## Table → Feature Lineage Map

| Table | Owning Module | Notes |
|---|---|---|
| `organizations` | Tenancy | Tenant root; unique public `slug` drives `/c/{slug}` careers pages |
| `users` | Auth | Global identity; `platform_role` = PLATFORM_ADMIN or CANDIDATE |
| `org_members` | Tenancy / RBAC | Staff role (ORG_ADMIN/RECRUITER) is scoped here, not on users |
| `candidate_profiles` | Candidate | 1:1 extension of users for candidate-role accounts |
| `skills` | Skills master | Case-insensitive unique name |
| `candidate_skills` | Candidate / Parsing | Source distinguishes parsed vs self-declared |
| `jobs` | Job Management | Org-scoped postings with rich lifecycle |
| `job_skills` | Matching Engine | Requirement level feeds deterministic scoring weights |
| `resumes` | Resume Pipeline | Storage key points at object storage; async parse status |
| `applications` | Matching / Pipeline | Unique per (user, job); score breakdown persisted as JSONB |
| `skill_gap_reports` | Skill Gap | One report per application |
| `interview_questions` | Interview Generator | Tracks source + human-edit flag |
| `notifications` | Notifications | In-app feed; emails are dispatched alongside |
| `audit_logs` | Auditability | Append-only decision trail |
| `refresh_tokens` | Auth | Rotating refresh tokens stored as SHA-256 hashes |
| `email_tokens` | Auth / Invites | Verify, reset, and recruiter-invite tokens |
| `course_resources` | Skill Gap | Recommendation catalog, optionally skill-linked |

## Local Verification

The integrity tests run against the compose Postgres (host port `5433`,
chosen to avoid colliding with any native PostgreSQL install):

```bash
docker compose up -d postgres
cd apps/api
./mvnw test -Dtest=MigrationIntegrityTest
```

If the database is unreachable the tests skip with an explanatory message;
CI always provisions a database, so they always execute there. Connection
settings can be overridden via `HIREDSENSE_TEST_DB_URL`, `HIREDSENSE_TEST_DB_USER`
and `HIREDSENSE_TEST_DB_PASSWORD`.

