package com.ron.javainfohunter.api.aspect;

import com.ron.javainfohunter.api.aspect.RateLimit.KeyType;
import com.ron.javainfohunter.api.aspect.RateLimit.TimeUnit;
import com.ron.javainfohunter.api.exception.RateLimitExceededException;
import com.ron.javainfohunter.api.redis.RedisService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitAspect
 *
 * Test Coverage:
 * - IP-based rate limiting
 * - User ID-based rate limiting
 * - Endpoint-based rate limiting
 * - Custom key-based rate limiting
 * - Exception handling when limit exceeded
 * - Proper key generation with prefixes and method names
 */
@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RedisService redisService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Signature signature;

    @Mock
    private MockHttpServletRequest request;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        mockRequest = new MockHttpServletRequest();

        // Setup request attributes
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        // Setup join point mocks
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(
            getClass().getDeclaredMethod("dummyMethod")
        );
        lenient().when(methodSignature.getName()).thenReturn("dummyMethod");
        lenient().when(joinPoint.getTarget()).thenReturn(this);
    }

    // Dummy method for testing
    void dummyMethod() {
    }

    // ==================== IP-based Rate Limiting Tests ====================

    @Test
    void rateLimit_ByIp_ShouldAllow_WhenUnderLimit() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.1");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", true
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(true);

        // When
        Object result = rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        assertThat(result).isNull(); // proceed() returns null in our mock
        verify(redisService).checkRateLimit(
            contains("203.0.113.1"),
            eq(100),
            eq(Duration.ofMinutes(1))
        );
    }

    @Test
    void rateLimit_ByIp_ShouldUseXRealIp_WhenXForwardedForNotPresent() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("X-Real-IP", "198.51.100.1");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", true
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            contains("198.51.100.1"),
            eq(100),
            any(Duration.class)
        );
    }

    @Test
    void rateLimit_ByIp_ShouldUseRemoteAddr_WhenNoHeaders() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", true
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            contains("192.168.1.100"),
            eq(100),
            any(Duration.class)
        );
    }

    @Test
    void rateLimit_ByIp_ShouldThrowException_WhenOverLimit() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", true
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> rateLimitAspect.rateLimit(joinPoint, rateLimit))
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessageContaining("Rate limit exceeded")
            .satisfies(ex -> {
                RateLimitExceededException rle = (RateLimitExceededException) ex;
                assertThat(rle.getKey()).contains("192.168.1.100");
                assertThat(rle.getLimit()).isEqualTo(100);
                assertThat(rle.getWindowSeconds()).isEqualTo(60);
            });
    }

    // ==================== User ID-based Rate Limiting Tests ====================

    @Test
    void rateLimit_ByUserId_ShouldAllow_WhenUnderLimit() throws Throwable {
        // Given
        mockRequest.setAttribute("userId", 12345L);

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.USER_ID, 10, 1, TimeUnit.HOURS, "premium", true
        );

        when(redisService.checkRateLimit(anyString(), eq(10), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            contains("12345"),
            eq(10),
            eq(Duration.ofHours(1))
        );
    }

    @Test
    void rateLimit_ByUserId_ShouldThrowException_WhenOverLimit() throws Throwable {
        // Given
        mockRequest.setAttribute("userId", 12345L);

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.USER_ID, 10, 1, TimeUnit.HOURS, "premium", true
        );

        when(redisService.checkRateLimit(anyString(), eq(10), any(Duration.class)))
            .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> rateLimitAspect.rateLimit(joinPoint, rateLimit))
            .isInstanceOf(RateLimitExceededException.class)
            .satisfies(ex -> {
                RateLimitExceededException rle = (RateLimitExceededException) ex;
                assertThat(rle.getKey()).contains("12345");
                assertThat(rle.getLimit()).isEqualTo(10);
            });
    }

    // ==================== Endpoint-based Rate Limiting Tests ====================

    @Test
    void rateLimit_ByEndpoint_ShouldAllow_WhenUnderLimit() throws Throwable {
        // Given
        mockRequest.setRequestURI("/api/news");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.ENDPOINT, 1000, 1, TimeUnit.SECONDS, "api", false
        );

        when(redisService.checkRateLimit(anyString(), eq(1000), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            contains("/api/news"),
            eq(1000),
            eq(Duration.ofSeconds(1))
        );
    }

    // ==================== Custom Key Rate Limiting Tests ====================

    @Test
    void rateLimit_ByCustomKey_ShouldUseSpelExpression() throws Throwable {
        // Given
        RateLimit rateLimit = createRateLimitAnnotationWithExpression(
            KeyType.CUSTOM, 50, 1, TimeUnit.MINUTES, "custom", true, "'test-key'"
        );

        when(redisService.checkRateLimit(anyString(), eq(50), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            contains("test-key"),
            eq(50),
            eq(Duration.ofMinutes(1))
        );
    }

    // ==================== Edge Cases Tests ====================

    @Test
    void rateLimit_ShouldIncludeMethodName_WhenIncludeMethodTrue() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", true
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            argThat(key -> key.contains("dummyMethod") && key.contains("192.168.1.100")),
            eq(100),
            any(Duration.class)
        );
    }

    @Test
    void rateLimit_ShouldNotIncludeMethodName_WhenIncludeMethodFalse() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", false
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            argThat(key -> !key.contains("dummyMethod")),
            eq(100),
            any(Duration.class)
        );
    }

    @Test
    void rateLimit_ShouldHandleMultipleXForwardedForIps() throws Throwable {
        // Given
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.1");

        RateLimit rateLimit = createRateLimitAnnotation(
            KeyType.IP, 100, 1, TimeUnit.MINUTES, "api", true
        );

        when(redisService.checkRateLimit(anyString(), eq(100), any(Duration.class)))
            .thenReturn(true);

        // When
        rateLimitAspect.rateLimit(joinPoint, rateLimit);

        // Then
        verify(redisService).checkRateLimit(
            contains("203.0.113.1"), // Should use first IP
            eq(100),
            any(Duration.class)
        );
    }

    // ==================== Helper Methods ====================

    private RateLimit createRateLimitAnnotation(
        KeyType keyType,
        int limit,
        long windowValue,
        TimeUnit windowUnit,
        String prefix,
        boolean includeMethod
    ) {
        return createRateLimitAnnotationWithExpression(
            keyType, limit, windowValue, windowUnit, prefix, includeMethod, ""
        );
    }

    private RateLimit createRateLimitAnnotationWithExpression(
        KeyType keyType,
        int limit,
        long windowValue,
        TimeUnit windowUnit,
        String prefix,
        boolean includeMethod,
        String keyExpression
    ) {
        return new RateLimit() {
            @Override
            public KeyType keyType() {
                return keyType;
            }

            @Override
            public String keyExpression() {
                return keyExpression;
            }

            @Override
            public int limit() {
                return limit;
            }

            @Override
            public Window window() {
                return new Window() {
                    @Override
                    public long value() {
                        return windowValue;
                    }

                    @Override
                    public TimeUnit unit() {
                        return windowUnit;
                    }

                    @Override
                    public Class<Window> annotationType() {
                        return Window.class;
                    }
                };
            }

            @Override
            public String prefix() {
                return prefix;
            }

            @Override
            public boolean includeMethod() {
                return includeMethod;
            }

            @Override
            public Class<RateLimit> annotationType() {
                return RateLimit.class;
            }
        };
    }
}
