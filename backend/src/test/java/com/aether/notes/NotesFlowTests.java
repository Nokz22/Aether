package com.aether.notes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Notes CRUD against a real PostgreSQL, including per-user isolation. */
@SpringBootTest(properties = "aether.auth.open-registration=true")
@AutoConfigureMockMvc
@Testcontainers
class NotesFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void noteLifecycleWithIsolation() throws Exception {
        String alice = registerAndGetToken("alice-notes@aether.app");

        mvc.perform(get("/api/notes")).andExpect(status().isUnauthorized());

        // Create.
        MvcResult created = mvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Groceries","content":"milk\\nbread"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.content").value("milk\nbread"))
                .andReturn();
        String noteId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        // List shows a summary with a preview from the first line.
        mvc.perform(get("/api/notes").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].preview").value("milk"));

        // Partial update: content only, title kept.
        mvc.perform(patch("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"milk\\neggs\\nbread"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Groceries"))
                .andExpect(jsonPath("$.content").value("milk\neggs\nbread"));

        // Empty content is rejected.
        mvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"x","content":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        // Another user sees nothing and cannot read or delete Alice's note.
        String bruno = registerAndGetToken("bruno-notes@aether.app");
        mvc.perform(get("/api/notes").header("Authorization", "Bearer " + bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/notes/" + noteId).header("Authorization", "Bearer " + bruno))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("note_not_found"));
        mvc.perform(delete("/api/notes/" + noteId).header("Authorization", "Bearer " + bruno))
                .andExpect(status().isNotFound());

        // Owner deletes it.
        mvc.perform(delete("/api/notes/" + noteId).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notes/" + noteId).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNotFound());
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
