package com.vibeclip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibeclip.dto.reaction.ReactionRequest;
import com.vibeclip.dto.reaction.ReactionResponse;
import com.vibeclip.entity.ReactionType;
import com.vibeclip.entity.User;
import com.vibeclip.service.JwtService;
import com.vibeclip.service.ReactionService;
import com.vibeclip.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(ReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReactionService reactionService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@test.com")
    void create_success_shouldReturnCreatedReaction() throws Exception {
        User user = currentUser();
        UUID videoId = UUID.randomUUID();
        ReactionRequest request = ReactionRequest.builder()
                .videoId(videoId)
                .reactionType(ReactionType.LIKE)
                .build();
        ReactionResponse response = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .videoId(videoId)
                .reactionType(ReactionType.LIKE)
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.create(any(ReactionRequest.class), eq(user))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reactions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoId").value(videoId.toString()))
                .andExpect(jsonPath("$.reactionType").value("LIKE"));

        verify(reactionService).create(any(ReactionRequest.class), eq(user));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void create_likeToggleOff_shouldReturnNoContent() throws Exception {
        User user = currentUser();
        ReactionRequest request = ReactionRequest.builder()
                .videoId(UUID.randomUUID())
                .reactionType(ReactionType.LIKE)
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.create(any(ReactionRequest.class), eq(user))).thenReturn(null);

        mockMvc.perform(post("/api/v1/reactions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void create_invalidRequest_shouldReturnBadRequest() throws Exception {
        ReactionRequest request = new ReactionRequest();

        mockMvc.perform(post("/api/v1/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reactionService);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void delete_success_shouldReturnNoContent() throws Exception {
        User user = currentUser();
        UUID videoId = UUID.randomUUID();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(reactionService).delete(videoId, ReactionType.LIKE, user);

        mockMvc.perform(delete("/api/v1/reactions/video/{videoId}", videoId)
                        .principal(authentication())
                        .param("reactionType", "LIKE"))
                .andExpect(status().isNoContent());

        verify(reactionService).delete(videoId, ReactionType.LIKE, user);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getByVideo_success_shouldReturnUserReactionsForVideo() throws Exception {
        User user = currentUser();
        UUID videoId = UUID.randomUUID();
        ReactionResponse response = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .videoId(videoId)
                .reactionType(ReactionType.SHARE)
                .shareUrl("/api/v1/videos/" + videoId)
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.getByUserAndVideo(user, videoId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reactions/video/{videoId}", videoId)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reactionType").value("SHARE"))
                .andExpect(jsonPath("$[0].shareUrl").value("/api/v1/videos/" + videoId));

        verify(reactionService).getByUserAndVideo(user, videoId);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void hasReaction_success_shouldReturnBoolean() throws Exception {
        User user = currentUser();
        UUID videoId = UUID.randomUUID();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.hasReaction(user, videoId, ReactionType.LIKE)).thenReturn(true);

        mockMvc.perform(get("/api/v1/reactions/video/{videoId}/check", videoId)
                        .principal(authentication())
                        .param("reactionType", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(reactionService).hasReaction(user, videoId, ReactionType.LIKE);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getMyReactions_success_shouldReturnReactionsByType() throws Exception {
        User user = currentUser();
        ReactionResponse response = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .reactionType(ReactionType.LIKE)
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reactionService.getByUser(user, ReactionType.LIKE)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reactions/my")
                        .principal(authentication())
                        .param("reactionType", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reactionType").value("LIKE"));

        verify(reactionService).getByUser(user, ReactionType.LIKE);
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
