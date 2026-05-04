package com.vibeclip.controller;

import com.vibeclip.config.SecurityConfig;
import com.vibeclip.service.JwtService;

import com.vibeclip.controller.AdminController;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.ReactionRepository;
import com.vibeclip.repository.VideoRepository;
import com.vibeclip.service.UserService;
import com.vibeclip.service.VideoService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@Import(SecurityConfig.class)
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = true)
public class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;
    @MockBean
    private VideoRepository videoRepository;
    @MockBean
    private VideoMapper videoMapper;
    @MockBean
    private ReactionRepository reactionRepository;
    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getVideosForModeration_defaultStatus_shouldReturnPage() throws Exception {
        Video video = new Video();
        video.setId(UUID.randomUUID());
        video.setStatus(VideoStatus.PENDING);

        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setStatus(video.getStatus());

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByStatus(eq(VideoStatus.PENDING), any(Pageable.class))).thenReturn(videoPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(get("/api/v1/admin/moderation/videos")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(videoRepository).findByStatus(eq(VideoStatus.PENDING), any(Pageable.class));
        verify(videoMapper).toDTO(video);
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getVideosForModeration_withStatus_shouldUseProvidedStatus() throws Exception {
        Page<Video> videoPage = new PageImpl<>(List.of());

        when(videoRepository.findByStatus(eq(VideoStatus.PUBLISHED), any(Pageable.class))).thenReturn(videoPage);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(get("/api/v1/admin/moderation/videos")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk());

        verify(videoRepository).findByStatus(eq(VideoStatus.PUBLISHED), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getVideosForModeration_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation/videos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void approveVideo_success_shouldPublishVideo() throws Exception {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");

        Video video = new Video();
        video.setId(videoId);
        video.setStatus(VideoStatus.PENDING);

        VideoResponse response = new VideoResponse();
        response.setStatus(video.getStatus());
        response.setId(videoId);

        when(videoService.getEntityById(videoId)).thenReturn(video);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(videoMapper.toDTO(any(Video.class))).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            VideoResponse r = new VideoResponse();
            r.setId(v.getId());
            r.setStatus(v.getStatus());
            return r;
        });

        mockMvc.perform(post("/api/v1/admin/moderation/videos/{id}/approve", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertEquals(VideoStatus.PUBLISHED, video.getStatus());

        verify(videoRepository).save(video);
        verify(videoService).getEntityById(videoId);
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void approveVideo_notPending_shouldThrowException() throws Exception {
        UUID videoId = UUID.randomUUID();

        Video video = new Video();
        video.setId(videoId);
        video.setStatus(VideoStatus.PUBLISHED);

        when(videoService.getEntityById(videoId)).thenReturn(video);
        when(userService.findByEmail(any())).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/api/v1/admin/moderation/videos/{id}/approve", videoId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void approveVideo_forbidden() throws Exception {
        UUID videoId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/moderation/videos/{id}/approve", videoId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void rejectVideo_success_shouldRejectVideo() throws Exception {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");

        Video video = new Video();
        video.setId(videoId);
        video.setStatus(VideoStatus.PENDING);

        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setStatus(video.getStatus());

        when(videoService.getEntityById(videoId)).thenReturn(video);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(videoMapper.toDTO(any(Video.class))).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            VideoResponse r = new VideoResponse();
            r.setId(v.getId());
            r.setStatus(v.getStatus());
            return r;
        });

        mockMvc.perform(post("/api/v1/admin/moderation/videos/{id}/reject", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertEquals(VideoStatus.REJECTED, video.getStatus());

        verify(videoRepository).save(video);
        verify(videoMapper).toDTO(video);
        verify(videoService).getEntityById(videoId);
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void rejectVideo_notPending_shouldThrowException() throws Exception {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");

        Video video = new Video();
        video.setId(videoId);
        video.setStatus(VideoStatus.PUBLISHED);

        when(videoService.getEntityById(videoId)).thenReturn(video);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/admin/moderation/videos/{id}/reject", videoId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalStateException));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectVideo_forbidden() throws Exception {
        UUID videoId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/moderation/videos/{id}/reject", videoId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getReportedVideos_shouldReturnPage() throws Exception {
        Video video = new Video();
        video.setId(UUID.randomUUID());
        video.setStatus(VideoStatus.PUBLISHED);

        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setStatus(video.getStatus());

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByStatus(eq(VideoStatus.PUBLISHED), any(Pageable.class))).thenReturn(videoPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(get("/api/v1/admin/moderation/videos/reported")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(videoRepository).findByStatus(eq(VideoStatus.PUBLISHED), any(Pageable.class));
        verify(videoMapper).toDTO(video);
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getReportedVideos_shouldUsePageable() throws Exception {
        Page<Video> videoPage = new PageImpl<>(List.of());

        when(videoRepository.findByStatus(eq(VideoStatus.PUBLISHED), any(Pageable.class))).thenReturn(videoPage);
        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(get("/api/v1/admin/moderation/videos/reported")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(videoRepository).findByStatus(eq(VideoStatus.PUBLISHED), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getReportedVideos_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation/videos/reported"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = "ROLE_ADMIN")
    void deleteVideo_shouldReturnNoContent() throws Exception {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");

        when(userService.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        doNothing().when(videoService).deleteByAdmin(videoId);

        mockMvc.perform(delete("/api/v1/admin/videos/{id}", videoId))
                .andExpect(status().isNoContent());

        verify(videoService).deleteByAdmin(videoId);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void deleteVideo_forbidden() throws Exception {
        UUID videoId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/videos/{id}", videoId))
                .andExpect(status().isForbidden());
    }
}