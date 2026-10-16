package com.serveless.envio.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceUrlConverter {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(@Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}") String url) {
        if (url == null || url.isEmpty()) {
            return new HikariDataSource();
        }
        String jdbcUrl = url;
        if (jdbcUrl.startsWith("mysql://")) {
            jdbcUrl = "jdbc:mysql://" + jdbcUrl.substring("mysql://".length())
                    + "?allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC";
        } else if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            // Try to be lenient if it's already jdbc
        }
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        return ds;
    }
}