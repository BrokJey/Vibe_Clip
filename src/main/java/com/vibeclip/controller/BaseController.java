package com.vibeclip.controller;

import com.vibeclip.entity.User;
import com.vibeclip.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

// Базовый контроллер с общими методами для всех контроллеров
public abstract class BaseController {

    protected final UserService userService;

    protected BaseController(UserService userService) {
        this.userService = userService;
    }

    // Получает текущего аутентифицированного пользователя из SecurityContext
    protected User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }
}

