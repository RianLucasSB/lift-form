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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController Integration Tests")
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String ME_URL = "/api/v1/users/me";

    // --------------------------------------------------------------- me
    @Test
    @DisplayName("GET /users/me returns 200 with the authenticated user's username and email")
    void getCurrentUser_authenticatedUser_returns200WithUsernameAndEmail() throws Exception {
        String email = "me_" + UUID.randomUUID() + "@example.com";
        String username = "me_" + UUID.randomUUID();
        String accessToken = registerUser(email, username);

        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("GET /users/me returns 403 when no token is provided")
    void getCurrentUser_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/me returns 403 for invalid JWT")
    void getCurrentUser_invalidToken_returns403() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers
    private String registerUser(String email, String username) throws Exception {
        var reg = Map.of("email", email, "username", username, "password", "password123");

        MvcResult result = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("accessToken");
    }
}
