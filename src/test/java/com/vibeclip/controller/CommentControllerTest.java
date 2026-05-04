package com.vibeclip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibeclip.dto.comment.CommentRequest;
import com.vibeclip.dto.comment.CommentResponse;
import com.vibeclip.entity.User;
import com.vibeclip.service.CommentService;
import com.vibeclip.service.JwtService;
import com.vibeclip.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@test.com")
    void createComment_success_shouldReturnCreatedComment() throws Exception {
        UUID videoId = UUID.randomUUID();
        User user = currentUser();
        CommentRequest request = CommentRequest.builder()
                .videoId(videoId)
                .text("Отличное видео")
                .build();
        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .videoId(videoId)
                .userId(user.getId())
                .username(user.getUsername())
                .text("Отличное видео")
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(commentService.create(any(CommentRequest.class), eq(user))).thenReturn(response);

        mockMvc.perform(post("/api/v1/videos/{videoId}/comments", videoId)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoId").value(videoId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.text").value("Отличное видео"));

        verify(commentService).create(any(CommentRequest.class), eq(user));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createComment_invalidRequest_shouldReturnBadRequest() throws Exception {
        CommentRequest request = new CommentRequest();

        mockMvc.perform(post("/api/v1/videos/{videoId}/comments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void getComments_withoutPagination_shouldReturnComments() throws Exception {
        UUID videoId = UUID.randomUUID();
        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .videoId(videoId)
                .username("author")
                .text("Первый")
                .build();

        when(commentService.getByVideoId(videoId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/videos/{videoId}/comments", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].videoId").value(videoId.toString()))
                .andExpect(jsonPath("$[0].text").value("Первый"));

        verify(commentService).getByVideoId(videoId);
    }

    @Test
    void getComments_withPagination_shouldReturnPageContent() throws Exception {
        UUID videoId = UUID.randomUUID();
        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .videoId(videoId)
                .text("Постраничный")
                .build();

        when(commentService.getByVideoId(eq(videoId), any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/videos/{videoId}/comments", videoId)
                        .param("paginated", "true")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Постраничный"));

        verify(commentService).getByVideoId(eq(videoId), any());
    }

    @Test
    void getComment_success_shouldReturnComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .text("Комментарий")
                .build();

        when(commentService.getById(commentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/videos/comments/{commentId}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.text").value("Комментарий"));

        verify(commentService).getById(commentId);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteComment_success_shouldReturnNoContent() throws Exception {
        UUID commentId = UUID.randomUUID();
        User user = currentUser();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(commentService).delete(commentId, user);

        mockMvc.perform(delete("/api/v1/videos/comments/{commentId}", commentId)
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(commentService).delete(commentId, user);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getMyComments_success_shouldReturnCurrentUserComments() throws Exception {
        User user = currentUser();
        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .text("Мой комментарий")
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(commentService.getByUser(user)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/videos/comments/my")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(user.getId().toString()))
                .andExpect(jsonPath("$[0].text").value("Мой комментарий"));

        verify(commentService).getByUser(user);
    }

    @Test
    void getComment_serviceThrows_shouldReturnBadRequest() throws Exception {
        UUID commentId = UUID.randomUUID();

        when(commentService.getById(commentId)).thenThrow(new IllegalArgumentException("Комментарий не найден"));

        mockMvc.perform(get("/api/v1/videos/comments/{commentId}", commentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Комментарий не найден"));
    }

    private User currentUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setUsername("testuser");
        return user;
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
