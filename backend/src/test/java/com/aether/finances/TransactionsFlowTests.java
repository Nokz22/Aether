package com.aether.finances;

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

/** Transactions against a real PostgreSQL: summary, range, validation, isolation. */
@SpringBootTest(properties = "aether.auth.open-registration=true")
@AutoConfigureMockMvc
@Testcontainers
class TransactionsFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void transactionLifecycleSummaryAndIsolation() throws Exception {
        String alice = registerAndGetToken("alice-fin@aether.app");

        mvc.perform(get("/api/transactions").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isUnauthorized());

        // Income and two expenses in August.
        String salaryId = create(alice, """
                {"type":"INCOME","amountCents":200000,"category":"Salário","date":"2026-08-01"}
                """);
        create(alice, """
                {"type":"EXPENSE","amountCents":5000,"category":"Comida","description":"almoço","date":"2026-08-15"}
                """);
        create(alice, """
                {"type":"EXPENSE","amountCents":3000,"category":"Comida","date":"2026-08-16"}
                """);

        // Negative amounts are rejected.
        mvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","amountCents":-100,"date":"2026-08-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        // Summary over August: income 2000.00, expense 80.00, net 1920.00.
        mvc.perform(get("/api/transactions/summary")
                        .header("Authorization", "Bearer " + alice)
                        .param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeCents").value(200000))
                .andExpect(jsonPath("$.expenseCents").value(8000))
                .andExpect(jsonPath("$.netCents").value(192000))
                .andExpect(jsonPath("$.byCategory[0].category").value("Salário"));

        // A July window excludes the August rows.
        mvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + alice)
                        .param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Reclassify the salary amount via partial update.
        mvc.perform(patch("/api/transactions/" + salaryId)
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amountCents":250000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountCents").value(250000))
                .andExpect(jsonPath("$.category").value("Salário"));

        // Another user sees nothing and cannot touch Alice's transaction.
        String bruno = registerAndGetToken("bruno-fin@aether.app");
        mvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + bruno)
                        .param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/transactions/" + salaryId).header("Authorization", "Bearer " + bruno))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("transaction_not_found"));

        // Owner deletes it.
        mvc.perform(delete("/api/transactions/" + salaryId).header("Authorization", "Bearer " + alice))
                .andExpect(status().isNoContent());
    }

    private String create(String token, String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
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
