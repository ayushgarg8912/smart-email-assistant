package com.ayush.emailassistant.exception;

/**
 * Raised for client-side input problems that Bean Validation cannot express
 * (for example an unsupported tone value). Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
