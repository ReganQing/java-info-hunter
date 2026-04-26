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
        boolean runInTest = environment.getProperty(
            "javainfohunter.startup.validation.run-in-test",
            Boolean.class,
            false
        );

        if (isTestProfile() && !runInTest) {
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

        validateNumericRanges();

        log.info(
            "Startup required-property validation passed ({} properties)",
            requiredProperties.size()
        );
    }

    private void validateNumericRanges() {
        String[] configuredRanges = environment.getProperty(
            "javainfohunter.startup.validation.numeric-ranges",
            String[].class,
            new String[0]
        );
        if (configuredRanges.length == 0) {
            String rawRanges = environment.getProperty("javainfohunter.startup.validation.numeric-ranges");
            if (StringUtils.hasText(rawRanges)) {
                configuredRanges = Arrays.stream(rawRanges.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toArray(String[]::new);
            }
        }
        List<String> numericRanges = Arrays.asList(configuredRanges);

        if (numericRanges.isEmpty()) {
            return;
        }

        for (String ruleText : numericRanges) {
            if (!StringUtils.hasText(ruleText)) {
                continue;
            }

            String[] parts = ruleText.split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalStateException(
                    "Startup numeric-range validation rule format is invalid: " + ruleText
                );
            }

            String key = parts[0].trim();
            if (!StringUtils.hasText(key)) {
                throw new IllegalStateException(
                    "Startup numeric-range validation rule has blank key: " + ruleText
                );
            }

            Double min = parseBound(parts[1], key, "min");
            Double max = parseBound(parts[2], key, "max");

            String value = environment.getProperty(key);
            if (!StringUtils.hasText(value)) {
                continue;
            }

            double numericValue;
            try {
                numericValue = Double.parseDouble(value.trim());
            } catch (NumberFormatException exception) {
                throw new IllegalStateException(
                    "Startup numeric-range validation failed: property '"
                        + key
                        + "' value '"
                        + value
                        + "' is not numeric"
                );
            }

            if (min != null && numericValue < min) {
                throw new IllegalStateException(
                    "Startup numeric-range validation failed: property '"
                        + key
                        + "' value "
                        + numericValue
                        + " is below min "
                        + min
                );
            }

            if (max != null && numericValue > max) {
                throw new IllegalStateException(
                    "Startup numeric-range validation failed: property '"
                        + key
                        + "' value "
                        + numericValue
                        + " exceeds max "
                        + max
                );
            }
        }

        log.info(
            "Startup numeric-range validation passed ({} rules)",
            numericRanges.size()
        );
    }

    private Double parseBound(String rawBound, String key, String boundName) {
        String bound = rawBound == null ? "" : rawBound.trim();
        if (!StringUtils.hasText(bound)) {
            return null;
        }

        try {
            return Double.parseDouble(bound);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                "Startup numeric-range validation failed: property '"
                    + key
                    + "' has non-numeric "
                    + boundName
                    + " bound '"
                    + bound
                    + "'"
            );
        }
    }

    private boolean isTestProfile() {
        return Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "test".equalsIgnoreCase(profile));
    }
}
