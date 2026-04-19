package com.ron.javainfohunter.api.service;

import com.ron.javainfohunter.api.dto.request.LoginRequest;
import com.ron.javainfohunter.api.dto.request.RegisterRequest;
import com.ron.javainfohunter.api.exception.BusinessException;
import com.ron.javainfohunter.api.security.JwtService;
import com.ron.javainfohunter.entity.User;
import com.ron.javainfohunter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, passwordEncoder);
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    private RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    void login_withInvalidUsername_shouldThrowBusinessException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("unknown", "pass")));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void login_withDisabledAccount_shouldThrowBusinessException() {
        User disabledUser = User.builder()
                .username("disabled")
                .passwordHash("hash")
                .isEnabled(false)
                .build();
        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(disabledUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("disabled", "pass")));

        assertEquals("Account is disabled", ex.getMessage());
    }

    @Test
    void login_withWrongPassword_shouldThrowBusinessException() {
        User user = User.builder()
                .username("user")
                .passwordHash("hash")
                .isEnabled(true)
                .build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("user", "wrong")));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void register_withDuplicateUsername_shouldThrowBusinessException() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(registerRequest("existing", "a@b.com", "password123")));

        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void register_withDuplicateEmail_shouldThrowBusinessException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@email.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(registerRequest("newuser", "taken@email.com", "password123")));

        assertEquals("Email already exists", ex.getMessage());
    }
}
