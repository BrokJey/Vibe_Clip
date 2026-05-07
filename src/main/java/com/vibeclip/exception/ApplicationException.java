package com.vibeclip.exception;

public abstract class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }
}