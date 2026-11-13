package com.ayush.emailassistant.service;

import com.ayush.emailassistant.dto.SummarizeResponse;
import com.ayush.emailassistant.exception.AiServiceException;
import com.ayush.emailassistant.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Core AI logic. Uses Spring AI's {@link ChatClient} to talk to any
 * OpenAI-compatible chat endpoint (OpenAI by default, Groq when
 * OPENAI_BASE_URL / AI_MODEL are pointed at it).
 *
 * <p>Every provider failure is translated into an {@link AiServiceException}
 * with a readable message so the API returns a clean 502 JSON error instead of
 * a stack trace.
 */
@Service
public class EmailAssistantService {

    private static final Logger log = LoggerFactory.getLogger(EmailAssistantService.class);

    /** Must match the placeholder used as the default in application.properties. */
    static final String API_KEY_PLACEHOLDER = "not-configured-placeholder";

    static final String DEFAULT_TONE = "professional";

    private static final Map<String, String> TONE_INSTRUCTIONS = Map.of(
            "professional", "professional: polite, clear and businesslike, the safe default for workplace email",
            "friendly", "friendly: warm and personable, conversational but still appropriate for email",
            "concise", "concise: as short as possible while covering every essential point — no filler, no pleasantries beyond a brief greeting and sign-off",
            "formal", "formal: highly formal and respectful, complete sentences, no contractions, no casual wording"
    );

    private static final String REPLY_SYSTEM_PROMPT = """
            You are an expert email assistant. Your job is to draft a reply to the email the user provides.

            Rules:
            - Output ONLY the reply body, ready to copy-paste and send. No subject line, no explanations, no preamble, no quotation marks around the reply.
            - Address every point raised in the original email.
            - NEVER invent facts, names, dates, numbers, prices or commitments that are not stated in the original email. If information you would need is missing, leave a simple placeholder such as [name] or [date] instead of guessing.
            - Refer to the sender by name only if their name appears in the email; otherwise use a neutral greeting such as "Hello,".
            - Sign off generically (for example "Best regards") without inventing a sender name.
            - Write in the tone described here: %s
            """;

    private static final String SUMMARIZE_SYSTEM_PROMPT = """
            You summarize emails for a busy professional.

            Respond with ONLY a JSON object — no markdown fences, no commentary — in exactly this shape:
            {"summary": "...", "actionItems": ["...", "..."]}

            - "summary": 2 to 3 short lines covering who the email is from, what it is about and what (if anything) is needed from the reader.
            - "actionItems": every concrete task, request, deadline or follow-up the reader must act on, each as one short sentence, in the order they appear in the email. Use an empty list when the email needs no action.
            - NEVER invent facts, names, dates or commitments that are not stated in the email.
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String configuredApiKey;

    public EmailAssistantService(ChatClient.Builder chatClientBuilder,
                                 ObjectMapper objectMapper,
                                 @Value("${spring.ai.openai.api-key:}") String configuredApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.configuredApiKey = configuredApiKey;
    }

    /**
     * Drafts a reply to {@code emailContent} in the requested tone.
     *
     * @param tone one of professional / friendly / concise / formal;
     *             null or blank means professional
     * @throws BadRequestException for an unsupported tone value
     * @throws AiServiceException  when the AI provider is not configured or fails
     */
    public String generateReply(String emailContent, String tone) {
        String normalizedTone = normalizeTone(tone);
        ensureAiConfigured();

        String systemPrompt = REPLY_SYSTEM_PROMPT.formatted(TONE_INSTRUCTIONS.get(normalizedTone));
        String userPrompt = "Draft a reply to the following email:\n\n" + emailContent;

        String reply = callModel(systemPrompt, userPrompt);
        if (reply == null || reply.isBlank()) {
            throw new AiServiceException("The AI provider returned an empty reply. Please try again.");
        }
        return reply.trim();
    }

    /**
     * Summarizes {@code emailContent} and extracts its action items.
     *
     * @throws AiServiceException when the AI provider is not configured, fails,
     *                            or returns something that is not the expected JSON
     */
    public SummarizeResponse summarize(String emailContent) {
        ensureAiConfigured();

        String raw = callModel(SUMMARIZE_SYSTEM_PROMPT, "Summarize the following email:\n\n" + emailContent);
        return parseSummary(raw);
    }

    // ------------------------------------------------------------------ tones

    private String normalizeTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return DEFAULT_TONE;
        }
        String normalized = tone.trim().toLowerCase();
        if (!TONE_INSTRUCTIONS.containsKey(normalized)) {
            throw new BadRequestException(
                    "Unsupported tone '" + tone + "'. Supported tones: professional, friendly, concise, formal.");
        }
        return normalized;
    }

    // ------------------------------------------------------------- AI plumbing

    private void ensureAiConfigured() {
        if (configuredApiKey == null || configuredApiKey.isBlank()
                || API_KEY_PLACEHOLDER.equals(configuredApiKey)) {
            throw new AiServiceException(
                    "AI service is not configured: the OPENAI_API_KEY environment variable is missing. "
                            + "Set OPENAI_API_KEY (and optionally OPENAI_BASE_URL / AI_MODEL) and restart the application.");
        }
    }

    private String callModel(String systemPrompt, String userPrompt) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("AI provider call failed: {}", ex.getMessage());
            throw new AiServiceException("AI provider request failed: " + readableMessage(ex), ex);
        }
    }

    private SummarizeResponse parseSummary(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AiServiceException("The AI provider returned an empty summary. Please try again.");
        }
        String json = extractJson(raw);
        try {
            SummarizeResponse parsed = objectMapper.readValue(json, SummarizeResponse.class);
            if (parsed.summary() == null || parsed.summary().isBlank()) {
                throw new AiServiceException("The AI provider returned a summary in an unexpected format. Please try again.");
            }
            List<String> items = parsed.actionItems().stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
            return new SummarizeResponse(parsed.summary().trim(), items);
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not parse summarize response as JSON: {}", ex.getMessage());
            throw new AiServiceException("The AI provider returned a summary in an unexpected format. Please try again.", ex);
        }
    }

    /** Strips markdown fences and isolates the outermost JSON object, if present. */
    private String extractJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * Extracts a short, human-readable description of a provider failure.
     * OpenAI-compatible providers return errors as
     * {@code {"error": {"message": "..."}}}; that message is surfaced when present.
     */
    private String readableMessage(Throwable ex) {
        if (ex instanceof RestClientResponseException httpEx) {
            try {
                JsonNode root = objectMapper.readTree(httpEx.getResponseBodyAsString());
                JsonNode message = root.path("error").path("message");
                if (message.isTextual() && !message.asText().isBlank()) {
                    return message.asText();
                }
            } catch (Exception ignored) {
                // fall through to the generic message below
            }
            return "the provider responded with HTTP " + httpEx.getStatusCode().value() + ".";
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "unexpected error (" + ex.getClass().getSimpleName() + ").";
        }
        return message.length() > 300 ? message.substring(0, 300) + "…" : message;
    }
}
