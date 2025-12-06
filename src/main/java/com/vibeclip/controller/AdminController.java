package com.vibeclip.controller;

import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.ReactionRepository;
import com.vibeclip.repository.VideoRepository;
import com.vibeclip.service.UserService;
import com.vibeclip.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends BaseController {

    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final ReactionRepository reactionRepository;

    public AdminController(
            UserService userService,
            VideoService videoService,
            VideoRepository videoRepository,
            VideoMapper videoMapper,
            ReactionRepository reactionRepository
    ) {
        super(userService);
        this.videoService = videoService;
        this.videoRepository = videoRepository;
        this.videoMapper = videoMapper;
        this.reactionRepository = reactionRepository;
    }

    // Получение списка видео на модерации с пагинацией
    @GetMapping("/moderation/videos")
    public ResponseEntity<Page<VideoResponse>> getVideosForModeration(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) VideoStatus status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        VideoStatus moderationStatus = status != null ? status : VideoStatus.PENDING;
        
        Page<Video> videos = videoRepository.findByStatus(moderationStatus, pageable);
        Page<VideoResponse> response = videos.map(videoMapper::toDTO);
        
        return ResponseEntity.ok(response);
    }

    // Одобрение видео для публикации
    @PostMapping("/moderation/videos/{id}/approve")
    public ResponseEntity<VideoResponse> approveVideo(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User admin = getCurrentUser(authentication);
        Video video = videoService.getEntityById(id);
        
        if (video.getStatus() != VideoStatus.PENDING) {
            throw new IllegalStateException("Видео не находится на модерации");
        }
        
        video.setStatus(VideoStatus.PUBLISHED);
        videoRepository.save(video);
        
        VideoResponse response = videoMapper.toDTO(video);
        return ResponseEntity.ok(response);
    }

    // Отклонение видео модератором
    @PostMapping("/moderation/videos/{id}/reject")
    public ResponseEntity<VideoResponse> rejectVideo(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User admin = getCurrentUser(authentication);
        Video video = videoService.getEntityById(id);
        
        if (video.getStatus() != VideoStatus.PENDING) {
            throw new IllegalStateException("Видео не находится на модерации");
        }
        
        video.setStatus(VideoStatus.REJECTED);
        videoRepository.save(video);
        
        VideoResponse response = videoMapper.toDTO(video);
        return ResponseEntity.ok(response);
    }

    // Получение видео с репортами (видео, на которые пожаловались пользователи)
    @GetMapping("/moderation/videos/reported")
    public ResponseEntity<Page<VideoResponse>> getReportedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Находим видео, на которые есть реакции типа REPORT
        // Это упрощенная версия - в реальности нужен более сложный запрос
        Page<Video> videos = videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable);
        Page<VideoResponse> response = videos.map(videoMapper::toDTO);
        
        return ResponseEntity.ok(response);
    }

    // Удаление видео администратором (без проверки авторства)
    @DeleteMapping("/videos/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) {
        Video video = videoService.getEntityById(id);
        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
        return ResponseEntity.noContent().build();
    }
}

