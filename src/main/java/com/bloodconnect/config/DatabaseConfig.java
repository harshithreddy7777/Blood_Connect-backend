package com.bloodconnect.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final Pattern PG_URL_PATTERN = Pattern.compile(
            "^(?:postgres|postgresql)://(?:([^:@]+)(?::([^@]+))?@)?([^:/]+)(?::(\\d+))?(/[^?]+)(?:\\?(.*))?$"
    );

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

        if (envDbUrl != null && (envDbUrl.startsWith("postgres://") || envDbUrl.startsWith("postgresql://"))) {
            Matcher matcher = PG_URL_PATTERN.matcher(envDbUrl.trim());
            if (matcher.matches()) {
                String user = matcher.group(1);
                String pass = matcher.group(2);
                String host = matcher.group(3);
                String port = matcher.group(4) != null ? matcher.group(4) : "5432";
                String path = matcher.group(5);
                String query = matcher.group(6);

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                } else {
                    jdbcUrl += "?sslmode=prefer";
                }

                log.info("Configured PostgreSQL JDBC URL: {}", jdbcUrl);
                config.setJdbcUrl(jdbcUrl);
                if (user != null) config.setUsername(user);
                if (pass != null) config.setPassword(pass);
            } else {
                String stripped = envDbUrl.replaceFirst("^(postgres|postgresql)://", "");
                String jdbcUrl = "jdbc:postgresql://" + stripped;
                if (!jdbcUrl.contains("sslmode")) {
                    jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=prefer";
                }
                log.info("Configured fallback PostgreSQL JDBC URL: {}", jdbcUrl);
                config.setJdbcUrl(jdbcUrl);
                if (datasourceUsername != null && !datasourceUsername.isBlank()) {
                    config.setUsername(datasourceUsername);
                }
                if (datasourcePassword != null && !datasourcePassword.isBlank()) {
                    config.setPassword(datasourcePassword);
                }
            }
            config.setDriverClassName("org.postgresql.Driver");
        } else {
            String jdbcUrl = envDbUrl != null && !envDbUrl.isBlank() ? envDbUrl : datasourceUrl;
            config.setJdbcUrl(jdbcUrl);
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
