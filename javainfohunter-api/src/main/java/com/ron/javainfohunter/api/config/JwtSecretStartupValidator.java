package com.ron.javainfohunter.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretStartupValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtSecretStartupValidator.class);
    private static final String JWT_PLACEHOLDER_SECRET =
            "jih_dev_placeholder_jwt_secret_min_32_chars_only_for_explicit_lifecycle_toggle";

    private final String jwtSecret;
    private final boolean allowPlaceholderSecret;

    public JwtSecretStartupValidator(
            @Value("${javainfohunter.security.jwt.secret}") String jwtSecret,
            @Value("${javainfohunter.security.jwt.allow-placeholder-secret:false}") boolean allowPlaceholderSecret) {
        this.jwtSecret = jwtSecret;
        this.allowPlaceholderSecret = allowPlaceholderSecret;
    }

    @PostConstruct
    void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is empty. Configure JWT_SECRET.");
        }

        if (JWT_PLACEHOLDER_SECRET.equals(jwtSecret) && !allowPlaceholderSecret) {
            throw new IllegalStateException(
                    "JWT secret uses placeholder value. Set JWT_SECRET or explicitly set "
                            + "JIH_ALLOW_PLACEHOLDER_SECRETS=true for local lifecycle checks.");
        }

        if (JWT_PLACEHOLDER_SECRET.equals(jwtSecret)) {
            logger.warn("Using placeholder JWT secret because JIH_ALLOW_PLACEHOLDER_SECRETS=true");
        }
    }
}
