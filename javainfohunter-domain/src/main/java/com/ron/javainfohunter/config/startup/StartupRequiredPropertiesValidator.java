package com.ron.javainfohunter.config.startup;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Validates required configuration properties at application startup.
 *
 * <p>Each service can declare its required keys under:
 * {@code javainfohunter.startup.validation.required-properties}.
 * If a required property is missing or blank, startup fails fast with
 * a clear error message.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRequiredPropertiesValidator {

    private final Environment environment;

    @PostConstruct
    void validate() {
        if (isTestProfile()) {
            log.debug("Skipping startup required-property validation in test profile");
            return;
        }

        boolean enabled = environment.getProperty(
            "javainfohunter.startup.validation.enabled",
            Boolean.class,
            true
        );
        if (!enabled) {
            log.info("Startup required-property validation disabled");
            return;
        }

        List<String> requiredProperties = Binder.get(environment)
            .bind(
                "javainfohunter.startup.validation.required-properties",
                Bindable.listOf(String.class)
            )
            .orElse(List.of());

        if (requiredProperties.isEmpty()) {
            log.debug("No required startup properties configured");
            return;
        }

        List<String> missing = requiredProperties.stream()
            .filter(key -> !StringUtils.hasText(environment.getProperty(key)))
            .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Missing required startup properties: " + String.join(", ", missing)
            );
        }

        log.info(
            "Startup required-property validation passed ({} properties)",
            requiredProperties.size()
        );
    }

    private boolean isTestProfile() {
        return Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "test".equalsIgnoreCase(profile));
    }
}
