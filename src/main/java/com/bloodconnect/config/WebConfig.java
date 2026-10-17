package com.bloodconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Comma-separated list of allowed CORS origins.
     * Defaults to localhost dev origins; override with CORS_ALLOWED_ORIGINS env var in production.
     * Example: https://blood-connect-frontend.onrender.com
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,https://blood-connect-frontend-eta.vercel.app}")
    private String allowedOriginsRaw;

    private List<String> allowedOriginPatterns() {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (allowedOriginsRaw != null && !allowedOriginsRaw.isBlank()) {
            for (String origin : allowedOriginsRaw.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                    list.add(trimmed);
                }
            }
        }
        if (!list.contains("https://*.vercel.app")) {
            list.add("https://*.vercel.app");
        }
        if (!list.contains("https://*.onrender.com")) {
            list.add("https://*.onrender.com");
        }
        if (!list.contains("http://localhost:[*]")) {
            list.add("http://localhost:[*]");
        }
        if (!list.contains("http://127.0.0.1:[*]")) {
            list.add("http://127.0.0.1:[*]");
        }
        return list;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

