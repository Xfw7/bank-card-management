package com.example.bankcards.config;

import com.example.bankcards.config.properties.AdminProperties;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (!StringUtils.hasText(adminProperties.getPassword())) {
            log.warn("Admin user was not created: set ADMIN_PASSWORD in environment");
            return;
        }

        User admin = User.builder()
                .username(adminProperties.getUsername())
                .passwordHash(passwordEncoder.encode(adminProperties.getPassword()))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("Default admin user created: {}", admin.getUsername());
    }
}
