package com.aether.projects;

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

/** Projects + tasks against a real PostgreSQL, including per-user isolation. */
@SpringBootTest(properties = "aether.auth.open-registration=true")
@AutoConfigureMockMvc
@Testcontainers
class ProjectsFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void projectAndTaskLifecycleWithIsolation() throws Exception {
        String alice = registerAndGetToken("alice-proj@aether.app");

        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());

        // Create a project.
        MvcResult created = mvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Website","description":"Marketing site"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Website"))
                .andExpect(jsonPath("$.tasks").isEmpty())
                .andReturn();
        String projectId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        // Add tasks — they start as TODO.
        String taskId = addTask(alice, projectId, "Draft copy");
        addTask(alice, projectId, "Pick colors");

        // Move one task to DOING.
        mvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DOING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Draft copy"))
                .andExpect(jsonPath("$.status").value("DOING"));

        // The list summary reflects the counts.
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskCounts.todo").value(1))
                .andExpect(jsonPath("$[0].taskCounts.doing").value(1))
                .andExpect(jsonPath("$[0].taskCounts.done").value(0));

        // An invalid status is rejected.
        mvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"NOPE"}
                                """))
                .andExpect(status().isBadRequest());

        // Another user can neither see nor touch Alice's project or its tasks.
        String bruno = registerAndGetToken("bruno-proj@aether.app");
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/projects/" + projectId).header("Authorization", "Bearer " + bruno))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("project_not_found"));
        mvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + bruno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"sneaky"}
                                """))
                .andExpect(status().isNotFound());

        // Deleting the project removes it and cascades its tasks.
        mvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + projectId).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNotFound());
    }

    private String addTask(String token, String projectId, String title) throws Exception {
        MvcResult result = mvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
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
