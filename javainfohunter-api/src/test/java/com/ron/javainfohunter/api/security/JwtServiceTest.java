package com.ron.javainfohunter.api.security;

import com.ron.javainfohunter.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtServiceTest {

    private JwtService createTestJwtService() {
        return new JwtService(
                "test-secret-key-must-be-at-least-32-characters-long-for-hmac",
                900000,
                604800000,
                mock(RefreshTokenRepository.class)
        );
    }

    private String invokeHashToken(JwtService jwtService, String rawToken) throws Exception {
        Method method = JwtService.class.getDeclaredMethod("hashToken", String.class);
        method.setAccessible(true);
        return (String) method.invoke(jwtService, rawToken);
    }

    @Test
    void hashToken_shouldUseSecureHash() throws Exception {
        JwtService jwtService = createTestJwtService();

        String token1 = "token-a";
        String token2 = "token-b";

        String hash1 = invokeHashToken(jwtService, token1);
        String hash2 = invokeHashToken(jwtService, token2);

        assertNotEquals(hash1, hash2);
        assertEquals(64, hash1.length());

        assertEquals(hash1, invokeHashToken(jwtService, token1));
    }

    @Test
    void hashToken_shouldNotCollide() throws NoSuchAlgorithmException {
        Set<String> hashes = new HashSet<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < 10_000; i++) {
            String token = UUID.randomUUID().toString();
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);
            assertTrue(hashes.add(hex), "Hash collision detected for token: " + token);
        }
    }
}
