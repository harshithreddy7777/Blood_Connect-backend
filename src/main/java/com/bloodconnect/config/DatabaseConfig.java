package com.bloodconnect.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("DB_URL");
        }
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = datasourceUrl;
        }

        HikariConfig config = new HikariConfig();

        // Support Render / Supabase / Neon standard raw URI formats: postgres:// or postgresql://
        if (envDbUrl != null && (envDbUrl.startsWith("postgres://") || envDbUrl.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(envDbUrl);
                String userInfo = uri.getUserInfo();
                String username = datasourceUsername;
                String password = datasourcePassword;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                } else if (!jdbcUrl.contains("sslmode")) {
                    jdbcUrl += "?sslmode=prefer";
                }

                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
            } catch (Exception e) {
                config.setJdbcUrl(envDbUrl);
                config.setUsername(datasourceUsername);
                config.setPassword(datasourcePassword);
            }
        } else {
            // Standard JDBC URL (e.g. jdbc:postgresql://... or jdbc:mysql://...)
            config.setJdbcUrl(envDbUrl != null && !envDbUrl.isBlank() ? envDbUrl : datasourceUrl);
            config.setUsername(datasourceUsername);
            config.setPassword(datasourcePassword);
            if (config.getJdbcUrl() != null && config.getJdbcUrl().contains("mysql")) {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else {
                config.setDriverClassName("org.postgresql.Driver");
            }
        }

        return new HikariDataSource(config);
    }
}
