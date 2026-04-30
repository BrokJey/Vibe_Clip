package com.vibeclip.service;

import com.vibeclip.dto.video.VideoMetricsResponse;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoMetric;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.VideoMetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoMetricServiceTest {
    @Mock
    private VideoMetricRepository videoMetricRepository;
    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private VideoMetricService videoMetricService;

    @Test
    void getByVideoId_success() {
        UUID videoId = UUID.randomUUID();
        VideoMetric metric = new VideoMetric();
        VideoMetricsResponse response = new VideoMetricsResponse();

        when(videoMetricRepository.findByVideoId(videoId))
                .thenReturn(Optional.of(metric));
        when(videoMapper.toMetricsResponse(metric))
                .thenReturn(response);

        VideoMetricsResponse result = videoMetricService.getByVideoId(videoId);

        assertNotNull(result);
        assertEquals(response, result);

        verify(videoMetricRepository).findByVideoId(videoId);
        verify(videoMapper).toMetricsResponse(metric);
    }

    @Test
    void getByVideoId_notFound() {
        UUID videoId = UUID.randomUUID();

        when(videoMetricRepository.findByVideoId(videoId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> videoMetricService.getByVideoId(videoId)
        );

        assertTrue(exception.getMessage().contains("Видеометрики не найдены"));

        verify(videoMetricRepository).findByVideoId(videoId);
        verify(videoMapper, never()).toMetricsResponse(any());
    }

    @Test
    void getByVideo_success() {
        Video video = new Video();
        VideoMetric metric = new VideoMetric();
        VideoMetricsResponse response = new VideoMetricsResponse();

        when(videoMetricRepository.findByVideo(video))
                .thenReturn(Optional.of(metric));
        when(videoMapper.toMetricsResponse(metric))
                .thenReturn(response);

        VideoMetricsResponse result = videoMetricService.getByVideo(video);

        assertNotNull(result);
        assertEquals(response, result);

        verify(videoMetricRepository).findByVideo(video);
        verify(videoMapper).toMetricsResponse(metric);
    }

    @Test
    void getByVideo_notFound() {
        Video video = new Video();

        when(videoMetricRepository.findByVideo(video))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> videoMetricService.getByVideo(video)
        );

        assertEquals("Видеометрики не найдены", exception.getMessage());

        verify(videoMetricRepository).findByVideo(video);
        verify(videoMapper, never()).toMetricsResponse(any());
    }

    @Test
    void incrementViewCount_success() {
        UUID videoId = UUID.randomUUID();

        videoMetricService.incrementViewCount(videoId);

        verify(videoMetricRepository).incrementViewCount(videoId);
    }

    @Test
    void incrementLikeCount_success() {
        UUID videoId = UUID.randomUUID();

        videoMetricService.incrementLikeCount(videoId);

        verify(videoMetricRepository).incrementLikeCount(videoId);
    }

    @Test
    void decrementLikeCount_success() {
        UUID videoId = UUID.randomUUID();

        videoMetricService.decrementLikeCount(videoId);

        verify(videoMetricRepository).decrementLikeCount(videoId);
    }

    @Test
    void incrementCommentCount_success() {
        UUID videoId = UUID.randomUUID();

        videoMetricService.incrementCommentCount(videoId);

        verify(videoMetricRepository).incrementCommentCount(videoId);
    }

    @Test
    void decrementCommentCount_success() {
        UUID videoId = UUID.randomUUID();

        videoMetricService.decrementCommentCount(videoId);

        verify(videoMetricRepository).decrementCommentCount(videoId);
    }

    @Test
    void incrementShareCount_success() {
        UUID videoId = UUID.randomUUID();

        videoMetricService.incrementShareCount(videoId);

        verify(videoMetricRepository).incrementShareCount(videoId);
    }

    @Test
    void getEntityByVideo_success() {
        Video video = new Video();
        VideoMetric metric = new VideoMetric();

        when(videoMetricRepository.findByVideo(video))
                .thenReturn(Optional.of(metric));

        VideoMetric result = videoMetricService.getEntityByVideo(video);

        assertNotNull(result);
        assertEquals(metric, result);

        verify(videoMetricRepository).findByVideo(video);
    }

    @Test
    void getEntityByVideo_notFound() {
        Video video = new Video();

        when(videoMetricRepository.findByVideo(video))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> videoMetricService.getEntityByVideo(video)
        );

        assertEquals("Видеометрики не найдены", exception.getMessage());

        verify(videoMetricRepository).findByVideo(video);
    }
}
