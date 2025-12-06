package com.vibeclip.controller;

import com.vibeclip.dto.user.UserResponse;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.UserMapper;
import com.vibeclip.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController {

    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        super(userService);
        this.userMapper = userMapper;
    }

    // Получение информации о текущем аутентифицированном пользователе
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserMe(Authentication authentication) {
        User user = getCurrentUser(authentication);
        UserResponse response = userMapper.toDTO(user);
        return ResponseEntity.ok(response);
    }
}

