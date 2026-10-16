# Persist sent emails (full content)

## Objective
Persist the FULL content of every email sent by the backend (to/subject/message and metadata) in a managed PostgreSQL database, using a reliable write-then-send strategy. This makes the archive auditable without inventing a partial "audit-only" schema.

## Problem statement
The user chose full email content persistence (not audit-only). Current backend is stateless (no DB). We need to add persistence while keeping provider code minimally touched and failure semantics clear.

## Why
- Regulatory/audit: keep complete sent email content (single HTML `message` as sent).
- Recoverability/traceability: link each send attempt to a stored record and capture provider message id when available.
- Avoid data loss of intent: record that an email was attempted before or regardless of delivery details in a controlled way.

## Scope
**In-scope**
- Add JPA + PostgreSQL deps; domain entity + repository.
- Write-then-send decorator `PersistingEmailService` implementing `EmailService` (delegates to `SendGridEmailService`).
- Capture `X-Message-Id` from SendGrid response headers (case-insensitively).
- Store full content: to, toName, subject, body (HTML; single field matching `SendEmailRequest.message`), status, sentAt, providerMessageId, errorMessage.
- H2-based tests (Docker unavailable). Prod uses Render managed Postgres.
- Update `application.yml`, `.env.example`, `render.yaml` (add Postgres block + datasource wiring), `README.md`, supersede DB-less statements.
- Minimal edits to `SendGridEmailService` (header extraction only).

**Out of scope**
- Attachments, cc/bcc, templates, retries/backoff, idempotency keys, soft-delete/archival policy, PII redaction, retention automation.
- Migrating existing data (none).
- Switching provider.

## Decisions (D1-D6)
- **D1 Engine**: Render managed Postgres (no managed MySQL on Render). Self-hosted MySQL on a disk ruled out for free plan.
- **D2 Content**: Store single `body` (HTML) — matches current `text/html` send; do NOT split bodyText/bodyHtml. Subject max 150 per DTO; message max 5000.
- **D3 Failure semantics**: write-then-send. INSERT row `status=PENDING` → call provider → UPDATE to `SENT` (set `providerMessageId`, `sentAt`) or `FAILED` (set `errorMessage`, `sentAt`). Rationale: `X-Message-Id` only exists after send; must record intent before/at attempt boundary.
- **D4 Fail-closed**: if the initial PENDING insert fails, do NOT send. Rethrow as server error (don't send what can't be recorded). Tradeoff: DB outage blocks delivery. Reversible if policy changes.
- **D5 Transactions**: do NOT wrap the whole orchestration in a single `@Transactional`. The FAILED update must not roll back when `EmailSendException` propagates. Use separate transactions (e.g. `@Transactional(propagation = REQUIRES_NEW)` for the final write, or repository saves in their own transaction boundaries).
- **D6 Datasource URL**: Render injects `postgresql://...` (not `jdbc:postgresql://`). Spring Boot JDBC needs `jdbc:postgresql://`. Convert `postgresql://` → `jdbc:postgresql://` in configuration (or inject `SPRING_DATASOURCE_URL` as `jdbc:postgresql://...`). Also handle `?sslmode=require` for Render.

## Constraints
- Java 17, Spring Boot 3.3.5. No Testcontainers (Docker daemon off) → H2 Postgres-compat for tests.
- Prod schema: `ddl-auto: validate` + Flyway (`flyway-core`, `flyway-database-postgresql`) to catch drift vs entity-generated schema in tests.
- Single writer at a time (gentle-ai-worker). Follow existing code style, no AI attribution in commits.
- PowerShell 5.1 gotchas (no `&&`, no ternary `? :`, use `;`).
- Credentials: never commit real keys/URLs; placeholders only.
- Render Free Postgres expires 30 days after creation (14-day grace then deletion). Archive becomes self-deleting on free tier — document this tradeoff in README/ODD.
- Blueprint applies only from a Git repo.
- CORS and frontend `apiUrl` must stay consistent with backend origin.

## Entity (proposed)
Table `sent_emails`:
- id (UUID, PK)
- to (VARCHAR(255), not null)
- to_name (VARCHAR(255))
- subject (VARCHAR(150), not null)
- body (TEXT, not null) — HTML as sent
- status (VARCHAR(20), not null) — PENDING/SENT/FAILED
- sent_at (TIMESTAMP, not null)
- provider_message_id (VARCHAR(255))
- error_message (VARCHAR(2000))

Index ideas (optional): `idx_sent_emails_sent_at`.

## Design sketch
- `domain/SentEmail.java`: JPA entity with `@Enumerated(EnumType.STRING)` or plain string for status; `@PrePersist` sets sentAt? Or set on transition. For PENDING insert, sentAt can be `now()` at insert time (attempt time). Update sentAt again on transition to SENT/FAILED (or keep attempt time) — choose attempt time for consistency.
- `repository/SentEmailRepository.java`: `JpaRepository<SentEmail, UUID>`.
- `service/PersistingEmailService implements EmailService`:
  - fields: `delegate` (SendGridEmailService), `repository` (SentEmailRepository)
  - `@Primary` or explicit `@Bean` wiring; constructor injection.
  - `send(request)`: save PENDING → try delegate.send → on success, load/update to SENT + providerMessageId + sentAt → return response; on exception, update to FAILED + errorMessage + sentAt → rethrow.
- `SendGridEmailService`: after `client.mail().send(request)`, extract `response.getHeaders()` for `X-Message-Id` (Header names case-insensitive). Return providerMessageId in response or pass via exception context? Current `SendEmailResponse` has `success,message`. Consider extending or logging — minimal change: capture and make available to persister (persister can read after delegate returns? Or have delegate return the id). Alternatively, have delegate throw a custom exception that carries providerMessageId, or have persister inspect response headers indirectly — easier: modify delegate to attach providerMessageId to the successful path by having it set on a holder? Or change `EmailService.send` to return providerMessageId? But DTO is fixed. Simpler: in persisting service, after successful delegate call, we don't have headers. So modify `SendGridEmailService.send` to return `(SendEmailResponse, providerMessageId)`? But interface changes. Alternative: introduce a small result object (`SendResult{response,id}`) or extend response. Or have delegate store last message id in a threadlocal (less clean). Better: change `EmailService` signature to return a richer result, but that touches controller. Minimal invasive: have `SendGridEmailService` capture last providerMessageId (volatile field) set after success and expose via a package-private getter; persister can call it after delegate returns. Acceptable for this scope.
- `application.yml` (prod): `spring.datasource.url=${SPRING_DATASOURCE_URL:${DATABASE_URL}}` and convert `postgresql://` to `jdbc:postgresql://`. Also set `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true`, connection pool sane defaults. Flyway enabled.
- `application-test.yml` (or via `@ActiveProfiles("test")`): H2 `jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, `ddl-auto=create-drop` or `validate` with Flyway disabled (`spring.flyway.enabled=false`).
- `render.yaml`: add
  ```yaml
  databases:
    - name: envio-correo-db
      plan: free  # NOTE: expires 30 days after creation
      region: oregon
  services:
    - type: web
      ...
      envVars:
        - key: SPRING_DATASOURCE_URL
          fromDatabase:
            name: envio-correo-db
            property: connectionString
        # convert postgresql:// -> jdbc:postgresql:// in app, or compute
  ```
  But need URL conversion. Add a small `DataSourceConfig` that rewrites `spring.datasource.url` if it starts with `postgresql://`.
- `.env.example`: add `DATABASE_URL` (optional local), `SPRING_DATASOURCE_URL`, Flyway notes.
- `README.md`: document Render Free Postgres 30-day expiry + paid requirement for permanent archive; security note about retention/PII.

## Checklist
- [ ] `odd/tasks/persist-sent-emails.md` written (this file) and mirrored to Engram.
- [ ] Supersede DB-less: update `render.yaml` line-13 comment; update `odd/tasks/scaffold-envio-correo.md` D2/status notes.
- [ ] `pom.xml`: add deps (starter-data-jpa, postgresql runtime, h2 test).
- [ ] `domain/SentEmail.java` + repository.
- [ ] `PersistingEmailService` (write-then-send, no single spanning @Transactional; fail-closed on initial insert).
- [ ] `SendGridEmailService`: extract X-Message-Id from response headers (case-insensitive); expose via minimal mechanism to persister.
- [ ] `EmailController` wiring: ensure `@Primary` or `@Bean` makes persisting service the one injected (keep SendGrid service as delegate).
- [ ] URL conversion config (postgresql:// → jdbc:postgresql://).
- [ ] `application.yml` updated (datasource, JPA validate, Flyway). `application-test.yml` with H2 Postgres-compat + Flyway off.
- [ ] Flyway migration: `V1__create_sent_emails.sql` (or equivalent) matching entity; keep minimal.
- [ ] Tests: PENDING→SENT, PENDING→FAILED, insert-fails⇒no-send; repository tests with H2.
- [ ] `render.yaml`: add database block + env wiring.
- [ ] `.env.example`, `README.md` updated (30-day expiry tradeoff, retention note).
- [ ] `mvn test` passes.
- [ ] Security scan: no secrets introduced.

## Verification
1. `mvn -q test` → all green.
2. Local (optional, no Postgres): tests use H2.
3. Render deploy: Blueprint creates DB; backend connects via converted JDBC URL; health still `200`; sending email creates a `SENT` row with providerMessageId.
4. Simulate provider failure (dummy key or mock): creates `FAILED` row with errorMessage.
5. Insert failure path: if repo.save(PENDING) throws, provider never called.

## Next step
After this ODD is approved/mirrored, delegate implementation to `gentle-ai-worker` with this task file attached.
