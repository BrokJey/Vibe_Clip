package com.vibeclip.service;

import com.vibeclip.dto.video.VideoRequest;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoMetric;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.VideoMetricRepository;
import com.vibeclip.repository.VideoRepository;
import com.vibeclip.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoMetricRepository videoMetricRepository;
    private final VideoMapper videoMapper;
    private final FileStorageService fileStorageService;
    private final VideoMetricService videoMetricService;
    private final RecommendationService recommendationService;
    private final com.vibeclip.repository.CommentRepository commentRepository;
    private final com.vibeclip.repository.ReactionRepository reactionRepository;
    private final com.vibeclip.repository.FolderVideoRepository folderVideoRepository;


    @Transactional
    public VideoResponse create(VideoRequest request, User author) {
        Video video = videoMapper.fromDTO(request);
        video.setAuthor(author);
        video.setStatus(VideoStatus.PUBLISHED);

        // Устанавливаем хэштеги, если они есть (нормализация происходит в addHashtag)
        if (request.getHashtags() != null) {
            request.getHashtags().forEach(video::addHashtag);
        }

        Video saved = videoRepository.save(video);

        // Создаем метрики для видео
        VideoMetric metric = VideoMetric.builder()
                .video(saved)
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .build();
        videoMetricRepository.save(metric);

        log.info("Создано видео {} ", request.getTitle());
        VideoResponse response = videoMapper.toDTO(saved);
        // Загружаем метрики и устанавливаем их в ответ
        try {
            response.setMetrics(videoMetricService.getByVideoId(saved.getId()));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метрики для нового видео {}: {}", saved.getId(), e.getMessage());
            response.setMetrics(null);
        }
        return response;
    }

    public VideoResponse getById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено: " + id));
        VideoResponse response = videoMapper.toDTO(video);
        // Загружаем метрики и устанавливаем их в ответ
        try {
            response.setMetrics(videoMetricService.getByVideoId(id));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метрики для видео {}: {}", id, e.getMessage());
            response.setMetrics(null);
        }
        return response;
    }

    public VideoResponse getByIdAndAuthor(UUID id, User author) {
        Video video = videoRepository.findByIdAndAuthorId(id, author.getId())
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено или вы не автор"));
        VideoResponse response = videoMapper.toDTO(video);
        // Загружаем метрики и устанавливаем их в ответ
        try {
            response.setMetrics(videoMetricService.getByVideoId(id));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метрики для видео {}: {}", id, e.getMessage());
            response.setMetrics(null);
        }
        return response;
    }

    @Transactional
    public VideoResponse update(UUID id, VideoRequest request, User author) {
        Video video = videoRepository.findByIdAndAuthorId(id, author.getId())
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено или вы не автор"));

        // Обновляем только переданные поля
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getThumbnailUrl() != null) {
            video.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getHashtags() != null) {
            video.getHashtags().clear();
            // Нормализация происходит в addHashtag
            request.getHashtags().forEach(video::addHashtag);
        }

        Video updated = videoRepository.save(video);
        VideoResponse response = videoMapper.toDTO(updated);
        // Загружаем метрики и устанавливаем их в ответ
        try {
            response.setMetrics(videoMetricService.getByVideoId(id));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метрики для видео {}: {}", id, e.getMessage());
            response.setMetrics(null);
        }
        return response;
    }

    @Transactional
    public void delete(UUID id, User author) {
        Video video = videoRepository.findByIdAndAuthorId(id, author.getId())
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено или вы не автор"));
        deleteVideoCompletely(video);
    }

    @Transactional
    public void deleteByAdmin(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено: " + id));
        deleteVideoCompletely(video);
    }

    private void deleteVideoCompletely(Video video) {
        log.info("Начинается удаление видео {} ({})", video.getId(), video.getTitle());
        
        // 1. Удаляем метрики
        try {
            videoMetricRepository.deleteByVideoId(video.getId());
            log.debug("Метрики видео {} удалены", video.getId());
        } catch (Exception e) {
            log.warn("Не удалось удалить метрики видео {}: {}", video.getId(), e.getMessage());
        }
        
        // 2. Удаляем комментарии
        try {
            commentRepository.deleteByVideo(video);
            log.debug("Комментарии к видео {} удалены", video.getId());
        } catch (Exception e) {
            log.warn("Не удалось удалить комментарии к видео {}: {}", video.getId(), e.getMessage());
        }
        
        // 3. Удаляем реакции
        try {
            reactionRepository.deleteByVideo(video);
            log.debug("Реакции на видео {} удалены", video.getId());
        } catch (Exception e) {
            log.warn("Не удалось удалить реакции на видео {}: {}", video.getId(), e.getMessage());
        }
        
        // 4. Удаляем связи с папками
        try {
            folderVideoRepository.deleteByVideo(video);
            log.debug("Связи видео {} с папками удалены", video.getId());
        } catch (Exception e) {
            log.warn("Не удалось удалить связи видео {} с папками: {}", video.getId(), e.getMessage());
        }
        
        // 5. Удаляем файлы (видео и превью)
        try {
            if (video.getVideoUrl() != null) {
                fileStorageService.deleteFile(video.getVideoUrl());
                log.debug("Файл видео {} удален: {}", video.getId(), video.getVideoUrl());
            }
            if (video.getThumbnailUrl() != null) {
                fileStorageService.deleteFile(video.getThumbnailUrl());
                log.debug("Превью видео {} удалено: {}", video.getId(), video.getThumbnailUrl());
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении файлов видео {}: {}", video.getId(), e.getMessage());
        }
        
        // 6. Удаляем само видео из БД
        videoRepository.delete(video);
        log.info("Видео {} ({}) полностью удалено", video.getId(), video.getTitle());
    }

    @Transactional
    public VideoResponse publish(UUID id, User author) {
        Video video = videoRepository.findByIdAndAuthorId(id, author.getId())
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено или вы не автор"));
        video.setStatus(VideoStatus.PUBLISHED);
        Video updated = videoRepository.save(video);
        VideoResponse response = videoMapper.toDTO(updated);
        // Загружаем метрики и устанавливаем их в ответ
        try {
            response.setMetrics(videoMetricService.getByVideoId(id));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метрики для видео {}: {}", id, e.getMessage());
            response.setMetrics(null);
        }
        return response;
    }

    public Page<VideoResponse> getByAuthor(User author, VideoStatus status, Pageable pageable) {
        return videoRepository.findByAuthorAndStatus(author, status, pageable)
                .map(video -> {
                    VideoResponse response = videoMapper.toDTO(video);
                    // Загружаем метрики для каждого видео
                    try {
                        response.setMetrics(videoMetricService.getByVideoId(video.getId()));
                    } catch (Exception e) {
                        log.warn("Не удалось загрузить метрики для видео {}: {}", video.getId(), e.getMessage());
                        response.setMetrics(null);
                    }
                    return response;
                });
    }

    public Page<VideoResponse> getPublished(Pageable pageable) {
        return videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable)
                .map(video -> {
                    VideoResponse response = videoMapper.toDTO(video);
                    // Загружаем метрики для каждого видео
                    try {
                        response.setMetrics(videoMetricService.getByVideoId(video.getId()));
                    } catch (Exception e) {
                        log.warn("Не удалось загрузить метрики для видео {}: {}", video.getId(), e.getMessage());
                        response.setMetrics(null);
                    }
                    return response;
                });
    }

    /**
     * Получает рекомендованную ленту для пользователя на основе его лайков
     * Если пользователь не авторизован или у него нет лайков, возвращает обычную ленту
     */
    public Page<VideoResponse> getRecommendedFeed(User user, Pageable pageable, Double randomPercentage) {
        // Получаем рекомендованную ленту
        Page<Video> recommendedVideos = recommendationService.getRecommendedFeed(user, pageable, randomPercentage);
        
        // Преобразуем в VideoResponse с метриками
        return recommendedVideos.map(video -> {
            VideoResponse response = videoMapper.toDTO(video);
            // Загружаем метрики для каждого видео
            try {
                response.setMetrics(videoMetricService.getByVideoId(video.getId()));
            } catch (Exception e) {
                log.warn("Не удалось загрузить метрики для видео {}: {}", video.getId(), e.getMessage());
                response.setMetrics(null);
            }
            return response;
        });
    }

    public Video getEntityById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено: " + id));
    }

    @Transactional
    public VideoResponse createWithFiles(
            MultipartFile videoFile,
            MultipartFile thumbnailFile,
            String title,
            String description,
            Set<String> hashtags,
            Integer durationSeconds,
            User author
    ) {
        // Сохраняем видеофайл
        String videoUrl = fileStorageService.storeFile(videoFile, "video");
        
        // Обрабатываем превью
        String thumbnailUrl;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            // Если превью передано, сохраняем его
            thumbnailUrl = fileStorageService.storeFile(thumbnailFile, "thumb");
        } else {
            // Если превью не передано, извлекаем первый кадр из видео
            try {
                java.nio.file.Path videoPath = fileStorageService.getFilePath(videoUrl);
                thumbnailUrl = fileStorageService.extractThumbnailFromVideo(videoPath);
                
                if (thumbnailUrl == null) {
                    log.warn("Не удалось извлечь превью из видео, используем null");
                    thumbnailUrl = null; // Превью будет null, фронтенд может использовать первый кадр видео
                }
            } catch (Exception e) {
                log.error("Ошибка при извлечении превью из видео", e);
                thumbnailUrl = null;
            }
        }

        // Создаем видео
        Video video = Video.builder()
                .title(title != null ? title : "Без названия")
                .description(description)
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .durationSeconds(durationSeconds != null ? durationSeconds : 0)
                .author(author)
                .status(VideoStatus.PUBLISHED)
                .hashtags(new java.util.HashSet<>())
                .build();

        // Добавляем хэштеги
        if (hashtags != null && !hashtags.isEmpty()) {
            hashtags.forEach(video::addHashtag);
        }

        Video saved = videoRepository.save(video);

        // Создаем метрики для видео
        VideoMetric metric = VideoMetric.builder()
                .video(saved)
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .build();
        videoMetricRepository.save(metric);

        log.info("Создано видео с файлами: {}", saved.getTitle());
        VideoResponse response = videoMapper.toDTO(saved);
        // Загружаем метрики и устанавливаем их в ответ
        try {
            response.setMetrics(videoMetricService.getByVideoId(saved.getId()));
        } catch (Exception e) {
            log.warn("Не удалось загрузить метрики для нового видео {}: {}", saved.getId(), e.getMessage());
            response.setMetrics(null);
        }
        return response;
    }
}