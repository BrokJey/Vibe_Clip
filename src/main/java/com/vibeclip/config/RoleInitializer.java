package com.vibeclip.config;

import com.vibeclip.entity.Role;
import com.vibeclip.entity.RoleName;
import com.vibeclip.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Компонент для инициализации ролей при старте приложения.
 * Создает роли, если их нет в базе данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initRoles() {
        log.info("Проверка наличия ролей в базе данных...");

        // Создаем ROLE_USER, если его нет
        if (roleRepository.findByName(RoleName.ROLE_USER).isEmpty()) {
            Role userRole = Role.builder()
                    .name(RoleName.ROLE_USER)
                    .build();
            roleRepository.save(userRole);
            log.info("Создана роль: ROLE_USER");
        }

        // Создаем ROLE_CREATOR, если его нет
        if (roleRepository.findByName(RoleName.ROLE_CREATOR).isEmpty()) {
            Role creatorRole = Role.builder()
                    .name(RoleName.ROLE_CREATOR)
                    .build();
            roleRepository.save(creatorRole);
            log.info("Создана роль: ROLE_CREATOR");
        }

        // Создаем ROLE_ADMIN, если его нет
        if (roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                    .name(RoleName.ROLE_ADMIN)
                    .build();
            roleRepository.save(adminRole);
            log.info("Создана роль: ROLE_ADMIN");
        }

        log.info("Проверка ролей завершена. Все необходимые роли присутствуют в базе данных.");
    }
}

