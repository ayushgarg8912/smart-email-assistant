package com.ayush.emailassistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/health → {"status": "ok"}.
 * Deliberately independent of the AI provider so it works with no API key set —
 * useful as a liveness check for local runs and hosted deployments (e.g. Render).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
