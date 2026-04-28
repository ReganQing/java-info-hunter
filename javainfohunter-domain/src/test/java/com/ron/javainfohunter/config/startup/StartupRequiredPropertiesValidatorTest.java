package com.ron.javainfohunter.config.startup;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartupRequiredPropertiesValidatorTest {

    @Test
    void validate_ShouldPass_WhenCompareRuleSatisfied() {
        MockEnvironment environment = baseEnvironment()
            .withProperty("a.min", "3")
            .withProperty("a.max", "10")
            .withProperty("javainfohunter.startup.validation.compare-rules", "a.min|<=|a.max");

        StartupRequiredPropertiesValidator validator = new StartupRequiredPropertiesValidator(environment);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_ShouldFail_WhenCompareRuleViolated() {
        MockEnvironment environment = baseEnvironment()
            .withProperty("a.min", "12")
            .withProperty("a.max", "10")
            .withProperty("javainfohunter.startup.validation.compare-rules", "a.min|<=|a.max");

        StartupRequiredPropertiesValidator validator = new StartupRequiredPropertiesValidator(environment);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_ShouldFail_WhenCompareRuleUsesNonNumericValue() {
        MockEnvironment environment = baseEnvironment()
            .withProperty("a.min", "x")
            .withProperty("a.max", "10")
            .withProperty("javainfohunter.startup.validation.compare-rules", "a.min|<=|a.max");

        StartupRequiredPropertiesValidator validator = new StartupRequiredPropertiesValidator(environment);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_ShouldFail_WhenNumericRangeViolated() {
        MockEnvironment environment = baseEnvironment()
            .withProperty("a.pool", "100")
            .withProperty("javainfohunter.startup.validation.numeric-ranges", "a.pool|1|64");

        StartupRequiredPropertiesValidator validator = new StartupRequiredPropertiesValidator(environment);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    private MockEnvironment baseEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        environment.setProperty("javainfohunter.startup.validation.enabled", "true");
        environment.setProperty("javainfohunter.startup.validation.run-in-test", "true");
        environment.setProperty("bootstrap.flag", "ok");
        environment.setProperty("javainfohunter.startup.validation.required-properties", "bootstrap.flag");
        return environment;
    }
}
