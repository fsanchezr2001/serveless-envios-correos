# agente.md — Sistema de envío de email

Instrucciones técnicas y de arquitectura para el desarrollo, empaquetado y despliegue de la aplicación.

---

## 1. Objetivo

Aplicación web integral donde el usuario completa un formulario reactivo y envía un correo electrónico. El flujo atraviesa desde la interfaz en **Angular**, pasa por la API REST en **Spring Boot**, se comunica con **SendGrid** para el despacho del correo y registra la auditoría del mensaje en base de datos.

---maestro2000*

## 2. Stack Tecnológico

| Capa | Tecnología | Detalle |
|---|---|---|
| **Frontend** | Angular 20 | Componentes Standalone, Reactive Forms, `HttpClient` |
| **Backend** | Spring Boot 3.3.5 (Java 17) | Spring MVC, Spring Data JPA, Hibernate, Maven 3.9 |
| **Email Service** | SendGrid API v3 | Librería `sendgrid-java` (v4.10.2) |
| **Base de Datos** | H2 Database / MySQL | H2 en memoria para desarrollo/Render free; compatible con MySQL |
| **Empaquetado (backend)** | Docker | Build multi-stage (Maven 3.9 + JRE 17) solo del backend, en `Dockerfile` raíz |
| **Empaquetado (frontend)** | Angular CLI | `npm run build` -> `dist/frontend/browser`, servido como estáticos |
| **Hosting Frontend** | Cloudflare Pages | Proyecto `envio-correo` -> `https://envio-correo.pages.dev` |
| **Hosting Backend** | Render | Web Service (runtime Docker) desde el `Dockerfile` raíz |

---

## 3. Arquitectura del Sistema

```
[ Navegador del Usuario ]
           │
           │ (GET / -> index.html, static assets)
           ▼
┌─────────────────────────────────────────────────────────────┐
│ Cloudflare Pages (SPA Angular 20 - estáticos, sin backend)   │
│  https://envio-correo.pages.dev                              │
│  - apiUrl: https://envio-correo.onrender.com/api            │
└──────────┬──────────────────────────────────────────────────┘
           │ (POST https://envio-correo.onrender.com/api/emails)
           │  origen cruzado -> requiere CORS_ALLOWED_ORIGINS
           ▼
┌─────────────────────────────────────────────────────────────┐
│ Contenedor Docker (Render - Puerto 8080)                    │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Spring Boot (API REST)                                │  │
│  │  - EmailController                                    │  │
│  │  - PersistingEmailService ──► H2 DB (email_logs)      │  │
│  │  - SendGridEmailService                               │  │
│  └──────────┬────────────────────────────────────────────┘  │
└─────────────┼───────────────────────────────────────────────┘
              │ (HTTPS POST https://api.sendgrid.com/v3/mail/send)
              ▼
    [ SendGrid Mail Service ] ────► [ Destinatario Final ]
```

### Principios Clave de la Arquitectura:
1. **Artefactos separados**: El frontend y el backend se construyen y despliegan de forma independiente. El `Dockerfile` raíz genera **solo** el backend; Angular **no** se incrusta en el JAR.
2. **CORS obligatorio en Producción**: El SPA vive en Cloudflare Pages y la API en Render, por lo que son **orígenes distintos**. El frontend usa `apiUrl` absoluta y el backend debe permitir el origen del SPA mediante `CORS_ALLOWED_ORIGINS` (variable leída por `WebConfig.java`), separados por comas para múltiples orígenes.
3. **CORS exclusivo para Desarrollo**: Para desarrollo local (`ng serve` en puerto 4200 y Spring Boot en puerto 8080), el valor por defecto de `CORS_ALLOWED_ORIGINS` es `http://localhost:4200`.
4. **Ruteo del SPA en Pages**: `frontend/public/_redirects` reescribe cualquier ruta a `index.html` (200) y `frontend/public/_headers` define la política de caché (`index.html` sin caché, bundles con hash inmutables).
5. **Endpoints de diagnóstico**: `GET /api/status` y `GET /api/emails/health` (este último es el health check de Render).

---

## 4. Estructura del Repositorio

```
ServelessEnvioCorreo/
├── .vscode/
│   └── launch.json                # Variables de entorno locales para VS Code (en .gitignore)
├── frontend/                      # Aplicación Angular 20
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/email-form/  # Formulario reactivo y manejo de estados UI
│   │   │   ├── services/email.service.ts # Cliente HTTP
│   │   │   └── models/                 # Modelos Request/Response
│   │   └── environments/
│   │       ├── environment.ts          # apiUrl absoluta de Render (producción)
│   │       └── environment.development.ts # apiUrl: 'http://localhost:8080/api'
│   ├── public/
│   │   ├── _redirects                  # SPA fallback: /* -> /index.html 200
│   │   └── _headers                    # Política de caché para Pages
│   └── angular.json               # outputPath: "dist/frontend"
├── backend/                       # API Spring Boot
│   ├── src/main/java/com/serveless/envio/
│   │   ├── config/
│   │   │   ├── SendGridConfig.java
│   │   │   └── WebConfig.java     # CORS desde CORS_ALLOWED_ORIGINS
│   │   ├── controller/
│   │   │   ├── EmailController.java # POST /api/emails, GET /api/emails/health
│   │   │   └── RootController.java  # GET /api/status
│   │   ├── service/
│   │   │   ├── PersistingEmailService.java # Decorador de persistencia
│   │   │   └── SendGridEmailService.java   # Cliente SendGrid
│   │   ├── repository/EmailLogRepository.java
│   │   ├── model/EmailLog.java
│   │   └── exception/GlobalExceptionHandler.java # Mapeo de errores HTTP 400, 502, 500
│   ├── src/main/resources/
│   │   └── application.yml        # Configuración server, H2 y SendGrid
│   └── pom.xml
├── Dockerfile                     # Build multi-stage del backend (sin Angular)
├── render.yaml                    # Especificación de infraestructura para Render
└── agente.md                      # Documentación del sistema
```

---

## 5. Contrato de la API

### **POST `/api/emails`**
Envía un correo electrónico a través de SendGrid.

**Request Body:**
```json
{
  "to": "destinatario@example.com",
  "toName": "Nombre Destinatario",
  "subject": "Asunto del Correo",
  "message": "<p>Contenido en HTML o texto plano</p>"
}
```

**Respuestas:**
* **`200 OK`**: Correo aceptado y encolado por SendGrid.
  ```json
  { "success": true, "message": "Email sent successfully" }
  ```
* **`400 Bad Request`**: Error de validación (campos requeridos, formato de email, longitud máxima).
* **`502 Bad Gateway`**: SendGrid rechazó la solicitud (credenciales inválidas, remitente no verificado, problemas de conectividad con la API externa).
* **`500 Internal Server Error`**: Falla inesperada o error al persistir en la base de datos.

### **GET `/api/emails/health`**
Endpoint de sondeo de salud (*health check*) utilizado por Render.
* Respuesta: `{"status": "ok"}` (`200 OK`).

### **GET `/api/status`**
Endpoint de metadatos del servicio.
* Respuesta: `{"status": "ok", "service": "envio-correo"}` (`200 OK`).

---

## 6. Variables de Entorno

| Variable | Requerido | Descripción | Ejemplo |
|---|---|---|---|
| `SENDGRID_API_KEY` | **Sí** | API Key con permiso **Mail Send** generada en SendGrid. | `SG.v47JKm...PbL2c` |
| `SENDGRID_FROM` | **Sí** | Remitente verificado en SendGrid (*Single Sender* o dominio). | `fsanchezr2001@gmail.com` |
| `SENDGRID_FROM_NAME` | No | Nombre visible que acompaña al remitente. | `Serveless Envio Correo` |
| `PORT` | No | Puerto HTTP del servidor (inyectado por Render automáticamente). | `8080` |
| `DB_URL` | No | URL de conexión JDBC. Por defecto H2 en memoria. | `jdbc:h2:mem:emaildb;DB_CLOSE_DELAY=-1` |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes permitidos por CORS, separados por comas. **En producción debe incluir el dominio de Cloudflare Pages.** | `https://envio-correo.pages.dev` |

> ⚠️ **Gotchas Críticos y Lecciones de SendGrid:**
> 1. **Texto pegado accidentalmente (`Copied!`)**: Al copiar la clave desde el portal web de SendGrid, el botón de la interfaz puede incluir el texto del tooltip en el portapapeles (`...PbL2cCopied!`). Esto invalida el hash de la clave y causa error `401 Unauthorized` de SendGrid, que el backend traduce en un `502 Bad Gateway`.
> 2. **Remitente No Verificado**: SendGrid exige verificar el correo del remitente en *Settings → Sender Authentication → Single Sender Verification*. Si el remitente en `SENDGRID_FROM` no coincide exactamente con el remitente validado, SendGrid devuelve error 400/403.

---

## 7. Construcción y Empaquetado Docker (solo backend)

El `Dockerfile` raíz construye **únicamente el backend**. Angular ya no se incrusta en el JAR: se despliega aparte, en Cloudflare Pages. Esto evita servir un bundle obsoleto con la `apiUrl` equivocada y elimina el `npm ci` / `ng build` del ciclo de build del backend.

```dockerfile
# 1) Build del backend Spring Boot
FROM maven:3.9-eclipse-temurin-17 AS back
WORKDIR /b
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q package -DskipTests

# 2) Imagen final de ejecución: JRE ligero
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=back /b/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Comandos de Construcción y Ejecución Local:
```bash
# Construir la imagen del backend (desde la raíz del repositorio)
docker build -t envio-correo-api .

# Ejecutar localmente con variables
docker run -p 8080:8080 \
  -e SENDGRID_API_KEY="SG.tu_clave_real" \
  -e SENDGRID_FROM="fsanchezr2001@gmail.com" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:4200" \
  envio-correo-api
```

### Build del frontend:
```bash
cd frontend
npm ci && npm run build     # salida en frontend/dist/frontend/browser
```

---

## 8. Despliegue

La aplicación son **dos servicios independientes**.

### 8.1 Frontend → Cloudflare Pages

1. Cloudflare Dashboard → **Workers & Pages** → **Create** → **Pages** → **Connect to Git**.
2. Configurar el build:
   * **Root directory**: `frontend`
   * **Build command**: `npm ci && npm run build`
   * **Build output directory**: `dist/frontend/browser`
3. Desplegar. URL resultante: `https://envio-correo.pages.dev`.
4. `frontend/public/_redirects` (`/* -> /index.html 200`) resuelve el ruteo del SPA y
   `frontend/public/_headers` define la caché (`index.html` sin caché, bundles inmutables).

### 8.2 Backend → Render

1. Render Dashboard → **New → Blueprint** y seleccionar el repositorio. Render lee
   `render.yaml` y crea el Web Service `envio-correo` con `runtime: docker`,
   `dockerfilePath: ./Dockerfile` y `healthCheckPath: /api/emails/health`.
2. Completar los secretos marcados con `sync: false` en la pestaña **Environment**:
   * `SENDGRID_API_KEY`: clave limpia, sin sufijos extra (`Copied!`).
   * `SENDGRID_FROM`: `fsanchezr2001@gmail.com` (remitente verificado).
   * `SENDGRID_FROM_NAME`: `Serveless Envio Correo`.
   * `DB_URL`: `jdbc:h2:mem:emaildb;DB_CLOSE_DELAY=-1`.
   * `CORS_ALLOWED_ORIGINS`: el dominio de Pages, `https://envio-correo.pages.dev`.
3. Guardar: Render reinicia el contenedor con las variables activas.
4. Copiar la URL del servicio (`https://envio-correo.onrender.com`) a
   `frontend/src/environments/environment.ts` (`apiUrl`) y re-desplegar Pages.

> ⚠️ **Plan free de Render**: el servicio se duerme tras un periodo de inactividad. La
> primera petición tras el despertar tarda decenas de segundos (se midió ~25 s de
> arranque de Spring Boot) y la base H2 en memoria se reinicia.

### 8.3 Orden de despliegue recomendado

1. Backend en Render primero (necesitás su URL para `apiUrl`).
2. Actualizar `environment.ts` con esa URL.
3. Frontend en Pages con el origen ya conocido.
4. Setear `CORS_ALLOWED_ORIGINS` en Render con la URL de Pages.

---

## 9. Checklist de Implementación y Estado

- [x] Estructura desacoplada `frontend/` y `backend/`
- [x] CORS configurable por entorno (`CORS_ALLOWED_ORIGINS` leído por `WebConfig.java`, verificado con preflight)
- [x] Formulario reactivo Angular con validaciones y feedback de estados
- [x] `apiUrl` absoluta de producción en `environment.ts` (origen cruzado a Render)
- [x] Decorador de persistencia en BD (`PersistingEmailService` con H2)
- [x] Integración con SendGrid API v3 (`sendgrid-java`)
- [x] Verificación de Single Sender en SendGrid (`fsanchezr2001@gmail.com`)
- [x] Endpoints de diagnóstico `/api/status` y `/api/emails/health`
- [x] `Dockerfile` multi-stage **solo backend** (Angular fuera del JAR), build verificado
- [x] `frontend/public/_redirects` y `_headers` para el ruteo y caché del SPA
- [x] Blueprint `render.yaml` con `dockerfilePath`, health check y `CORS_ALLOWED_ORIGINS`
- [ ] Conectar el repositorio a Cloudflare Pages (proyecto `envio-correo`)
- [ ] Crear el Web Service en Render y completar los secretos
- [ ] Fijar `CORS_ALLOWED_ORIGINS` con la URL final de Pages
