package com.aether.calendar;

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

/** Calendar events against a real PostgreSQL: range overlap, validation, isolation. */
@SpringBootTest(properties = "aether.auth.open-registration=true")
@AutoConfigureMockMvc
@Testcontainers
class EventsFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void eventLifecycleRangeAndIsolation() throws Exception {
        String alice = registerAndGetToken("alice-cal@aether.app");

        mvc.perform(get("/api/events")
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isUnauthorized());

        // Create a one-hour event, sent with a timezone offset.
        MvcResult created = mvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dentist","startsAt":"2026-08-20T15:00:00+01:00","endsAt":"2026-08-20T16:00:00+01:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Dentist"))
                // Stored and returned as a UTC instant.
                .andExpect(jsonPath("$.startsAt").value("2026-08-20T14:00:00Z"))
                .andReturn();
        String eventId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        // End before start is rejected.
        mvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backwards","startsAt":"2026-08-20T16:00:00Z","endsAt":"2026-08-20T15:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_interval"));

        // A window that overlaps the event returns it.
        mvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + alice)
                        .param("from", "2026-08-20T00:00:00Z")
                        .param("to", "2026-08-21T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(eventId));

        // A window that does not overlap returns nothing.
        mvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + alice)
                        .param("from", "2026-08-21T00:00:00Z")
                        .param("to", "2026-08-22T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Reschedule via partial update (move the whole event later).
        mvc.perform(patch("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt":"2026-08-20T17:00:00Z","endsAt":"2026-08-20T18:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dentist"))
                .andExpect(jsonPath("$.startsAt").value("2026-08-20T17:00:00Z"));

        // Another user sees nothing and cannot touch Alice's event.
        String bruno = registerAndGetToken("bruno-cal@aether.app");
        mvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + bruno)
                        .param("from", "2026-08-20T00:00:00Z")
                        .param("to", "2026-08-21T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + bruno))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("event_not_found"));

        // Owner deletes it.
        mvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
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
