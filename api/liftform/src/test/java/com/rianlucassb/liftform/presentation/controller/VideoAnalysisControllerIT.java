package com.rianlucassb.liftform.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("VideoAnalysisController Integration Tests")
class VideoAnalysisControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREATE_URL   = "/api/v1/analysis/create";
    private static final String REGISTER_URL = "/api/v1/auth/register";

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // register a user and grab the access token
        var reg = Map.of(
                "email", "analyst_" + UUID.randomUUID() + "@example.com",
                "username", "analyst_" + UUID.randomUUID(),
                "password", "password123"
        );

        MvcResult result = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        accessToken = (String) body.get("accessToken");
        assertThat(accessToken).isNotBlank();
    }

    // --------------------------------------------------------------- create
    @Test
    @DisplayName("POST /analysis/create returns 200 with uploadUrl and expiresIn when authenticated")
    void createAnalysis_authenticatedUser_returns200() throws Exception {
        var body = Map.of("exerciseType", "SQUAT", "fileName", "my_squat.mp4");

        MvcResult result = mockMvc.perform(post(CREATE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").isNotEmpty())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat((String) response.get("uploadUrl")).isNotBlank();
        assertThat((Integer) response.get("expiresIn")).isEqualTo(900); // 15 minutes
    }

    @Test
    @DisplayName("POST /analysis/create returns 403 when no token is provided")
    void createAnalysis_unauthenticated_returns403() throws Exception {
        var body = Map.of("exerciseType", "SQUAT", "fileName", "squat.mp4");

        mockMvc.perform(post(CREATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /analysis/create returns 403 for invalid JWT")
    void createAnalysis_invalidToken_returns403() throws Exception {
        var body = Map.of("exerciseType", "SQUAT", "fileName", "squat.mp4");

        mockMvc.perform(post(CREATE_URL)
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /analysis/create returns 400 for unknown exerciseType")
    void createAnalysis_invalidExerciseType_returns400() throws Exception {
        var body = Map.of("exerciseType", "DEADLIFT", "fileName", "deadlift.mp4");

        mockMvc.perform(post(CREATE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}



