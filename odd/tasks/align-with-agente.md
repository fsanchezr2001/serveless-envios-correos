# Feature: Align backend with agente.md specification

## Objective
Bring the codebase into alignment with `agente.md` as the single source of authority: Java 21, single Docker container serving Angular from Spring Boot `static/`, no CORS, H2 persistence, `SENDGRID_FROM` variable, `200 OK` response, and root-level `Dockerfile` + `.dockerignore`.

## Problem
The current code diverges from `agente.md` on Java version (17 vs 21), architecture (separate frontend on :4200 with CORS vs single container serving static/), env variable names (`SENDGRID_FROM_EMAIL` vs `SENDGRID_FROM`), response status (`202 Accepted` vs `200 OK`), entity/package naming (`domain/SentEmail` vs `model/EmailLog`), and missing root `Dockerfile`/`.dockerignore`.

## Why
The user selected `agente.md` as the authoritative specification. The code must match it before any deploy or further iteration.

## Scope
In scope:
- `backend/pom.xml`: Java 17 → 21
- Remove/disable CORS (`CorsConfig`)
- Configure Angular build output to `src/main/resources/static/`
- Create root `Dockerfile` (multi-stage, one container) and root `.dockerignore`
- Align `application.properties`/config with `agente.md` (`sendgrid.from`, `server.port`, H2)
- `EmailController`: return `200 OK` instead of `202 Accepted`
- Entity/package: align with `agente.md` (`model/EmailLog.java`)
- Env variable names: `SENDGRID_FROM`
- Local Docker image build verification

Out of scope (user action required):
- Verify SendGrid sender (Single Sender Verification)
- Deploy to Render and configure variables
- Real end-to-end send test

## Decisions
- `agente.md` is the source of authority; conflicting `.md` files (`README.md`, `odd/tasks/*.md`) are treated as historical and not updated in this feature.
- Java 21 with Spring Boot 3.3.5 (already matches `agente.md` stack).
- Single Docker container; Angular built and copied into Spring Boot `static/`.
- Fail-closed persistence (`PersistingEmailService`) is kept but aligned to `agente.md` naming where applicable.

## Constraints
- No secrets hardcoded; env vars only.
- Java 21, Spring Boot 3.3.5, Maven.
- H2 embedded database.
- One container, no CORS in production.
- Conventional commits, no AI attribution.

## Authorized scope (edit surfaces)
backend/pom.xml
backend/src/main/java/com/serveless/envio/config/CorsConfig.java
backend/src/main/java/com/serveless/envio/controller/EmailController.java
backend/src/main/java/com/serveless/envio/domain/SentEmail.java
backend/src/main/resources/application.yml
backend/Dockerfile
Dockerfile (root, new)
.dockerignore (root, new)
frontend/** (build outputPath config only)

## Checklist (stable task IDs)
- [ ] T1 - Java 17 → 21 in pom.xml and Dockerfiles
- [ ] T2 - Remove/disable CORS (CorsConfig)
- [ ] T3 - Angular build output to backend static/
- [ ] T4 - Create root Dockerfile (multi-stage, single container)
- [ ] T5 - Create root .dockerignore
- [ ] T6 - Align config with agente.md (sendgrid.from, H2, server.port)
- [ ] T7 - EmailController: 202 → 200 OK
- [ ] T8 - Entity/package alignment (EmailLog, model/)
- [ ] T9 - Local Docker image build verification
- [ ] T10 - (USER) Verify SendGrid sender
- [ ] T11 - (USER) Deploy to Render + env vars
- [ ] T12 - (USER) Real end-to-end send test

## Routes
- T1–T9: delegated direct → writer (`gentle-ai-worker`). Trigger: 2+ non-trivial files.
- T10–T12: parent flags as user-action; no code delegation.

## Acceptance criteria
- Backend compiles with Java 21.
- Single root Dockerfile builds one container serving Angular static + Spring Boot API.
- No CORS configuration active.
- `POST /api/emails` returns `200 OK`.
- Config uses `SENDGRID_FROM` and `sendgrid.from`.
- No secrets in repository.

## Progress notes
- `agente.md` selected as authority; `README.md` and `odd/tasks/*.md` conflicts left as-is (historical).
- Existing `PersistingEmailService`/`SentEmail` kept; naming aligned where feasible without breaking behavior.

## Next step
Delegate T1–T9 to `gentle-ai-worker` with this task file and allowed edit surfaces.