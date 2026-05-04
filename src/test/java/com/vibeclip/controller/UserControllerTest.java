package com.vibeclip.controller;

import com.vibeclip.dto.user.UserResponse;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.UserMapper;
import com.vibeclip.service.JwtService;
import com.vibeclip.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@test.com")
    void getUserMe_success_shouldReturnCurrentUser() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setUsername("testuser");

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(Set.of("ROLE_USER"))
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication("user@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).findByEmail("user@test.com");
        verify(userMapper).toDTO(user);
    }

    @Test
    @WithMockUser(username = "missing@test.com")
    void getUserMe_userNotFound_shouldReturnBadRequest() throws Exception {
        when(userService.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication("missing@test.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Пользователь не найден"));
    }

    private Authentication authentication(String username) {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .authorities("ROLE_USER")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
