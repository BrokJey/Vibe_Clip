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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoMetricRepository videoMetricRepository;
    private final VideoMapper videoMapper;


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
        return videoMapper.toDTO(saved);
    }

    public VideoResponse getById(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено: " + id));
        return videoMapper.toDTO(video);
    }

    public VideoResponse getByIdAndAuthor(UUID id, User author) {
        Video video = videoRepository.findByIdAndAuthorId(id, author.getId())
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено или вы не автор"));
        return videoMapper.toDTO(video);
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
        return videoMapper.toDTO(updated);
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
        return videoMapper.toDTO(updated);
    }

    public Page<VideoResponse> getByAuthor(User author, VideoStatus status, Pageable pageable) {
        return videoRepository.findByAuthorAndStatus(author, status, pageable)
                .map(videoMapper::toDTO);
    }

    public Page<VideoResponse> getPublished(Pageable pageable) {
        return videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable)
                .map(videoMapper::toDTO);
    }

    public Video getEntityById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено: " + id));
    }
}

