# ServelessEnvioCorreo

A minimal full-stack scaffold that sends an email to an end user through SendGrid.

The browser never talks to SendGrid directly. The Angular app calls the Spring Boot
backend, and only the backend holds the SendGrid API key.

## Architecture

```text
Angular (Cloudflare Pages)  ->  Spring Boot (Render)  ->  SendGrid  ->  end user
```

- `frontend/` - Angular 20 standalone application with a reactive email form.
  Deployed as static assets to **Cloudflare Pages**.
- `backend/` - Spring Boot 3 (Java 17) REST API that validates the request and
  sends the email through SendGrid. Deployed to **Render** from the root `Dockerfile`.

The frontend and the API are **different origins** in production, so the Angular
build points at the absolute backend URL and the backend must allow that origin via
`CORS_ALLOWED_ORIGINS`.

### Request flow

1. The Angular form posts to `${apiUrl}/emails`.
2. The backend validates the payload (bean validation) and calls SendGrid.
3. On success the backend returns `202 Accepted` with `{ "success": boolean, "message": string }`.
4. On validation failure it returns `400 Bad Request` with
   `{ timestamp, status, error, message, details: string[] }`.
5. On a SendGrid failure it returns `502 Bad Gateway`.

Health check: `GET /api/emails/health`.

## Prerequisites

- Node.js 20+ (developed against Node 24) and npm.
- Java 17 and Maven (or use the Docker image).
- Docker Desktop, only if you want to build the backend image locally.
- A SendGrid account with a verified sender email address.

## Backend: run locally

The backend reads its configuration from environment variables. It does **not** load
`.env` files natively, so `backend/.env.example` is a **reference only** - copying it
does not configure anything. Set real environment variables in your shell instead.

PowerShell:

```powershell
$env:SENDGRID_API_KEY="SG.your-real-key"
$env:SENDGRID_FROM="no-reply@your-verified-domain.com"
$env:SENDGRID_FROM_NAME="Serveless Envio Correo"
$env:CORS_ALLOWED_ORIGINS="http://localhost:4200"
$env:PORT="8080"
```

Then, from `backend/`:

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080` by default.

## Frontend: run locally

From `frontend/`:

```powershell
npm install
npm start
```

`npm start` runs `ng serve` and serves the app at `http://localhost:4200`.
In development mode the frontend uses `src/environments/environment.development.ts`,
which points at `http://localhost:8080/api`.

## Production builds

Frontend (from `frontend/`):

```powershell
npm run build
```

The output is written to `frontend/dist/frontend/browser`. `Cloudflare Pages` serves
that directory; the `public/_redirects` and `public/_headers` files are copied into the
build output and control SPA routing and cache headers.

Before deploying, set the production API URL in `src/environments/environment.ts`.

Backend (from the repository root):

```powershell
docker build -t envio-correo-api .
docker run -p 8080:8080 -e SENDGRID_API_KEY=... -e SENDGRID_FROM=... envio-correo-api
```

The root `Dockerfile` builds **only the backend**. Angular is not embedded in the JAR.

## Environment variables

| Variable               | Required | Description                                                        |
| ---------------------- | -------- | ------------------------------------------------------------------ |
| `SENDGRID_API_KEY`     | Yes      | SendGrid API key. Must never be committed or sent to the browser.  |
| `SENDGRID_FROM`        | Yes      | Sender address. Must be a verified sender in SendGrid.             |
| `SENDGRID_FROM_NAME`   | No       | Display name for the sender. Defaults to `Serveless Envio Correo`. |
| `CORS_ALLOWED_ORIGINS` | No       | Comma-separated origins allowed by CORS. Defaults to `http://localhost:4200`. |
| `PORT`                 | No       | HTTP port for the backend. Defaults to `8080`.                     |

## Deployment

### Frontend -> Cloudflare Pages

1. Cloudflare dashboard -> **Workers & Pages** -> **Create** -> **Pages** ->
   **Connect to Git** and select this repository.
2. Build settings:
   - **Root directory**: `frontend`
   - **Build command**: `npm ci && npm run build`
   - **Build output directory**: `dist/frontend/browser`
3. Deploy. The site is published at `https://<project>.pages.dev`.
4. Copy that URL - it is the origin the backend must allow.

### Backend -> Render

1. Render dashboard -> **New** -> **Blueprint** and select this repository.
   Render reads `render.yaml` and creates the `envio-correo` web service, building the
   root `Dockerfile` and health-checking `/api/emails/health`.
2. Fill in the `sync: false` secrets in the Render dashboard:
   `SENDGRID_API_KEY`, `SENDGRID_FROM`, `SENDGRID_FROM_NAME`, `DB_URL`, and
   `CORS_ALLOWED_ORIGINS`.
3. Set `CORS_ALLOWED_ORIGINS` to the Pages URL from the previous section,
   e.g. `https://envio-correo.pages.dev`.
4. Note the resulting service URL (e.g. `https://envio-correo.onrender.com`) and put it
   in `frontend/src/environments/environment.ts` as `apiUrl`, then redeploy the Pages
   project.

The two services are independent: a backend change does not rebuild the frontend, and
vice versa.

> **Render free plan:** the service sleeps after a period of inactivity, so the first
> request after waking takes tens of seconds and the in-memory H2 database is reset.
> Attach a paid instance for an always-on API.

## Security

- **Never commit secrets.** `SENDGRID_API_KEY` and any other credential must live only
  in real environment variables (Render Environment in production, `$env:...`
  locally). No `SG.` key should ever appear in the repository.
- `backend/.env.example` contains empty placeholders and is safe to commit; real values
  must never be written into it.
- The SendGrid key stays on the backend. The frontend only knows the backend API URL,
  so there is no secret in the browser bundle.
- Keep `CORS_ALLOWED_ORIGINS` restricted to the origins that actually need access.

## Troubleshooting

### `403 Forbidden` from SendGrid (unverified sender)

SendGrid rejects the request when the sender address is not verified. Verify it under
**Settings -> Sender Authentication** in SendGrid, or use a domain you have
authenticated, then restart the backend with the corrected value.

### CORS errors in the browser

A CORS error means the browser origin is not in `CORS_ALLOWED_ORIGINS`. Add the exact
frontend origin (scheme + host, for example `http://localhost:4200` or
`https://envio-correo.pages.dev`), restart the backend, and reload. Multiple origins are
comma-separated. The backend allows `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS` with
any header and does not allow credentials.

### `Cannot reach the server` in the frontend

The Angular app could not reach the backend. Confirm the backend is running and that
`apiUrl` in the active environment file points to the correct host. On Render's free
plan, a cold start can take tens of seconds.
