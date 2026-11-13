package com.ayush.emailassistant.exception;

/**
 * Raised when the AI provider cannot serve a request — the API key is missing,
 * the provider rejected the call (invalid key, rate limit, model unavailable),
 * or the provider's response could not be understood. Mapped to HTTP 502 with a
 * readable message; the underlying stack trace is never sent to clients.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
