package com.inkling.exception;

/**
 * Thrown when request validation fails.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
