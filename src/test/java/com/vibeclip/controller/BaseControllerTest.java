package com.vibeclip.controller;

import com.vibeclip.entity.User;
import com.vibeclip.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BaseControllerTest {

    private final UserService userService = mock(UserService.class);

    private static class TestBaseController extends BaseController {
        public TestBaseController(UserService userService) {
            super(userService);
        }
    }

    @Test
    void getCurrentUser_success_shouldReturnUser() {
        // given
        TestBaseController controller = new TestBaseController(userService);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        User user = new User();
        user.setEmail("test@test.com");

        when(userService.findByEmail("test@test.com"))
                .thenReturn(Optional.of(user));

        User result = controller.getCurrentUser(authentication);

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());

        verify(userService).findByEmail("test@test.com");
    }

    @Test
    void getCurrentUser_userNotFound_shouldThrowException() {
        TestBaseController controller = new TestBaseController(userService);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        when(userService.findByEmail("test@test.com"))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> controller.getCurrentUser(authentication)
        );

        assertEquals("Пользователь не найден", ex.getMessage());

        verify(userService).findByEmail("test@test.com");
    }
}
