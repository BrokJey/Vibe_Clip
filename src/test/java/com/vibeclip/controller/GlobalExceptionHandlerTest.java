package com.vibeclip.controller;

import com.vibeclip.dto.auth.RegisterRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalState_shouldReturnBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(
                new IllegalStateException("Некорректное состояние")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBodyMessage(response, "Некорректное состояние");
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Некорректный аргумент")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBodyMessage(response, "Некорректный аргумент");
    }

    @Test
    void handleUsernameNotFound_shouldReturnNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleUsernameNotFound(
                new UsernameNotFoundException("Пользователь не найден")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertBodyMessage(response, "Пользователь не найден");
    }

    @Test
    void handleValidationExceptions_shouldReturnValidationErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new RegisterRequest(), "request");
        bindingResult.addError(new FieldError("request", "email", "Некорректный email"));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", RegisterRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBodyMessage(response, "Ошибка валидации");

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals("Некорректный email", errors.get("email"));
    }

    @Test
    void handleMaxUploadSizeExceeded_shouldReturnPayloadTooLarge() {
        ResponseEntity<Map<String, Object>> response = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(1024)
        );

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("Размер файла превышает"));
    }

    @Test
    void handleExpiredJwt_shouldReturnUnauthorized() {
        ResponseEntity<Map<String, Object>> response = handler.handleExpiredJwt(
                new ExpiredJwtException(mock(Header.class), mock(Claims.class), "expired")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertBodyMessage(response, "Токен доступа истек. Пожалуйста, войдите в систему снова.");
    }

    @Test
    void handleAccessDenied_shouldReturnForbidden() {
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(
                new AccessDeniedException("denied")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertBodyMessage(response, "Доступ запрещен. У вас нет прав для выполнения этого действия.");
    }

    @Test
    void handleAuthentication_shouldReturnUnauthorized() {
        ResponseEntity<Map<String, Object>> response = handler.handleAuthentication(
                new InsufficientAuthenticationException("auth required")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertBodyMessage(response, "Требуется аутентификация. Пожалуйста, войдите в систему.");
    }

    @Test
    void handleBadCredentials_shouldReturnUnauthorized() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(
                new BadCredentialsException("bad")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertBodyMessage(response, "Неверные учетные данные.");
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(
                new RuntimeException("boom")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertBodyMessage(response, "Внутренняя ошибка сервера");
    }

    @SuppressWarnings("unused")
    private static void validationTarget(RegisterRequest request) {
    }

    private void assertBodyMessage(ResponseEntity<Map<String, Object>> response, String expectedMessage) {
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}
