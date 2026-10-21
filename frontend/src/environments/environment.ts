// Production environment.
//
// The Angular bundle is served by Cloudflare Pages, while the Spring Boot API runs
// on Render. They are different origins, so apiUrl must be the absolute backend URL
// and that origin must be present in the backend's CORS_ALLOWED_ORIGINS.
export const environment = {
  production: true,
  apiUrl: 'https://envio-correo.onrender.com/api',
};
