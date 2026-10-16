package com.serveless.envio.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Normalizes managed-database connection URLs into JDBC form.
 *
 * <p>Cloud MySQL providers (Railway, Heroku, Clever Cloud) expose the connection
 * as {@code mysql://user:pass@host:port/db}, but the JDBC driver requires
 * {@code jdbc:mysql://...}. This runs before auto-configuration so that HikariCP,
 * Flyway and Hibernate all resolve the same valid URL from a single source.</p>
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "normalizedDatabaseUrl";
    private static final String MYSQL_SCHEME = "mysql://";
    private static final String JDBC_MYSQL_SCHEME = "jdbc:mysql://";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_PARAMS =
            "allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"));

        if (url == null || !url.startsWith(MYSQL_SCHEME)) {
            return;
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.datasource.url", toJdbcUrl(url));
        overrides.put("spring.datasource.driver-class-name", MYSQL_DRIVER);
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, overrides));
    }

    private static String toJdbcUrl(String url) {
        String jdbcUrl = JDBC_MYSQL_SCHEME + url.substring(MYSQL_SCHEME.length());
        char separator = jdbcUrl.indexOf('?') >= 0 ? '&' : '?';
        return jdbcUrl + separator + MYSQL_PARAMS;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
