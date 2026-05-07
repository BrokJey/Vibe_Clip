package com.vibeclip.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "USER_NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(AlreadySubscribedException.class)
    public ResponseEntity<?> handleAlreadySubscribed(AlreadySubscribedException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "ALREADY_SUBSCRIBED",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", ex.getMessage()
                ));
    }
}