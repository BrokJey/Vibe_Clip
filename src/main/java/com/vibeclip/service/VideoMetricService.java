package com.vibeclip.service;

import com.vibeclip.dto.video.VideoMetricsResponse;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoMetric;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.VideoMetricRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VideoMetricService {

    private final VideoMetricRepository videoMetricRepository;
    private final VideoMapper videoMapper;

    public VideoMetricsResponse getByVideoId(UUID videoId) {
        VideoMetric metric = videoMetricRepository.findByVideoId(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Видеометрики не найдены для видео: " + videoId));
        return videoMapper.toMetricsResponse(metric);
    }

    public VideoMetricsResponse getByVideo(Video video) {
        VideoMetric metric = videoMetricRepository.findByVideo(video)
                .orElseThrow(() -> new IllegalArgumentException("Видеометрики не найдены"));
        return videoMapper.toMetricsResponse(metric);
    }

    public void incrementViewCount(UUID videoId) {
        videoMetricRepository.incrementViewCount(videoId);
    }

    public void incrementLikeCount(UUID videoId) {
        videoMetricRepository.incrementLikeCount(videoId);
    }

    public void decrementLikeCount(UUID videoId) {
        videoMetricRepository.decrementLikeCount(videoId);
    }

    public void incrementCommentCount(UUID videoId) {
        videoMetricRepository.incrementCommentCount(videoId);
    }

    public void incrementShareCount(UUID videoId) {
        videoMetricRepository.incrementShareCount(videoId);
    }

    public VideoMetric getEntityByVideo(Video video) {
        return videoMetricRepository.findByVideo(video)
                .orElseThrow(() -> new IllegalArgumentException("Видеометрики не найдены"));
    }
}

