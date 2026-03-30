package com.vibeclip.service;

import com.vibeclip.dto.auth.AuthResponse;
import com.vibeclip.dto.auth.LoginRequest;
import com.vibeclip.dto.auth.RegisterRequest;
import com.vibeclip.entity.Role;
import com.vibeclip.entity.RoleName;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.UserMapper;
import com.vibeclip.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");
        request.setUsername("test");
        request.setPassword("123");

        User user = new User();
        User savedUser = new User();
        Role role = new Role();
        role.setName(RoleName.ROLE_USER);

        when(userService.emailExists(request.getEmail())).thenReturn(false);
        when(userService.usernameExists(request.getUsername())).thenReturn(false);
        when(userMapper.fromDTO(request)).thenReturn(user);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("123")).thenReturn("encoded");
        when(userService.save(user)).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertEquals("token", response.getAccessToken());

        verify(passwordEncoder).encode("123");
        verify(userService).save(user);
        verify(jwtService).generateToken(eq(savedUser.getEmail()), any());

        assertEquals("encoded", user.getPassword());
        assertTrue(user.getRoles().contains(role));
    }

    @Test
    void register_emailExists_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");

        when(userService.emailExists(request.getEmail())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void register_usernameExists_shouldThrow() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("test");

        when(userService.usernameExists(request.getUsername())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void register_roleNotFound_shouldThrow() {
        RegisterRequest request = new RegisterRequest();

        when(userService.emailExists(request.getEmail())).thenReturn(false);
        when(userService.usernameExists(request.getUsername())).thenReturn(false);
        when(userMapper.fromDTO(request)).thenReturn(new User());
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("123");

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(request.getEmail());
        when(userDetails.getAuthorities()).thenReturn(List.of());
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        AuthResponse response = authService.login(request);

        assertEquals("token", response.getAccessToken());

        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateToken(any(), any());
    }

    @Test
    void login_badCredentials_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });

        verify(authenticationManager).authenticate(any());
    }
}
