package com.vibeclip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibeclip.dto.reaction.ReactionRequest;
import com.vibeclip.dto.reaction.ReactionResponse;
import com.vibeclip.dto.video.VideoMetricsResponse;
import com.vibeclip.dto.video.VideoRequest;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.ReactionType;
import com.vibeclip.entity.User;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoService videoService;

    @MockBean
    private VideoMetricService videoMetricService;

    @MockBean
    private ReactionService reactionService;

    @MockBean
    private VideoReportService videoReportService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void create_success_shouldReturnCreatedVideo() throws Exception {
        User author = currentUser();
        VideoRequest request = VideoRequest.builder()
                .title("Первое видео")
                .description("Описание")
                .videoUrl("/uploads/video.mp4")
                .thumbnailUrl("/uploads/thumb.jpg")
                .durationSeconds(30)
                .hashtags(Set.of("music"))
                .build();
        VideoResponse response = videoResponse(UUID.randomUUID(), "Первое видео");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(author));
        when(videoService.create(any(VideoRequest.class), eq(author))).thenReturn(response);

        mockMvc.perform(post("/api/v1/videos")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.title").value("Первое видео"));

        verify(videoService).create(any(VideoRequest.class), eq(author));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void create_invalidDuration_shouldReturnBadRequest() throws Exception {
        VideoRequest request = VideoRequest.builder()
                .title("Bad")
                .durationSeconds(0)
                .build();

        mockMvc.perform(post("/api/v1/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(videoService);
    }

    @Test
    void getById_success_shouldReturnVideo() throws Exception {
        UUID videoId = UUID.randomUUID();
        VideoResponse response = videoResponse(videoId, "Публичное видео");

        when(videoService.getById(videoId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/videos/{id}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(videoId.toString()))
                .andExpect(jsonPath("$.title").value("Публичное видео"));

        verify(videoService).getById(videoId);
    }

    @Test
    void getMetrics_success_shouldReturnMetrics() throws Exception {
        UUID videoId = UUID.randomUUID();
        VideoMetricsResponse response = VideoMetricsResponse.builder()
                .viewCount(10L)
                .likeCount(3L)
                .commentCount(2L)
                .shareCount(1L)
                .build();

        when(videoMetricService.getByVideoId(videoId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/videos/{id}/metrics", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(10))
                .andExpect(jsonPath("$.likeCount").value(3));

        verify(videoMetricService).getByVideoId(videoId);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void update_success_shouldReturnUpdatedVideo() throws Exception {
        User author = currentUser();
        UUID videoId = UUID.randomUUID();
        VideoRequest request = VideoRequest.builder()
                .title("Обновлено")
                .description("Новое описание")
                .build();
        VideoResponse response = videoResponse(videoId, "Обновлено");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(author));
        when(videoService.update(eq(videoId), any(VideoRequest.class), eq(author))).thenReturn(response);

        mockMvc.perform(put("/api/v1/videos/{id}", videoId)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлено"));

        verify(videoService).update(eq(videoId), any(VideoRequest.class), eq(author));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void delete_success_shouldReturnNoContent() throws Exception {
        User author = currentUser();
        UUID videoId = UUID.randomUUID();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(author));
        doNothing().when(videoService).delete(videoId, author);

        mockMvc.perform(delete("/api/v1/videos/{id}", videoId)
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(videoService).delete(videoId, author);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void publish_success_shouldReturnPublishedVideo() throws Exception {
        User author = currentUser();
        UUID videoId = UUID.randomUUID();
        VideoResponse response = videoResponse(videoId, "Опубликовано");
        response.setStatus(VideoStatus.PUBLISHED);

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(author));
        when(videoService.publish(videoId, author)).thenReturn(response);

        mockMvc.perform(post("/api/v1/videos/{id}/publish", videoId)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(videoService).publish(videoId, author);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getMyVideos_success_shouldReturnAuthorVideos() throws Exception {
        User author = currentUser();
        VideoResponse response = videoResponse(UUID.randomUUID(), "Моё видео");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(author));
        when(videoService.getByAuthor(eq(author), eq(VideoStatus.PUBLISHED), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/videos/my")
                        .principal(authentication())
                        .param("status", "PUBLISHED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Моё видео"));

        verify(videoService).getByAuthor(eq(author), eq(VideoStatus.PUBLISHED), any());
    }

    @Test
    void getPublished_withoutRecommended_shouldReturnPublishedVideos() throws Exception {
        VideoResponse response = videoResponse(UUID.randomUUID(), "Лента");

        when(videoService.getPublished(any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/videos")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Лента"));

        verify(videoService).getPublished(any());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getPublished_recommended_shouldUseRecommendedFeed() throws Exception {
        User user = currentUser();
        VideoResponse response = videoResponse(UUID.randomUUID(), "Рекомендация");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(videoService.getRecommendedFeed(eq(user), any(), eq(0.25)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/videos")
                        .principal(authentication())
                        .param("recommended", "true")
                        .param("randomPercentage", "0.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Рекомендация"));

        verify(videoService).getRecommendedFeed(eq(user), any(), eq(0.25));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void uploadVideo_success_shouldPassMultipartDataToService() throws Exception {
        User author = currentUser();
        VideoResponse response = videoResponse(UUID.randomUUID(), "Загрузка");
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "clip.mp4",
                "video/mp4",
                "video-bytes".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnail",
                "thumb.jpg",
                "image/jpeg",
                "thumb-bytes".getBytes(StandardCharsets.UTF_8)
        );

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(author));
        when(videoService.createWithFiles(
                any(MultipartFile.class),
                any(MultipartFile.class),
                any(),
                any(),
                any(),
                any(),
                eq(author)
        )).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/videos/upload")
                        .file(videoFile)
                        .file(thumbnailFile)
                        .file(textPart("title", "Загрузка"))
                        .file(textPart("description", "Описание"))
                        .file(textPart("hashtags", "[\"#Music\", \"Sport\"]"))
                        .file(jsonNumberPart("durationSeconds", 42))
                        .principal(authentication()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Загрузка"));

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Set<String>> hashtagCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(videoService).createWithFiles(
                any(MultipartFile.class),
                any(MultipartFile.class),
                eq("Загрузка"),
                eq("Описание"),
                hashtagCaptor.capture(),
                eq(42),
                eq(author)
        );
        assertEquals(Set.of("music", "sport"), hashtagCaptor.getValue());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createReaction_success_shouldUsePathVideoId() throws Exception {
        User user = currentUser();
        UUID videoId = UUID.randomUUID();
        ReactionRequest request = ReactionRequest.builder()
                .videoId(UUID.randomUUID())
                .reactionType(ReactionType.LIKE)
                .build();
        ReactionResponse response = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .videoId(videoId)
                .reactionType(ReactionType.LIKE)
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.create(any(ReactionRequest.class), eq(user))).thenReturn(response);

        mockMvc.perform(post("/api/v1/videos/{id}/reactions", videoId)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoId").value(videoId.toString()))
                .andExpect(jsonPath("$.reactionType").value("LIKE"));

        ArgumentCaptor<ReactionRequest> requestCaptor = ArgumentCaptor.forClass(ReactionRequest.class);
        verify(reactionService).create(requestCaptor.capture(), eq(user));
        assertEquals(videoId, requestCaptor.getValue().getVideoId());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createReaction_toggleOff_shouldReturnNoContent() throws Exception {
        User user = currentUser();
        UUID videoId = UUID.randomUUID();
        ReactionRequest request = ReactionRequest.builder()
                .videoId(videoId)
                .reactionType(ReactionType.LIKE)
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.create(any(ReactionRequest.class), eq(user))).thenReturn(null);

        mockMvc.perform(post("/api/v1/videos/{id}/reactions", videoId)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createReaction_invalidRequest_shouldReturnBadRequest() throws Exception {
        ReactionRequest request = ReactionRequest.builder()
                .videoId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/videos/{id}/reactions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reactionService);
    }

    @Test
    void getById_serviceThrows_shouldReturnBadRequest() throws Exception {
        UUID videoId = UUID.randomUUID();

        when(videoService.getById(videoId)).thenThrow(new IllegalArgumentException("Видео не найдено"));

        mockMvc.perform(get("/api/v1/videos/{id}", videoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Видео не найдено"));
    }

    private MockMultipartFile textPart(String name, String value) {
        return new MockMultipartFile(
                name,
                "",
                MediaType.TEXT_PLAIN_VALUE,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile jsonNumberPart(String name, Integer value) {
        return new MockMultipartFile(
                name,
                "",
                MediaType.APPLICATION_JSON_VALUE,
                value.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private User currentUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setUsername("testuser");
        return user;
    }

    private VideoResponse videoResponse(UUID id, String title) {
        return VideoResponse.builder()
                .id(id)
                .title(title)
                .description("Описание")
                .videoUrl("/uploads/video.mp4")
                .thumbnailUrl("/uploads/thumb.jpg")
                .durationSeconds(30)
                .status(VideoStatus.PUBLISHED)
                .authorId(UUID.randomUUID())
                .authorUsername("testuser")
                .hashtags(Set.of("music"))
                .metrics(VideoMetricsResponse.builder()
                        .viewCount(1L)
                        .likeCount(2L)
                        .commentCount(3L)
                        .shareCount(4L)
                        .build())
                .build();
    }

    private Authentication authentication() {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("user@test.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
