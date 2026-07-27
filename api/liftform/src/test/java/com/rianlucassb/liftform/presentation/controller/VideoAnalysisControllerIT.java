package com.rianlucassb.liftform.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.AbstractIntegrationTest;
import com.rianlucassb.liftform.core.domain.model.AnalysisResult;
import com.rianlucassb.liftform.core.domain.model.VideoAnalysis;
import com.rianlucassb.liftform.core.gateway.analysis.VideoAnalysisRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("VideoAnalysisController Integration Tests")
class VideoAnalysisControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoAnalysisRepository videoAnalysisRepository;

    @Autowired
    private S3Client s3Client;

    private static final String CREATE_URL   = "/api/v1/analysis/create";
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String CONFIRM_ANALYSIS_VIDEO_UPLOAD_URL   = "/api/v1/analysis/videos/%d/upload-complete";
    private static final String GET_ANALYSIS_URL = "/api/v1/analysis/%d";
    private static final String LIST_ANALYSES_URL = "/api/v1/analysis";

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // register a user and grab the access token
        accessToken = registerUser();
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

    @Test
    @DisplayName("POST /analysis/videos/{id}/upload-complete returns 200 when authenticated and video exists")
    void confirmVideoUpload_authenticatedUser_existingCreatedAnalysis_returns200() throws Exception {
        Long analysisId = createAnalysisAndUploadFakeVideo(new byte[1024], "video/mp4");

        mockMvc.perform(post(CONFIRM_ANALYSIS_VIDEO_UPLOAD_URL.formatted(analysisId))
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /analysis/videos/{id}/upload-complete returns 413 when uploaded file exceeds the max size")
    void confirmVideoUpload_fileTooLarge_returns413() throws Exception {
        Long analysisId = createAnalysisAndUploadFakeVideo(new byte[600 * 1024 * 1024], "video/mp4");

        mockMvc.perform(post(CONFIRM_ANALYSIS_VIDEO_UPLOAD_URL.formatted(analysisId))
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("POST /analysis/videos/{id}/upload-complete returns 415 when uploaded file content type is not supported")
    void confirmVideoUpload_unsupportedContentType_returns415() throws Exception {
        Long analysisId = createAnalysisAndUploadFakeVideo(new byte[1024], "video/quicktime");

        mockMvc.perform(post(CONFIRM_ANALYSIS_VIDEO_UPLOAD_URL.formatted(analysisId))
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("POST /analysis/videos/{id}/upload-complete returns 403 for invalid JWT")
    void confirmVideoUpload_invalidToken_returns403() throws Exception {

        mockMvc.perform(post(CONFIRM_ANALYSIS_VIDEO_UPLOAD_URL.formatted(1L))
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /analysis/videos/{id}/upload-complete returns 404 for invalid analysis ID")
    void confirmVideoUpload_invalidAnalysisId_returns409() throws Exception {
        mockMvc.perform(post(CONFIRM_ANALYSIS_VIDEO_UPLOAD_URL.formatted(1L))
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------------------------------- get analysis
    @Test
    @DisplayName("GET /analysis/{id} returns 200 with status and null result while not completed")
    void getAnalysis_ownedCreatedAnalysis_returns200WithNullResult() throws Exception {
        Long analysisId = createAnalysis(accessToken);

        mockMvc.perform(get(GET_ANALYSIS_URL.formatted(analysisId))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.result").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("GET /analysis/{id} returns 200 with result when analysis is completed")
    void getAnalysis_completedAnalysis_returns200WithResult() throws Exception {
        Long analysisId = createAnalysis(accessToken);
        markAnalysisCompleted(analysisId);

        mockMvc.perform(get(GET_ANALYSIS_URL.formatted(analysisId))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.modelVersion").value("v1"))
                .andExpect(jsonPath("$.result.overallScore").value(0.85))
                .andExpect(jsonPath("$.result.classification").value("GOOD"))
                .andExpect(jsonPath("$.result.feedback.overall_label").value("excellent"))
                .andExpect(jsonPath("$.result.rawFeatures").doesNotExist())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("GET /analysis/{id} returns 404 when the analysis belongs to another user")
    void getAnalysis_wrongOwner_returns404() throws Exception {
        Long analysisId = createAnalysis(accessToken);
        String otherUserToken = registerUser();

        mockMvc.perform(get(GET_ANALYSIS_URL.formatted(analysisId))
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /analysis/{id} returns 404 for nonexistent analysis")
    void getAnalysis_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get(GET_ANALYSIS_URL.formatted(999999L))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /analysis/{id} returns 403 when no token is provided")
    void getAnalysis_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(GET_ANALYSIS_URL.formatted(1L)))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------------------------- list analyses
    @Test
    @DisplayName("GET /analysis returns 200 with empty array when user has no analyses")
    void listAnalyses_noAnalyses_returns200WithEmptyArray() throws Exception {
        mockMvc.perform(get(LIST_ANALYSES_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /analysis returns only the authenticated user's analyses")
    void listAnalyses_multipleUsers_returnsOnlyOwnAnalyses() throws Exception {
        Long firstId = createAnalysis(accessToken);
        Long secondId = createAnalysis(accessToken);

        String otherUserToken = registerUser();
        Long otherUserAnalysisId = createAnalysis(otherUserToken);

        mockMvc.perform(get(LIST_ANALYSES_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[*].id", Matchers.containsInAnyOrder(firstId.intValue(), secondId.intValue())))
                .andExpect(jsonPath("$[*].id", Matchers.not(Matchers.hasItem(otherUserAnalysisId.intValue()))))
                .andExpect(jsonPath("$[0].exerciseType").value("SQUAT"))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    @DisplayName("GET /analysis returns 403 when no token is provided")
    void listAnalyses_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(LIST_ANALYSES_URL))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------------------------- helpers
    private String registerUser() throws Exception {
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
        return (String) body.get("accessToken");
    }

    private Long createAnalysis(String token) throws Exception {
        var body = Map.of("exerciseType", "SQUAT", "fileName", "my_squat.mp4");

        MvcResult result = mockMvc.perform(post(CREATE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) response.get("analysisId")).longValue();
    }

    private static final String TEST_BUCKET = "test-bucket";

    private Long createAnalysisAndUploadFakeVideo(byte[] content, String contentType) throws Exception {
        Long analysisId = createAnalysis(accessToken);

        VideoAnalysis analysis = videoAnalysisRepository.findById(analysisId).orElseThrow();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(TEST_BUCKET)
                        .key(analysis.getVideoS3Key())
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );

        return analysisId;
    }

    private void markAnalysisCompleted(Long analysisId) {
        var analysis = videoAnalysisRepository.findById(analysisId).orElseThrow();
        analysis.confirmUpload();
        analysis.markCompleted(AnalysisResult.score(
                analysisId,
                "v1",
                new BigDecimal("0.85"),
                Map.of("overall_label", "excellent"),
                Map.of("depth_mean", 0.42)
        ));
        videoAnalysisRepository.save(analysis);
    }
}



