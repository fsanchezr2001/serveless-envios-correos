package com.serveless.envio.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 *
 * In production the Angular bundle is served by Cloudflare Pages, so the browser
 * origin differs from the API host and CORS is required. Allowed origins are read
 * from the CORS_ALLOWED_ORIGINS environment variable (comma-separated) and default
 * to the local dev server.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
