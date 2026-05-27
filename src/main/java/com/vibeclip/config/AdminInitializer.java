package com.vibeclip.config;

import com.vibeclip.entity.Role;
import com.vibeclip.entity.RoleName;
import com.vibeclip.entity.User;
import com.vibeclip.repository.RoleRepository;
import com.vibeclip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Проверяем ROLE_ADMIN
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(RoleName.ROLE_ADMIN)
                                .build()
                ));

        // Проверяем admin пользователя
        String adminEmail = "admin@vibeclip.com";

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin пользователь уже существует");
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .privateProfile(false)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);

        log.info("ADMIN пользователь создан");
        log.info("EMAIL: {}", adminEmail);
        log.info("PASSWORD: admin123");
    }
}