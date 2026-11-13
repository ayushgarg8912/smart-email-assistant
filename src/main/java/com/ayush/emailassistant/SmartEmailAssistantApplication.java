package com.ayush.emailassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smart Email Assistant — a Spring Boot + Spring AI application that drafts
 * tone-aware email replies and extracts summaries with action items.
 */
@SpringBootApplication
public class SmartEmailAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEmailAssistantApplication.class, args);
    }
}
