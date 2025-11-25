package com.vibeclip.service;

import com.vibeclip.dto.auth.AuthResponse;
import com.vibeclip.dto.auth.LoginRequest;
import com.vibeclip.dto.auth.RegisterRequest;
import com.vibeclip.entity.Role;
import com.vibeclip.entity.RoleName;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.UserMapper;
import com.vibeclip.repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserService userService,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userService.emailExists(request.getEmail())) {
            throw new IllegalStateException("Email already registered");
        }
        if (userService.usernameExists(request.getUsername())) {
            throw new IllegalStateException("Username already in use");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Default role missing"));
        user.addRole(userRole);

        User saved = userService.save(user);
        String token = jwtService.generateToken(saved.getEmail(), saved.getRoles());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails.getUsername(), userDetails.getAuthorities());
        return new AuthResponse(token);
    }
}

