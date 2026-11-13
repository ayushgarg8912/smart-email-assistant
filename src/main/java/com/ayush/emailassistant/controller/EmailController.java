package com.ayush.emailassistant.controller;

import com.ayush.emailassistant.dto.GenerateReplyRequest;
import com.ayush.emailassistant.dto.GenerateReplyResponse;
import com.ayush.emailassistant.dto.SummarizeRequest;
import com.ayush.emailassistant.dto.SummarizeResponse;
import com.ayush.emailassistant.service.EmailAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the Smart Email Assistant.
 *
 * <ul>
 *   <li>POST /api/email/generate-reply — {emailContent, tone?} → {reply}</li>
 *   <li>POST /api/email/summarize      — {emailContent} → {summary, actionItems}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailAssistantService emailAssistantService;

    public EmailController(EmailAssistantService emailAssistantService) {
        this.emailAssistantService = emailAssistantService;
    }

    @PostMapping("/generate-reply")
    public ResponseEntity<GenerateReplyResponse> generateReply(@Valid @RequestBody GenerateReplyRequest request) {
        String reply = emailAssistantService.generateReply(request.emailContent(), request.tone());
        return ResponseEntity.ok(new GenerateReplyResponse(reply));
    }

    @PostMapping("/summarize")
    public ResponseEntity<SummarizeResponse> summarize(@Valid @RequestBody SummarizeRequest request) {
        SummarizeResponse result = emailAssistantService.summarize(request.emailContent());
        return ResponseEntity.ok(result);
    }
}
