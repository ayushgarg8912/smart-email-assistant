package com.ayush.emailassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/email/generate-reply.
 *
 * @param emailContent the original email text to reply to (required, non-blank)
 * @param tone         desired reply tone: professional, friendly, concise or formal.
 *                     Optional — defaults to "professional" when omitted or blank.
 */
public record GenerateReplyRequest(
        @NotBlank(message = "emailContent must not be empty")
        @Size(max = 20000, message = "emailContent must not exceed 20000 characters")
        String emailContent,

        String tone
) {
}
