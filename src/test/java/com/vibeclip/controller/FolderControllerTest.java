package com.vibeclip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibeclip.dto.folder.FolderFeedResponse;
import com.vibeclip.dto.folder.FolderRequest;
import com.vibeclip.dto.folder.FolderResponse;
import com.vibeclip.dto.folder.FolderVideoResponse;
import com.vibeclip.dto.folder.preference.FolderPreferenceRequest;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderStatus;
import com.vibeclip.entity.FolderVideo;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.FolderVideoMapper;
import com.vibeclip.service.FolderService;
import com.vibeclip.service.JwtService;
import com.vibeclip.service.RecommendationService;
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
import java.util.Set;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@AutoConfigureMockMvc(addFilters = false)
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FolderService folderService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private FolderVideoMapper folderVideoMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "user@test.com")
    void create_success_shouldReturnCreatedFolder() throws Exception {
        User owner = currentUser();
        FolderRequest request = FolderRequest.builder()
                .name("Музыка")
                .description("Клипы")
                .preference(FolderPreferenceRequest.builder()
                        .allowedHashtags(Set.of("music"))
                        .build())
                .build();
        FolderResponse response = folderResponse(UUID.randomUUID(), "Музыка");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(folderService.create(any(FolderRequest.class), eq(owner))).thenReturn(response);

        mockMvc.perform(post("/api/v1/folders")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.name").value("Музыка"));

        verify(folderService).create(any(FolderRequest.class), eq(owner));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void create_invalidName_shouldReturnBadRequest() throws Exception {
        FolderRequest request = FolderRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(folderService);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getMyFolders_success_shouldReturnFolders() throws Exception {
        User owner = currentUser();
        FolderResponse response = folderResponse(UUID.randomUUID(), "Избранное");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(folderService.getByOwner(owner)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/folders")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Избранное"));

        verify(folderService).getByOwner(owner);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getById_success_shouldReturnFolder() throws Exception {
        User owner = currentUser();
        UUID folderId = UUID.randomUUID();
        FolderResponse response = folderResponse(folderId, "Спорт");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(folderService.getById(folderId, owner)).thenReturn(response);

        mockMvc.perform(get("/api/v1/folders/{id}", folderId)
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folderId.toString()))
                .andExpect(jsonPath("$.name").value("Спорт"));

        verify(folderService).getById(folderId, owner);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void update_success_shouldReturnUpdatedFolder() throws Exception {
        User owner = currentUser();
        UUID folderId = UUID.randomUUID();
        FolderRequest request = FolderRequest.builder()
                .name("Новые клипы")
                .description("Обновлено")
                .build();
        FolderResponse response = folderResponse(folderId, "Новые клипы");

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(folderService.update(eq(folderId), any(FolderRequest.class), eq(owner))).thenReturn(response);

        mockMvc.perform(put("/api/v1/folders/{id}", folderId)
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новые клипы"));

        verify(folderService).update(eq(folderId), any(FolderRequest.class), eq(owner));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void delete_success_shouldReturnNoContent() throws Exception {
        User owner = currentUser();
        UUID folderId = UUID.randomUUID();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        doNothing().when(folderService).delete(folderId, owner);

        mockMvc.perform(delete("/api/v1/folders/{id}", folderId)
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(folderService).delete(folderId, owner);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void archive_success_shouldReturnNoContent() throws Exception {
        User owner = currentUser();
        UUID folderId = UUID.randomUUID();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        doNothing().when(folderService).archive(folderId, owner);

        mockMvc.perform(post("/api/v1/folders/{id}/archive", folderId)
                        .principal(authentication()))
                .andExpect(status().isNoContent());

        verify(folderService).archive(folderId, owner);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getFeed_success_shouldReturnFolderFeed() throws Exception {
        User owner = currentUser();
        Folder folder = folderEntity("Лента");
        FolderVideo folderVideo = FolderVideo.builder()
                .id(UUID.randomUUID())
                .folder(folder)
                .score(0.9)
                .position(1)
                .shown(false)
                .build();
        FolderVideoResponse videoResponse = FolderVideoResponse.builder()
                .id(folderVideo.getId())
                .score(0.9)
                .position(1)
                .shown(false)
                .video(VideoResponse.builder().id(UUID.randomUUID()).title("Видео").build())
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(folderService.getEntityById(folder.getId(), owner)).thenReturn(folder);
        when(recommendationService.getFeedForFolder(folder, 10)).thenReturn(List.of(folderVideo));
        when(folderVideoMapper.toDTO(folderVideo)).thenReturn(videoResponse);

        mockMvc.perform(get("/api/v1/folders/{id}/feed", folder.getId())
                        .principal(authentication())
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folder.getId().toString()))
                .andExpect(jsonPath("$.folderName").value("Лента"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.videos[0].score").value(0.9));

        verify(recommendationService).getFeedForFolder(folder, 10);
        verify(folderVideoMapper).toDTO(folderVideo);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void regenerateFeed_success_shouldGenerateAndReturnFeed() throws Exception {
        User owner = currentUser();
        Folder folder = folderEntity("Перегенерация");
        FolderVideo folderVideo = FolderVideo.builder()
                .id(UUID.randomUUID())
                .folder(folder)
                .score(1.0)
                .position(0)
                .shown(false)
                .build();
        FolderVideoResponse videoResponse = FolderVideoResponse.builder()
                .id(folderVideo.getId())
                .score(1.0)
                .position(0)
                .shown(false)
                .video(VideoResponse.builder().id(UUID.randomUUID()).title("Новое").build())
                .build();

        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(folderService.getEntityById(folder.getId(), owner)).thenReturn(folder);
        when(recommendationService.generateFeedForFolder(folder, 5)).thenReturn(List.of(folderVideo));
        when(recommendationService.getFeedForFolder(folder, 5)).thenReturn(List.of(folderVideo));
        when(folderVideoMapper.toDTO(folderVideo)).thenReturn(videoResponse);

        mockMvc.perform(post("/api/v1/folders/{id}/regenerate", folder.getId())
                        .principal(authentication())
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderName").value("Перегенерация"))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.videos[0].video.title").value("Новое"));

        verify(recommendationService).generateFeedForFolder(folder, 5);
        verify(recommendationService).getFeedForFolder(folder, 5);
    }

    private User currentUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setUsername("testuser");
        return user;
    }

    private FolderResponse folderResponse(UUID id, String name) {
        return FolderResponse.builder()
                .id(id)
                .name(name)
                .status(FolderStatus.ACTIVE)
                .ownerId(UUID.randomUUID())
                .ownerUsername("testuser")
                .videoCount(0)
                .build();
    }

    private Folder folderEntity(String name) {
        Folder folder = new Folder();
        folder.setId(UUID.randomUUID());
        folder.setName(name);
        folder.setStatus(FolderStatus.ACTIVE);
        return folder;
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
