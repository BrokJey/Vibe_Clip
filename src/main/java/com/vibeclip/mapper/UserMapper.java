package com.vibeclip.mapper;

import com.vibeclip.dto.auth.RegisterRequest;
import com.vibeclip.dto.user.UserResponse;
import com.vibeclip.entity.Role;
import com.vibeclip.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct генерирует реализацию этого интерфейса автоматически.
 * Реализация будет в target/generated-sources/annotations/
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Преобразует RegisterRequest в User entity (без пароля и ролей)
     * Пароль и роли должны быть установлены отдельно в сервисе
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User fromDTO(RegisterRequest request);

    /**
     * Преобразует User entity в UserResponse DTO (без пароля)
     * Роли преобразуются в Set<String> через кастомный метод
     */
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringSet")
    UserResponse toDTO(User user);

    /**
     * Обновляет существующего User данными из RegisterRequest
     * Используется для обновления профиля (без пароля)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget User user, RegisterRequest request);

    /**
     * Кастомный метод для преобразования Set<Role> в Set<String>
     */
    @Named("rolesToStringSet")
    default Set<String> rolesToStringSet(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}

