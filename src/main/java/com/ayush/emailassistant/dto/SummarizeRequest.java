package com.ayush.emailassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/email/summarize.
 *
 * @param emailContent the email text to summarize (required, non-blank)
 */
public record SummarizeRequest(
        @NotBlank(message = "emailContent must not be empty")
        @Size(max = 20000, message = "emailContent must not exceed 20000 characters")
        String emailContent
) {
}
