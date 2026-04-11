package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.request.LoginRequest;
import com.ron.javainfohunter.api.dto.request.RegisterRequest;
import com.ron.javainfohunter.api.dto.response.AuthResponse;
import com.ron.javainfohunter.api.security.JwtService;
import com.ron.javainfohunter.entity.User;
import com.ron.javainfohunter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${javainfohunter.security.jwt.access-token-expiry:900000}")
    private long accessTokenExpiry;

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!user.getIsEnabled()) {
            throw new IllegalStateException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return generateAuthResponse(user);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .isEnabled(true)
                .build();

        userRepository.save(user);

        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        var storedToken = jwtService.validateRefreshToken(refreshToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getIsEnabled()) {
            throw new IllegalStateException("Account is disabled");
        }

        // Revoke old refresh token and issue new one
        storedToken.setIsRevoked(true);

        return generateAuthResponse(user);
    }

    public void logout(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiry / 1000)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
