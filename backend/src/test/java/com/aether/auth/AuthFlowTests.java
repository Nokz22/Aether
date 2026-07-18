package com.aether.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

/** Full auth lifecycle against a real PostgreSQL, exactly as the frontend will use it. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowTests {

    private static final String REFRESH_COOKIE = "aether_refresh";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthenticationLifecycle() throws Exception {
        // 1. First account registers even though registration is closed by default.
        MvcResult registered = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nuno@aether.app","password":"password123","displayName":"Nuno"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("nuno@aether.app"))
                .andReturn();

        Cookie refreshCookie = registered.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getSecure()).isTrue();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/auth");

        // 2. A second registration is refused: registration closed after the first account.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"other@aether.app","password":"password123","displayName":"Other"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("registration_closed"));

        // 3. Wrong password fails with a non-revealing error.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nuno@aether.app","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));

        // 4. Correct credentials log in.
        MvcResult loggedIn = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nuno@aether.app","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String accessToken = accessTokenOf(loggedIn);
        Cookie loginCookie = loggedIn.getResponse().getCookie(REFRESH_COOKIE);

        // 5. The access token authenticates /me; no token means 401.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuno@aether.app"))
                .andExpect(jsonPath("$.displayName").value("Nuno"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        // 6. Refresh rotates the token: the new one works, the used one is dead.
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(loginCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie rotatedCookie = refreshed.getResponse().getCookie(REFRESH_COOKIE);
        mvc.perform(post("/api/auth/refresh").cookie(loginCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));

        // 7. Logout revokes the rotated token and clears the cookie.
        MvcResult loggedOut = mvc.perform(post("/api/auth/logout").cookie(rotatedCookie))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie clearedCookie = loggedOut.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isZero();
        mvc.perform(post("/api/auth/refresh").cookie(rotatedCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorsCarryFieldDetails() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fields.email").isNotEmpty())
                .andExpect(jsonPath("$.fields.password").isNotEmpty())
                .andExpect(jsonPath("$.fields.displayName").isNotEmpty());
    }

    private String accessTokenOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
