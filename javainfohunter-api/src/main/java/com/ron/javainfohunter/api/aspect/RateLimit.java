package com.ron.javainfohunter.api.aspect;

import com.ron.javainfohunter.api.aspect.RateLimit.KeyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for rate limiting method execution
 *
 * Supports multiple key types:
 * - IP: Rate limit by client IP address
 * - USER_ID: Rate limit by user ID (requires authentication)
 * - ENDPOINT: Rate limit by endpoint path
 * - CUSTOM: Custom key expression using SpEL
 *
 * Examples:
 * <pre>
 * {@code
 * @RateLimit(keyType = KeyType.IP, limit = 100, window = @RateLimit.Window(value = 1, unit = TimeUnit.MINUTES))
 * public ResponseEntity<?> myEndpoint() { ... }
 *
 * @RateLimit(keyType = KeyType.USER_ID, limit = 10, window = @RateLimit.Window(value = 1, unit = TimeUnit.HOURS))
 * public ResponseEntity<?> premiumFeature() { ... }
 *
 * @RateLimit(keyType = KeyType.CUSTOM, keyExpression "'api:' + #userId", limit = 50, window = ...)
 * public ResponseEntity<?> customKey(@PathVariable String userId) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Key type for rate limiting
     */
    KeyType keyType() default KeyType.IP;

    /**
     * Custom key expression using SpEL (only used when keyType = CUSTOM)
     */
    String keyExpression() default "";

    /**
     * Maximum number of requests allowed within the time window
     */
    int limit();

    /**
     * Time window configuration
     */
    Window window();

    /**
     * Prefix for the rate limit key in Redis (optional)
     */
    String prefix() default "rate_limit";

    /**
     * Whether to include method name in the key
     */
    boolean includeMethod() default true;

    /**
     * Time window configuration
     */
    @interface Window {
        /**
         * Time value
         */
        long value();

        /**
         * Time unit
         */
        com.ron.javainfohunter.api.aspect.RateLimit.TimeUnit unit() default com.ron.javainfohunter.api.aspect.RateLimit.TimeUnit.SECONDS;
    }

    /**
     * Key type enumeration
     */
    enum KeyType {
        /**
         * Rate limit by client IP address
         * Extracts IP from X-Forwarded-For or X-Real-IP headers
         */
        IP,

        /**
         * Rate limit by user ID
         * Requires authenticated user with ID attribute
         */
        USER_ID,

        /**
         * Rate limit by endpoint path
         * Uses the request URI path
         */
        ENDPOINT,

        /**
         * Custom key using SpEL expression
         * Allows complex key generation logic
         */
        CUSTOM
    }

    /**
     * Time unit enumeration
     */
    enum TimeUnit {
        SECONDS, MINUTES, HOURS, DAYS
    }
}
