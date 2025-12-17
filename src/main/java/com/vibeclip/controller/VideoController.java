package com.vibeclip.controller;

import com.vibeclip.dto.reaction.ReactionRequest;
import com.vibeclip.dto.reaction.ReactionResponse;
import com.vibeclip.dto.video.VideoRequest;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.dto.video.VideoMetricsResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.service.ReactionService;
import com.vibeclip.service.UserService;
import com.vibeclip.service.VideoMetricService;
import com.vibeclip.service.VideoService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController extends BaseController {

    private final VideoService videoService;
    private final VideoMetricService videoMetricService;
    private final ReactionService reactionService;

    public VideoController(
            UserService userService,
            VideoService videoService,
            VideoMetricService videoMetricService,
            ReactionService reactionService
    ) {
        super(userService);
        this.videoService = videoService;
        this.videoMetricService = videoMetricService;
        this.reactionService = reactionService;
    }

    // Создание нового видео
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<VideoResponse> create(@Valid @RequestBody VideoRequest request, Authentication authentication) {
        User author = getCurrentUser(authentication);
        VideoResponse response = videoService.create(request, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Получение видео по идентификатору
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getById(@PathVariable UUID id) {
        VideoResponse response = videoService.getById(id);
        return ResponseEntity.ok(response);
    }

    // Получение метрик видео (просмотры, лайки, комментарии, репосты)
    @GetMapping("/{id}/metrics")
    public ResponseEntity<VideoMetricsResponse> getMetrics(@PathVariable UUID id) {
        VideoMetricsResponse response = videoMetricService.getByVideoId(id);
        return ResponseEntity.ok(response);
    }

    // Обновление информации о видео (только свое видео)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<VideoResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody VideoRequest request,
            Authentication authentication
    ) {
        User author = getCurrentUser(authentication);
        VideoResponse response = videoService.update(id, request, author);
        return ResponseEntity.ok(response);
    }

    // Удаление видео (полное удаление из БД и файлов, только свое видео)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        User author = getCurrentUser(authentication);
        videoService.delete(id, author);
        return ResponseEntity.noContent().build();
    }

    // Публикация видео (изменение статуса на PUBLISHED, только свое видео)
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<VideoResponse> publish(@PathVariable UUID id, Authentication authentication) {
        User author = getCurrentUser(authentication);
        VideoResponse response = videoService.publish(id, author);
        return ResponseEntity.ok(response);
    }

    // Получение списка видео текущего пользователя с фильтрацией по статусу
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<VideoResponse>> getMyVideos(
            @RequestParam(required = false) VideoStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        User author = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponse> response = videoService.getByAuthor(author, status, pageable);
        return ResponseEntity.ok(response);
    }

    // Получение списка опубликованных видео с пагинацией
    // Если передан параметр recommended=true и пользователь авторизован, возвращается рекомендованная лента
    @GetMapping
    public ResponseEntity<Page<VideoResponse>> getPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean recommended,
            @RequestParam(required = false) Double randomPercentage,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponse> response;
        
        // Если запрошена рекомендованная лента и пользователь авторизован
        if (Boolean.TRUE.equals(recommended) && authentication != null) {
            User user = getCurrentUser(authentication);
            response = videoService.getRecommendedFeed(user, pageable, randomPercentage);
        } else {
            // Обычная лента без рекомендаций
            response = videoService.getPublished(pageable);
        }
        
        return ResponseEntity.ok(response);
    }

    // Загрузка видео с файлами
    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestPart("file") MultipartFile videoFile,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "hashtags", required = false) String hashtags,
            @RequestPart("durationSeconds") Integer durationSeconds,
            Authentication authentication
    ) {
        User author = getCurrentUser(authentication);

        // Парсим и нормализуем хэштеги из строки (если переданы как строка через @RequestPart)
        java.util.Set<String> hashtagSet = null;
        if (hashtags != null && !hashtags.trim().isEmpty()) {
            String trimmed = hashtags.trim();
            // Если строка начинается с [ и заканчивается ], это JSON массив - парсим как массив
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                // Удаляем квадратные скобки и парсим как JSON массив
                String arrayContent = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!arrayContent.isEmpty()) {
                    // Парсим элементы массива, учитывая что они могут быть в кавычках
                    hashtagSet = java.util.Arrays.stream(arrayContent.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            // Агрессивно удаляем все кавычки (одинарные и двойные) в начале и конце
                            .map(s -> {
                                // Удаляем все кавычки в начале
                                while (!s.isEmpty() && (s.startsWith("\"") || s.startsWith("'"))) {
                                    s = s.substring(1).trim();
                                }
                                // Удаляем все кавычки в конце
                                while (!s.isEmpty() && (s.endsWith("\"") || s.endsWith("'"))) {
                                    s = s.substring(0, s.length() - 1).trim();
                                }
                                return s;
                            })
                            .map(com.vibeclip.util.HashtagUtil::normalize)
                            .filter(h -> h != null && !h.isEmpty())
                            .collect(java.util.stream.Collectors.toSet());
                }
            } else {
                // Обычный формат через запятую
                hashtagSet = java.util.Arrays.stream(trimmed.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        // Агрессивно удаляем все кавычки перед нормализацией
                        .map(s -> {
                            // Удаляем все кавычки в начале
                            while (!s.isEmpty() && (s.startsWith("\"") || s.startsWith("'"))) {
                                s = s.substring(1).trim();
                            }
                            // Удаляем все кавычки в конце
                            while (!s.isEmpty() && (s.endsWith("\"") || s.endsWith("'"))) {
                                s = s.substring(0, s.length() - 1).trim();
                            }
                            return s;
                        })
                        .map(com.vibeclip.util.HashtagUtil::normalize)
                        .filter(h -> h != null && !h.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());
            }
        }

        VideoResponse response = videoService.createWithFiles(
                videoFile,
                thumbnailFile,
                title,
                description,
                hashtagSet,
                durationSeconds,
                author
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Создание реакции на видео (лайк, просмотр, репорт и т.д.)
    // Для LIKE реакций работает toggle: если лайк уже есть, он удаляется (возвращается null)
    @PostMapping("/{id}/reactions")
    public ResponseEntity<ReactionResponse> createReaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReactionRequest request,
            Authentication authentication
    ) {
        // Устанавливаем videoId из пути (приоритет пути над телом запроса)
        request.setVideoId(id);
        
        User user = getCurrentUser(authentication);
        ReactionResponse response = reactionService.create(request, user);
        
        // Если реакция была удалена (toggle для LIKE), возвращаем 204 No Content
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

