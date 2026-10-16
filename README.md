# ServelessEnvioCorreo

A minimal full-stack scaffold that sends an email to an end user through SendGrid.

The browser never talks to SendGrid directly. The Angular app calls the Spring Boot
backend, and only the backend holds the SendGrid API key.

## Architecture

```text
Angular (frontend)  ->  Spring Boot (backend)  ->  SendGrid  ->  end user
```

- `frontend/` - Angular 20 standalone application with a reactive email form.
- `backend/` - Spring Boot 3 (Java 17) REST API that validates the request and
  sends the email through SendGrid.

### Request flow

1. The Angular form posts to `POST /api/emails`.
2. The backend validates the payload (bean validation) and calls SendGrid.
3. On success the backend returns `202 Accepted` with `{ "success": boolean, "message": string }`.
4. On validation failure it returns `400 Bad Request` with
   `{ timestamp, status, error, message, details: string[] }`.
5. On a SendGrid failure it returns `502 Bad Gateway`.

Health check: `GET /api/emails/health`.

## Prerequisites

- Node.js 20+ (developed against Node 24) and npm.
- Java 17 and Maven (or use the Docker image).
- A SendGrid account with a verified sender email address.

## Backend: run locally

The backend reads its configuration from environment variables. It does **not** load
`.env` files natively, so `backend/.env.example` is a **reference only** - copying it
does not configure anything. Set real environment variables in your shell instead.

PowerShell:

```powershell
$env:SENDGRID_API_KEY="SG.your-real-key"
$env:SENDGRID_FROM_EMAIL="no-reply@your-verified-domain.com"
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

The output is written to `frontend/dist/frontend/browser`. Serve those static files
from any web host and set the production API URL in
`src/environments/environment.ts` **before** building.

Backend (from `backend/`):

```powershell
mvn clean package
java -jar target/envio-correo-0.0.1-SNAPSHOT.jar
```

## Environment variables

| Variable               | Required | Description                                                        |
| ---------------------- | -------- | ------------------------------------------------------------------ |
| `SENDGRID_API_KEY`     | Yes      | SendGrid API key. Must never be committed or sent to the browser.  |
| `SENDGRID_FROM_EMAIL`  | Yes      | Sender address. Must be a verified sender in SendGrid.             |
| `SENDGRID_FROM_NAME`   | No       | Display name for the sender. Defaults to `Serveless Envio Correo`. |
| `CORS_ALLOWED_ORIGINS` | No       | Comma-separated origins allowed by CORS. Defaults to `http://localhost:4200`. |
| `PORT`                 | No       | HTTP port for the backend. Defaults to `8080`.                     |

## Deployment

### Railway

Railway builds the backend from `backend/Dockerfile`.

1. Create a new project and point it at this repository.
2. Set the root directory to `backend/` (or configure the Dockerfile path).
3. Add the environment variables from the table above in the Railway dashboard.
4. Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin.
5. After the first deploy, copy the public backend URL into
   `frontend/src/environments/environment.ts` (`apiUrl`) and rebuild the frontend.

### Render

`render.yaml` is a minimal Blueprint that declares a Docker-based `web` service for
the backend using `backend/Dockerfile` and health-checking `/api/emails/health`.

1. Create a new Blueprint in Render and select this repository.
2. Render reads `render.yaml` and creates the service.
3. Fill in the secret values (`sync: false` variables) in the Render dashboard.
4. Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin.

## Security

- **Never commit secrets.** `SENDGRID_API_KEY` and any other credential must live only
  in real environment variables (Railway/Render Variables in production, `$env:...`
  locally). No `SG.` key should ever appear in the repository.
- `backend/.env.example` contains empty placeholders and is safe to commit; real values
  must never be written into it.
- The SendGrid key stays on the backend. The frontend only knows the backend API URL,
  so there is no secret in the browser bundle.
- Keep `CORS_ALLOWED_ORIGINS` restricted to the origins that actually need access.

## Troubleshooting

### `403 Forbidden` from SendGrid (unverified sender)

SendGrid rejects the request when `SENDGRID_FROM_EMAIL` is not a verified sender.
Verify the address under **Settings -> Sender Authentication** in SendGrid, or use a
domain you have authenticated, then restart the backend with the corrected value.

### CORS errors in the browser

A CORS error means the browser origin is not in `CORS_ALLOWED_ORIGINS`. Add the exact
frontend origin (scheme + host + port, for example `http://localhost:4200` or your
deployed frontend URL), restart the backend, and reload. Multiple origins are
comma-separated. The backend allows `GET`, `POST`, and `OPTIONS` with any header and
does not allow credentials.

### `Cannot reach the server` in the frontend

The Angular app could not reach the backend. Confirm the backend is running and that
`apiUrl` in the active environment file points to the correct host and port.
