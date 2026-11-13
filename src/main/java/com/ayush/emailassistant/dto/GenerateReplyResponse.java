package com.ayush.emailassistant.dto;

/**
 * Response body for POST /api/email/generate-reply.
 *
 * @param reply the drafted reply, ready to copy-paste and send
 */
public record GenerateReplyResponse(String reply) {
}
