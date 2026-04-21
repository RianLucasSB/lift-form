package com.rianlucassb.liftform.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Integration Tests")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String REFRESH_URL  = "/api/v1/auth/refresh";

    // --------------------------------------------------------------- register
    @Test
    @DisplayName("POST /register returns 200 and accessToken for valid request")
    void register_validRequest_returns200WithAccessToken() throws Exception {
        var body = Map.of("email", "new@example.com", "username", "newuser", "password", "password123");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    @DisplayName("POST /register returns 409 when email already exists")
    void register_duplicateEmail_returns409() throws Exception {
        var body = Map.of("email", "dup@example.com", "username", "dupuser", "password", "password123");

        // first registration succeeds
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // second registration with same email fails
        var body2 = Map.of("email", "dup@example.com", "username", "dupuser2", "password", "password123");
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /register returns 400 for invalid payload (short password)")
    void register_shortPassword_returns400() throws Exception {
        var body = Map.of("email", "short@example.com", "username", "shortpwd", "password", "abc");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------------- login
    @Test
    @DisplayName("POST /login returns 200 and accessToken for valid credentials")
    void login_validCredentials_returns200() throws Exception {
        // register first
        var reg = Map.of("email", "login@example.com", "username", "loginuser", "password", "password123");
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        // login
        var login = Map.of("login", "login@example.com", "password", "password123");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    @DisplayName("POST /login returns 401 for wrong password")
    void login_wrongPassword_returns401() throws Exception {
        var reg = Map.of("email", "wrongpwd@example.com", "username", "wrongpwduser", "password", "correctpwd1");
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        var login = Map.of("login", "wrongpwd@example.com", "password", "badpassword");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login returns 401 for unknown user")
    void login_unknownUser_returns401() throws Exception {
        var login = Map.of("login", "ghost@example.com", "password", "somepassword");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------------------------- refresh
    @Test
    @DisplayName("POST /refresh returns 200 and new accessToken for valid refresh cookie")
    void refresh_validCookie_returns200WithNewAccessToken() throws Exception {
        // register to get a refresh token cookie
        var reg = Map.of("email", "refresh@example.com", "username", "refreshuser", "password", "password123");
        MvcResult regResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshCookie = regResult.getResponse().getHeader("Set-Cookie");
        assertThat(refreshCookie).isNotNull();
        String refreshToken = extractCookieValue(refreshCookie, "refresh_token");

        // use the refresh token
        mockMvc.perform(post(REFRESH_URL)
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /refresh returns 401 when refresh token cookie is missing")
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post(REFRESH_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /refresh returns 401 for invalid refresh token")
    void refresh_invalidToken_returns401() throws Exception {
        mockMvc.perform(post(REFRESH_URL)
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- helpers
    private String extractCookieValue(String setCookieHeader, String name) {
        for (String part : setCookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith(name + "=")) {
                return part.substring((name + "=").length());
            }
        }
        throw new IllegalArgumentException("Cookie '" + name + "' not found in: " + setCookieHeader);
    }
}

