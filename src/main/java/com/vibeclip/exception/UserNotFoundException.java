package com.vibeclip.exception;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(String username) {
        super("Пользователь не найден: " + username);
    }

    public UserNotFoundException(String message, boolean raw) {
        super(message);
    }
}