# Feature: Send-email scaffold (Angular -> Spring Boot -> SendGrid)

Status: Scaffold complete and locally verified - ready to deploy

## Objective
Create a minimal, runnable full-stack scaffold that sends an email to an end user
following the flow `Angular -> Spring Boot -> SendGrid`, deployed on Railway.

## Problem
`ServelessEnvioCorreo/` is empty (0 files). There is no base project to build the
email business logic on.

## Why
The user asked to create the backend + frontend first, then iterate the business
logic on top of it.

## Scope
- Backend: Spring Boot 3 (Java 17) with `POST /api/emails`, bean validation,
  SendGrid integration, uniform error handling, CORS.
- Frontend: Angular 20 standalone app with a reactive form + HTTP service.
- Docs: run + deploy instructions.

Out of scope (decided): persistence, auth, SendGrid dynamic templates, async queues,
rate limiting implementation.

## Decisions
- D1 - Deployment target: Railway, used for deploy only. Render stays as a
  documented alternative (`render.yaml`). Decided by the user.
- D2 - No database. The user explicitly chose "Railway solo para desplegar", so the
  backend is stateless: it sends and returns, it does not log or deduplicate sends.
- D3 - Validation message language is pinned to English via a fixed locale resolver,
  so responses do not depend on the host JVM locale (dev machine was returning
  Spanish, a container would return English). Both layers stay consistent.
- D4 - Not a git repository, so there are no work-unit commits and no native review.

## Constraints
- SendGrid API key ONLY via environment variable (never hardcoded).
- `from` sender must be verified in SendGrid.
- Validation enforced on BOTH layers; backend is the source of truth.
- Secrets must never reach the frontend.

## Authorized scope (edit surfaces)
- `backend/**`
- `frontend/**`
- `README.md`
- `.gitignore`
- `render.yaml`
- this task file (`odd/tasks/scaffold-envio-correo.md`)

## Checklist (stable task IDs)
- [x] T1 - Backend scaffold: pom, config, controller, service, DTOs, errors, CORS
- [x] T2 - Backend verify: `mvn -q test` (compiles + unit test on SendGrid service)
- [x] T3 - Frontend scaffold: CLI-generated Angular app + form/service/models/env
- [x] T4 - Frontend verify: `npm run build`
- [x] T5 - Root README + .gitignore + deploy notes
- [x] T6 - Backend boot smoke check (health, validation, provider-failure mapping)

## Routes
- T1/T2: delegated direct -> writer (`gentle-ai-worker`). Trigger: 2+ non-trivial files.
- T3/T4/T5: delegated direct -> writer (`gentle-ai-worker`). Trigger: 2+ non-trivial files.
- T6: parent inline (bounded local run + HTTP probe).
- Parent spot check: re-ran `npm run build` after the writer returned.

## Acceptance criteria
- Backend compiles, tests pass, exposes `POST /api/emails`.
- Frontend builds and can submit the form to the backend.
- No secret is hardcoded anywhere.

## Verification evidence
| Check | Command | Result |
|---|---|---|
| Backend compile + unit test | `mvn -q test` (in `backend/`) | PASS (writer-reported) |
| Frontend build | `npm run build` (in `frontend/`) | PASS - 212.70 kB initial / 60.84 kB transfer, ~9.7s (parent spot check) |
| Backend boot + health | `mvn spring-boot:run`, `GET /api/emails/health` | PASS - 200 `{"status":"ok"}`, listens on `PORT` |
| Invalid payload handling | `POST /api/emails` with empty fields | PASS - 400 + `details[]` per field |
| Provider failure mapping | `POST /api/emails` with a dummy key | PASS - 502 (SendGrid rejected, mapped) |
| Locale determinism | `POST /api/emails` after D3 | PASS - `must not be blank` (was Spanish before the fix) |

Not verified: a real successful send (needs a rotated key and a verified sender).

## Progress notes
- Angular 20 `ng new` produced the new naming style (`app.ts` / class `App`); root
  generated files were not renamed. Component files are `email-form.ts` etc.
- Writer deviation, accepted: the service import path is `../../environments/environment`
  (the original spec said one level shallower). Verified by a reproduced build.
- No frontend unit-test runner is configured (no headless browser); the build, which
  includes template type-checking, is the verification of record for T4. This is an
  explicit testing exception, not silent coverage.
- The backend had never actually booted until T6; `mvn test` is a unit test that does
  not load the Spring context. T6 closed that gap.
- The frontend renders backend validation `details[]` verbatim, which is why the
  locale fix in D3 matters to the end user.
- `frontend/src/environments/environment.ts` still holds the placeholder
  `https://REPLACE-WITH-YOUR-BACKEND-URL/api` and must be set before the production build.

## Next step
Deploy: rotate credentials, verify the SendGrid sender, then deploy the backend
(Railway, `backend/` + Dockerfile) and the frontend, and wire
`CORS_ALLOWED_ORIGINS` to the deployed frontend URL.
