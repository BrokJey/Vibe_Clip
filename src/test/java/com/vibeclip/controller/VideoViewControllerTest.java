package com.vibeclip.controller;

import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.service.JwtService;
import com.vibeclip.service.UserService;
import com.vibeclip.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class VideoViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @Test
    void viewVideo_success_shouldReturnVideo() throws Exception {
        UUID videoId = UUID.randomUUID();
        VideoResponse response = VideoResponse.builder()
                .id(videoId)
                .title("Deep link video")
                .build();

        when(videoService.getById(videoId)).thenReturn(response);

        mockMvc.perform(get("/v/{videoId}", videoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(videoId.toString()))
                .andExpect(jsonPath("$.title").value("Deep link video"));

        verify(videoService).getById(videoId);
    }

    @Test
    void viewVideo_notFound_shouldReturnBadRequest() throws Exception {
        UUID videoId = UUID.randomUUID();

        when(videoService.getById(videoId)).thenThrow(new IllegalArgumentException("Видео не найдено"));

        mockMvc.perform(get("/v/{videoId}", videoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Видео не найдено"));
    }
}
