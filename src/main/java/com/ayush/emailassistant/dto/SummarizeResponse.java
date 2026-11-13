package com.ayush.emailassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body for POST /api/email/summarize. Also used as the parsing target
 * for the JSON the model is asked to return.
 *
 * @param summary     a 2–3 line summary of the email
 * @param actionItems concrete tasks / requests / deadlines extracted from the email
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SummarizeResponse(String summary, List<String> actionItems) {

    public SummarizeResponse {
        if (actionItems == null) {
            actionItems = List.of();
        }
    }
}
