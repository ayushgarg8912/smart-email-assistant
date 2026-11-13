package com.ayush.emailassistant.dto;

/**
 * Uniform JSON error body returned for every failure, e.g.
 * {@code { "error": "emailContent must not be empty" }}.
 * A stack trace is never exposed to clients.
 */
public record ErrorResponse(String error) {
}
