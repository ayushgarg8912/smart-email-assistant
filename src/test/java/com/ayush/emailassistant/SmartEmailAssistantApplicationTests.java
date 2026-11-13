package com.ayush.emailassistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests that run WITHOUT any live API key: the health endpoint must work
 * with no key configured, and input validation must reject empty emails with a
 * clean 400 JSON error before any AI call is attempted.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SmartEmailAssistantApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsOkWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void emptyEmailIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/email/generate-reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\": \"\", \"tone\": \"professional\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void emptySummarizeEmailIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/email/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailContent\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
