package com.vibeclip.controller;

import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Контроллер для публичного просмотра видео по короткой ссылке
 * Используется для Deep Links (Android App Links)
 */
@RestController
@RequestMapping("/v")
@RequiredArgsConstructor
public class VideoViewController {

    private final VideoService videoService;

    /**
     * Публичный endpoint для просмотра видео по короткой ссылке
     * Используется для Deep Links: http://192.168.1.18:8000/v/{videoId}
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResponse> viewVideo(@PathVariable UUID videoId) {
        VideoResponse response = videoService.getById(videoId);
        return ResponseEntity.ok(response);
    }
}
