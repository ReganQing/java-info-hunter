package com.ron.javainfohunter.api.aspect;

import com.ron.javainfohunter.api.aspect.RateLimit.KeyType;
import com.ron.javainfohunter.api.aspect.RateLimit.TimeUnit;
import com.ron.javainfohunter.api.exception.RateLimitExceededException;
import com.ron.javainfohunter.api.redis.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.function.Function;

/**
 * Aspect for enforcing rate limits on annotated methods
 *
 * Features:
 * - Multiple key types (IP, USER_ID, ENDPOINT, CUSTOM)
 * - SpEL expression support for custom keys
 * - Configurable time windows and limits
 * - Automatic key generation with prefixes and method names
 *
 * Thread Safety: Thread-safe using RedisService
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisService redisService;
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Around advice for @RateLimit annotation
     *
     * @param joinPoint The joint point representing the method execution
     * @param rateLimit The rate limit annotation
     * @return The result of the method execution
     * @throws Throwable if the method execution fails
     */
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Build the rate limit key
        String key = buildRateLimitKey(joinPoint, rateLimit);

        // Convert time window to Duration
        Duration window = convertToDuration(rateLimit.window());

        // Check rate limit
        boolean allowed = redisService.checkRateLimit(key, rateLimit.limit(), window);

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, limit: {}, window: {}",
                key, rateLimit.limit(), window);

            throw new RateLimitExceededException(
                key,
                rateLimit.limit(),
                window.getSeconds()
            );
        }

        log.debug("Rate limit check passed for key: {}", key);

        // Proceed with method execution
        return joinPoint.proceed();
    }

    /**
     * Build the rate limit key based on the annotation configuration
     *
     * @param joinPoint The joint point
     * @param rateLimit The rate limit annotation
     * @return The rate limit key
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder(rateLimit.prefix());

        // Add key type specific part
        String keyValue = getKeyValue(joinPoint, rateLimit.keyType(), rateLimit.keyExpression());
        keyBuilder.append(":").append(keyValue);

        // Add method name if configured
        if (rateLimit.includeMethod()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            keyBuilder.append(":").append(signature.getName());
        }

        return keyBuilder.toString();
    }

    /**
     * Get the key value based on the key type
     *
     * @param joinPoint The joint point
     * @param keyType The key type
     * @param keyExpression The SpEL expression (for CUSTOM key type)
     * @return The key value
     */
    private String getKeyValue(ProceedingJoinPoint joinPoint, KeyType keyType, String keyExpression) {
        return switch (keyType) {
            case IP -> extractClientIp();
            case USER_ID -> extractUserId();
            case ENDPOINT -> extractEndpoint();
            case CUSTOM -> evaluateCustomKey(joinPoint, keyExpression);
        };
    }

    /**
     * Extract client IP address from request
     * Priority: X-Forwarded-For > X-Real-IP > RemoteAddr
     *
     * @return The client IP address
     */
    private String extractClientIp() {
        HttpServletRequest request = getCurrentRequest();

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For may contain multiple IPs, use the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract user ID from request attribute
     *
     * @return The user ID as string
     */
    private String extractUserId() {
        HttpServletRequest request = getCurrentRequest();

        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("User ID not found in request attributes");
        }

        return String.valueOf(userId);
    }

    /**
     * Extract endpoint path from request
     *
     * @return The endpoint path
     */
    private String extractEndpoint() {
        HttpServletRequest request = getCurrentRequest();
        return request.getRequestURI();
    }

    /**
     * Evaluate custom SpEL expression
     *
     * @param joinPoint The joint point
     * @param expression The SpEL expression
     * @return The evaluated expression result
     */
    private String evaluateCustomKey(ProceedingJoinPoint joinPoint, String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("Custom key expression cannot be empty");
        }

        EvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // Set method parameters as variables
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression exp = parser.parseExpression(expression);
        Object value = exp.getValue(context);

        if (value == null) {
            throw new IllegalArgumentException("Custom key expression evaluated to null");
        }

        return String.valueOf(value);
    }

    /**
     * Convert window configuration to Duration
     *
     * @param window The window configuration
     * @return The duration
     */
    private Duration convertToDuration(RateLimit.Window window) {
        return switch (window.unit()) {
            case SECONDS -> Duration.ofSeconds(window.value());
            case MINUTES -> Duration.ofMinutes(window.value());
            case HOURS -> Duration.ofHours(window.value());
            case DAYS -> Duration.ofDays(window.value());
        };
    }

    /**
     * Get the current HTTP request
     *
     * @return The current request
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

        return attributes.getRequest();
    }
}
