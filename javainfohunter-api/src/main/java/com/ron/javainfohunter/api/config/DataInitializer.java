package com.ron.javainfohunter.api.config;

import com.ron.javainfohunter.entity.User;
import com.ron.javainfohunter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${javainfohunter.security.initial-admin.username:admin}") String adminUsername,
            @Value("${javainfohunter.security.initial-admin.password:}") String adminPassword,
            @Value("${javainfohunter.security.initial-admin.email:admin@javainfohunter.local}") String adminEmail) {

        return args -> {
            if (adminPassword == null || adminPassword.isBlank()) {
                logger.info("No admin password configured, skipping admin user creation");
                return;
            }

            if (userRepository.existsByUsername(adminUsername)) {
                logger.info("Admin user '{}' already exists", adminUsername);
                return;
            }

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .isEnabled(true)
                    .build();

            userRepository.save(admin);
            logger.info("Created admin user '{}'", adminUsername);
        };
    }
}
