package com.aether.ai.internal;

import static com.aether.ai.internal.ScriptedAnthropicClient.text;
import static com.aether.ai.internal.ScriptedAnthropicClient.toolUse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The chat endpoint against a real PostgreSQL, with the model answering from a
 * script: no API key and no network, so this runs anywhere CI does.
 */
@SpringBootTest(properties = {
        "aether.auth.open-registration=true",
        "aether.ai.api-key=test-key"
})
@AutoConfigureMockMvc
@Testcontainers
class AiChatFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    static class ScriptedModel {

        @Bean
        @Primary
        ScriptedAnthropicClient scriptedAnthropicClient() {
            return new ScriptedAnthropicClient();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptedAnthropicClient model;

    @Test
    void chatNeedsAuthentication() throws Exception {
        mvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(message("olá")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ai/messages")).andExpect(status().isUnauthorized());
    }

    @Test
    void conversationIsPersistedPerUserAndCanBeCleared() throws Exception {
        String alice = registerAndGetToken("alice-ai@aether.app");
        model.script(text("Olá, Nuno."));

        mvc.perform(chat(alice, "olá"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.content").value("Olá, Nuno."))
                .andExpect(jsonPath("$.toolsUsed").isEmpty());

        // Both sides are stored, oldest first.
        mvc.perform(get("/api/ai/messages").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value("olá"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"));

        // Another user's conversation is invisible.
        String bruno = registerAndGetToken("bruno-ai@aether.app");
        mvc.perform(get("/api/ai/messages").header("Authorization", "Bearer " + bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(delete("/api/ai/messages").header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/ai/messages").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void theAssistantActsThroughTheModulesAndReportsWhatItDid() throws Exception {
        String token = registerAndGetToken("carla-ai@aether.app");
        model.script(
                toolUse("call-1", "create_habit", Map.of("name", "Correr")),
                text("Criei o hábito Correr."));

        mvc.perform(chat(token, "cria o hábito correr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Criei o hábito Correr."))
                .andExpect(jsonPath("$.toolsUsed[0]").value("create_habit"));

        // The habit really exists, created through the habits module's own API.
        mvc.perform(get("/api/habits")
                        .header("Authorization", "Bearer " + token)
                        .param("today", "2026-09-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Correr"));
    }

    @Test
    void aMessageWithoutCalendarContextIsRejected() throws Exception {
        String token = registerAndGetToken("diogo-ai@aether.app");

        mvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"olá"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mvc.perform(chat(token, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void theToolsOfferedNeverIncludeDeleting() throws Exception {
        String token = registerAndGetToken("eva-ai@aether.app");
        model.script(text("Não consigo apagar nada."));

        mvc.perform(chat(token, "apaga as minhas notas")).andExpect(status().isOk());

        assertThat(model.lastCall().tools())
                .isNotEmpty()
                .extracting(ToolSpec::name)
                .noneMatch(name -> name.contains("delete") || name.contains("remove"));
    }

    private MockHttpServletRequestBuilder chat(String token, String message) {
        return post("/api/ai/chat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(message(message));
    }

    private String message(String message) {
        return """
                {"message":"%s","today":"2026-09-12","offsetMinutes":60}
                """.formatted(message);
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","displayName":"Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
