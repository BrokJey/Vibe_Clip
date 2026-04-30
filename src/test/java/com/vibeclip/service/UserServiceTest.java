package com.vibeclip.service;

import com.vibeclip.entity.Role;
import com.vibeclip.entity.RoleName;
import com.vibeclip.entity.User;
import com.vibeclip.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findByEmail_userExists() {
        String email = "test@mail.com";

        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());

        verify(userRepository).findByEmail(email);
    }

    @Test
    void findByEmail_userNotFound() {
        String email = "notfound@mail.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail(email);

        assertFalse(result.isPresent());

        verify(userRepository).findByEmail(email);
    }

    @Test
    void emailExists_true() {
        String email = "test@mail.com";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        boolean result = userService.emailExists(email);

        assertTrue(result);

        verify(userRepository).existsByEmail(email);
    }

    @Test
    void emailExists_false() {
        String email = "notfound@mail.com";

        when(userRepository.existsByEmail(email)).thenReturn(false);

        boolean result = userService.emailExists(email);

        assertFalse(result);

        verify(userRepository).existsByEmail(email);
    }

    @Test
    void usernameExists_shouldReturnTrue() {
        String username = "testUser";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        boolean result = userService.usernameExists(username);

        assertTrue(result);

        verify(userRepository).existsByUsername(username);
    }

    @Test
    void usernameExists_shouldReturnFalse() {
        String username = "testUser";

        when(userRepository.existsByUsername(username)).thenReturn(false);

        boolean result = userService.usernameExists(username);

        assertFalse(result);

        verify(userRepository).existsByUsername(username);
    }

    @Test
    void save_shouldReturnSavedUser() {
        User user = new User();
        user.setUsername("testUser");

        User savedUser = new User();
        savedUser.setUsername("testUser");
        savedUser.setId(UUID.randomUUID());

        when(userRepository.save(user)).thenReturn(savedUser);

        User result = userService.save(user);

        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(savedUser.getUsername(), result.getUsername());

        verify(userRepository).save(user);
    }

    @Test
    void save_shouldCallRepository() {
        User user = new User();

        when(userRepository.save(user)).thenReturn(user);

        userService.save(user);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void loadUserByUsername_success() {
        String email = "test@mail.com";

        Role role = new Role();
        role.setName(RoleName.ROLE_USER);

        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setRoles(Set.of(role));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername(email);

        assertNotNull(result);
        assertEquals(email, result.getUsername());
        assertEquals("password", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_userNotFound_shouldThrow() {
        String email = "notfound@mail.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(email)
        );

        assertTrue(ex.getMessage().contains(email));

        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_multipleRoles() {
        String email = "test@mail.com";

        Role role1 = new Role();
        role1.setName(RoleName.ROLE_USER);

        Role role2 = new Role();
        role2.setName(RoleName.ROLE_ADMIN);

        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setRoles(Set.of(role1, role2));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername(email);

        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    
}
