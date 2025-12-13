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


    @Transactional
    public VideoResponse create(VideoRequest request, User author) {
        Video video = videoMapper.fromDTO(request);
        video.setAuthor(author);
        video.setStatus(VideoStatus.PUBLISHED);

        // Устанавливаем хэштеги, если они есть
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
        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
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

