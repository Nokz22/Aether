package com.aether.habits;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

/** Habits lifecycle against a real PostgreSQL, including per-user isolation. */
@SpringBootTest(properties = "aether.auth.open-registration=true")
@AutoConfigureMockMvc
@Testcontainers
class HabitsFlowTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void habitLifecycleWithStreaksAndIsolation() throws Exception {
        String alice = registerAndGetToken("alice@aether.app");

        mvc.perform(get("/api/habits").param("today", TODAY.toString()))
                .andExpect(status().isUnauthorized());

        // Create and list: fresh habit, nothing done, streak 0.
        MvcResult created = mvc.perform(post("/api/habits")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ler 10 páginas"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ler 10 páginas"))
                .andReturn();
        String habitId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        listExpecting(alice, "$[0].doneToday", "false");
        listExpecting(alice, "$[0].currentStreak", "0");

        // Today plus yesterday makes a streak of two; future days are refused.
        checkIn(alice, habitId, TODAY);
        checkIn(alice, habitId, TODAY.minusDays(1));
        mvc.perform(put("/api/habits/" + habitId + "/checkins/" + TODAY.plusDays(1))
                        .param("today", TODAY.toString())
                        .header("Authorization", "Bearer " + alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("future_checkin"));

        listExpecting(alice, "$[0].currentStreak", "2");
        listExpecting(alice, "$[0].doneToday", "true");

        // Unchecking today falls back to yesterday's streak.
        mvc.perform(delete("/api/habits/" + habitId + "/checkins/" + TODAY)
                        .header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
        listExpecting(alice, "$[0].currentStreak", "1");
        listExpecting(alice, "$[0].doneToday", "false");

        // Another user sees nothing and cannot touch Alice's habit.
        String bruno = registerAndGetToken("bruno@aether.app");
        mvc.perform(get("/api/habits").param("today", TODAY.toString())
                        .header("Authorization", "Bearer " + bruno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(put("/api/habits/" + habitId + "/checkins/" + TODAY)
                        .param("today", TODAY.toString())
                        .header("Authorization", "Bearer " + bruno))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("habit_not_found"));

        // Deleting the habit removes it and its history.
        mvc.perform(delete("/api/habits/" + habitId)
                        .header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/habits").param("today", TODAY.toString())
                        .header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
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

    private void checkIn(String token, String habitId, LocalDate date) throws Exception {
        mvc.perform(put("/api/habits/" + habitId + "/checkins/" + date)
                        .param("today", TODAY.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private void listExpecting(String token, String jsonPathExpr, String expected) throws Exception {
        mvc.perform(get("/api/habits").param("today", TODAY.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPathExpr).value(expected.equals("true") || expected.equals("false")
                        ? Boolean.valueOf(expected)
                        : Integer.valueOf(expected)));
    }
}
