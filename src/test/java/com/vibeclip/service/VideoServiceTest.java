package com.vibeclip.service;

import com.vibeclip.dto.video.VideoMetricsResponse;
import com.vibeclip.dto.video.VideoRequest;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoMetric;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.mapper.VideoMapper;
import com.vibeclip.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoServiceTest {
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private VideoMetricRepository videoMetricRepository;
    @Mock
    private VideoMapper videoMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private VideoMetricService videoMetricService;
    @Mock
    private RecommendationService recommendationService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private FolderVideoRepository folderVideoRepository;
    @Mock
    private VideoReportRepository videoReportRepository;

    @Spy
    @InjectMocks
    private VideoService videoService;

    @Test
    void create_success() {
        VideoRequest request = new VideoRequest();
        request.setTitle("test");
        request.setHashtags(Set.of("tag1", "tag2"));

        User author = new User();
        Video video = new Video();
        Video saved = new Video();
        saved.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metricsResponse = new VideoMetricsResponse();

        when(videoMapper.fromDTO(request)).thenReturn(video);
        when(videoRepository.save(video)).thenReturn(saved);
        when(videoMapper.toDTO(saved)).thenReturn(response);
        when(videoMetricService.getByVideoId(saved.getId())).thenReturn(metricsResponse);

        VideoResponse result = videoService.create(request, author);

        assertNotNull(result);
        assertEquals(response, result);
        assertEquals(metricsResponse, result.getMetrics());

        assertEquals(author, video.getAuthor());
        assertEquals(VideoStatus.PUBLISHED, video.getStatus());


        verify(videoMapper).fromDTO(request);
        verify(videoRepository).save(video);
        verify(videoMetricRepository).save(any(VideoMetric.class));
        verify(videoMapper).toDTO(saved);
        verify(videoMetricService).getByVideoId(saved.getId());
    }

    @Test
    void create_metricsFail_metricsIsNull() {
        VideoRequest request = new VideoRequest();
        request.setTitle("test");

        User author = new User();
        Video video = new Video();
        Video saved = new Video();
        saved.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();

        when(videoMapper.fromDTO(request)).thenReturn(video);
        when(videoRepository.save(video)).thenReturn(saved);
        when(videoMapper.toDTO(saved)).thenReturn(response);
        when(videoMetricService.getByVideoId(saved.getId())).thenThrow(new RuntimeException("error"));

        VideoResponse result = videoService.create(request, author);

        assertNotNull(result);
        assertNull(result.getMetrics());

        verify(videoMetricService).getByVideoId(saved.getId());
    }

    @Test
    void create_shouldInitializeMetricsWithZeroValues() {
        VideoRequest request = new VideoRequest();
        User author = new User();
        Video video = new Video();
        Video saved = new Video();
        saved.setId(UUID.randomUUID());

        when(videoMapper.fromDTO(request)).thenReturn(video);
        when(videoRepository.save(video)).thenReturn(saved);
        when(videoMapper.toDTO(saved)).thenReturn(new VideoResponse());
        when(videoMetricService.getByVideoId(any())).thenReturn(new VideoMetricsResponse());

        videoService.create(request, author);

        verify(videoMetricRepository).save(argThat(metric ->
                metric.getVideo().equals(saved) &&
                        metric.getViewCount() == 0L &&
                        metric.getLikeCount() == 0L &&
                        metric.getCommentCount() == 0L &&
                        metric.getShareCount() == 0L
        ));
    }

    @Test
    void getById_success_withMetrics() {
        UUID id = UUID.randomUUID();

        Video video = new Video();
        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenReturn(metrics);

        VideoResponse result = videoService.getById(id);

        assertNotNull(result);
        assertEquals(response, result);
        assertEquals(metrics, result.getMetrics());

        verify(videoRepository).findById(id);
        verify(videoMapper).toDTO(video);
        verify(videoMetricService).getByVideoId(id);
    }

    @Test
    void getById_metricsFail_setsNull() {
        UUID id = UUID.randomUUID();

        Video video = new Video();
        VideoResponse response = new VideoResponse();

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenThrow(new RuntimeException("metrics error"));

        VideoResponse result = videoService.getById(id);

        assertNotNull(result);
        assertNull(result.getMetrics());

        verify(videoMetricService).getByVideoId(id);
    }

    @Test
    void getById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(videoRepository.findById(id))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> videoService.getById(id)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(id);
        verifyNoInteractions(videoMapper, videoMetricService);
    }

    @Test
    void getByIdAndAuthor_success_withMetrics() {
        UUID id = UUID.randomUUID();

        User author = new User();
        author.setId(id);

        Video video = new Video();
        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenReturn(metrics);

        VideoResponse result = videoService.getByIdAndAuthor(id, author);

        assertNotNull(result);
        assertEquals(response, result);
        assertEquals(metrics, result.getMetrics());

        verify(videoRepository).findByIdAndAuthorId(id, author.getId());
        verify(videoMapper).toDTO(video);
        verify(videoMetricService).getByVideoId(id);
    }

    @Test
    void getByIdAndAuthor_metricsFail_setsNull() {
        UUID id = UUID.randomUUID();

        User author = new User();
        author.setId(id);

        Video video = new Video();
        VideoResponse response = new VideoResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenThrow(new RuntimeException("metrics error"));

        VideoResponse result = videoService.getByIdAndAuthor(id, author);

        assertNotNull(result);
        assertNull(result.getMetrics());

        verify(videoMetricService).getByVideoId(id);
    }

    @Test
    void getByIdAndAuthor_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        User author = new User();
        author.setId(id);

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> videoService.getByIdAndAuthor(id, author)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено или вы не автор"));

        verify(videoRepository).findByIdAndAuthorId(id, author.getId());
        verifyNoInteractions(videoMapper, videoMetricService);
    }

    @Test
    void update_success_fullUpdate_withMetrics() {
        UUID id = UUID.randomUUID();

        User author = new User();
        Video video = new Video();
        video.setHashtags(new HashSet<>());

        VideoRequest request = new VideoRequest();
        request.setTitle("new title");
        request.setDescription("new desc");
        request.setThumbnailUrl("thumb.jpg");
        request.setHashtags(Set.of("tag1", "tag2"));

        Video updated = new Video();
        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(video)).thenReturn(updated);
        when(videoMapper.toDTO(updated)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenReturn(metrics);

        VideoResponse result = videoService.update(id, request, author);

        assertNotNull(result);
        assertEquals(response, result);
        assertEquals(metrics, result.getMetrics());

        // проверка обновлений
        assertEquals("new title", video.getTitle());
        assertEquals("new desc", video.getDescription());
        assertEquals("thumb.jpg", video.getThumbnailUrl());
        assertEquals(2, video.getHashtags().size());

        verify(videoRepository).save(video);
    }

    @Test
    void update_partialUpdate_onlyTitle() {
        UUID id = UUID.randomUUID();

        User author = new User();
        Video video = new Video();

        VideoRequest request = new VideoRequest();
        request.setTitle("only title");

        Video updated = new Video();
        VideoResponse response = new VideoResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(video)).thenReturn(updated);
        when(videoMapper.toDTO(updated)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenReturn(new VideoMetricsResponse());

        videoService.update(id, request, author);

        assertEquals("only title", video.getTitle());
        assertNull(video.getDescription());
        assertNull(video.getThumbnailUrl());
    }

    @Test
    void update_metricsFail_setsNull() {
        UUID id = UUID.randomUUID();

        User author = new User();
        Video video = new Video();

        VideoRequest request = new VideoRequest();

        Video updated = new Video();
        VideoResponse response = new VideoResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(video)).thenReturn(updated);
        when(videoMapper.toDTO(updated)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenThrow(new RuntimeException("fail"));

        VideoResponse result = videoService.update(id, request, author);

        assertNull(result.getMetrics());
    }

    @Test
    void update_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        User author = new User();
        VideoRequest request = new VideoRequest();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> videoService.update(id, request, author));

        verifyNoInteractions(videoMapper, videoMetricService);
    }

    @Test
    void delete_success() {
        UUID id = UUID.randomUUID();

        User author = new User();

        Video video = new Video();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        doNothing().when(videoReportRepository).deleteAllByVideo(any(Video.class));

        videoService.delete(id, author);

        verify(videoRepository).findByIdAndAuthorId(id, author.getId());
        verify(videoRepository).delete(video);
    }

    @Test
    void delete_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        User author = new User();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> videoService.delete(id, author));

        verify(videoRepository).findByIdAndAuthorId(id, author.getId());
        verify(videoRepository).findByIdAndAuthorId(id, author.getId());
        verifyNoMoreInteractions(videoRepository);
    }

    @Test
    void deleteByAdmin_success() {
        UUID id = UUID.randomUUID();

        Video video = new Video();

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));
        doNothing().when(videoReportRepository).deleteAllByVideo(any(Video.class));

        videoService.deleteByAdmin(id);


        verify(videoRepository).findById(id);
        verify(videoRepository).delete(video);
    }

    @Test
    void deleteByAdmin_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(videoRepository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> videoService.deleteByAdmin(id)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(id);
        verify(videoRepository).findById(id);
        verifyNoMoreInteractions(videoRepository);
    }

    @Test
    void deleteVideoCompletely_success_flow() {
        UUID id = UUID.randomUUID();

        Video video = new Video();
        video.setId(id);
        video.setTitle("test");
        video.setVideoUrl("video.mp4");
        video.setThumbnailUrl("thumb.jpg");

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));

        doNothing().when(videoMetricRepository).deleteByVideoId(id);
        doNothing().when(commentRepository).deleteByVideo(video);
        doNothing().when(reactionRepository).deleteByVideo(video);
        doNothing().when(folderVideoRepository).deleteByVideo(video);
        doNothing().when(fileStorageService).deleteFile(anyString());
        doNothing().when(videoReportRepository).deleteAllByVideo(any(Video.class));

        videoService.deleteByAdmin(id);

        verify(videoMetricRepository).deleteByVideoId(id);
        verify(commentRepository).deleteByVideo(video);
        verify(reactionRepository).deleteByVideo(video);
        verify(folderVideoRepository).deleteByVideo(video);
        verify(fileStorageService).deleteFile("video.mp4");
        verify(fileStorageService).deleteFile("thumb.jpg");
        verify(videoRepository).delete(video);
    }

    @Test
    void deleteVideoCompletely_partialFailures_stillDeletesVideo() {
        UUID id = UUID.randomUUID();

        Video video = new Video();
        video.setId(id);

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));

        doThrow(new RuntimeException("fail metrics")).when(videoMetricRepository).deleteByVideoId(id);
        doThrow(new RuntimeException("fail comments")).when(commentRepository).deleteByVideo(video);

        doNothing().when(reactionRepository).deleteByVideo(video);
        doNothing().when(folderVideoRepository).deleteByVideo(video);
        doNothing().when(videoReportRepository).deleteAllByVideo(any(Video.class));

        videoService.deleteByAdmin(id);

        // главное правило: видео ВСЕГДА удаляется
        verify(videoRepository).delete(video);
    }

    @Test
    void deleteVideoCompletely_nullFiles_skipsFileDeletion() {
        UUID id = UUID.randomUUID();

        Video video = new Video();
        video.setId(id);
        video.setVideoUrl(null);
        video.setThumbnailUrl(null);

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));
        doNothing().when(videoReportRepository).deleteAllByVideo(any(Video.class));

        videoService.deleteByAdmin(id);

        verify(fileStorageService, never()).deleteFile(any());
        verify(videoRepository).delete(video);
    }

    @Test
    void publish_success_withMetrics() {
        UUID id = UUID.randomUUID();

        User author = new User();

        Video video = new Video();
        Video updated = new Video();
        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(video)).thenReturn(updated);
        when(videoMapper.toDTO(updated)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenReturn(metrics);

        VideoResponse result = videoService.publish(id, author);

        assertNotNull(result);
        assertEquals(response, result);
        assertEquals(metrics, result.getMetrics());

        assertEquals(VideoStatus.PUBLISHED, video.getStatus());

        verify(videoRepository).save(video);
        verify(videoMetricService).getByVideoId(id);
    }

    @Test
    void publish_metricsFail_setsNull() {
        UUID id = UUID.randomUUID();

        User author = new User();

        Video video = new Video();
        Video updated = new Video();
        VideoResponse response = new VideoResponse();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(video)).thenReturn(updated);
        when(videoMapper.toDTO(updated)).thenReturn(response);
        when(videoMetricService.getByVideoId(id)).thenThrow(new RuntimeException("fail"));

        VideoResponse result = videoService.publish(id, author);

        assertNotNull(result);
        assertNull(result.getMetrics());
    }

    @Test
    void publish_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        User author = new User();

        when(videoRepository.findByIdAndAuthorId(id, author.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> videoService.publish(id, author));

        verify(videoRepository).findByIdAndAuthorId(id, author.getId());
        verifyNoInteractions(videoMapper, videoMetricService);
    }

    @Test
    void getByAuthor_success_withMetrics() {
        User author = new User();
        VideoStatus status = VideoStatus.PUBLISHED;
        Pageable pageable = Pageable.unpaged();

        Video video = new Video();
        video.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByAuthorAndStatus(author, status, pageable)).thenReturn(videoPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(video.getId())).thenReturn(metrics);

        Page<VideoResponse> result = videoService.getByAuthor(author, status, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(response, result.getContent().get(0));
        assertEquals(metrics, result.getContent().get(0).getMetrics());

        verify(videoRepository).findByAuthorAndStatus(author, status, pageable);
    }

    @Test
    void getByAuthor_metricsFail_setsNull() {
        User author = new User();
        VideoStatus status = VideoStatus.PUBLISHED;
        Pageable pageable = Pageable.unpaged();

        Video video = new Video();
        video.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByAuthorAndStatus(author, status, pageable)).thenReturn(videoPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(video.getId())).thenThrow(new RuntimeException("fail"));

        Page<VideoResponse> result = videoService.getByAuthor(author, status, pageable);

        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getMetrics());
    }

    @Test
    void getByAuthor_emptyPage_returnsEmpty() {
        User author = new User();
        VideoStatus status = VideoStatus.PUBLISHED;
        Pageable pageable = Pageable.unpaged();

        Page<Video> emptyPage = Page.empty();

        when(videoRepository.findByAuthorAndStatus(author, status, pageable)).thenReturn(emptyPage);

        Page<VideoResponse> result = videoService.getByAuthor(author, status, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPublished_success_withMetrics() {
        Pageable pageable = Pageable.unpaged();

        Video video = new Video();
        video.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable)).thenReturn(videoPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(video.getId())).thenReturn(metrics);

        Page<VideoResponse> result = videoService.getPublished(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(response, result.getContent().get(0));
        assertEquals(metrics, result.getContent().get(0).getMetrics());

        verify(videoRepository).findByStatus(VideoStatus.PUBLISHED, pageable);
    }

    @Test
    void getPublished_metricsFail_setsNull() {
        Pageable pageable = Pageable.unpaged();

        Video video = new Video();
        video.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable)).thenReturn(videoPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(video.getId())).thenThrow(new RuntimeException("fail"));

        Page<VideoResponse> result = videoService.getPublished(pageable);

        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getMetrics());
    }

    @Test
    void getPublished_empty_returnsEmptyPage() {
        Pageable pageable = Pageable.unpaged();

        Page<Video> emptyPage = Page.empty();

        when(videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable)).thenReturn(emptyPage);

        Page<VideoResponse> result = videoService.getPublished(pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void getRecommendedFeed_success_withMetrics() {
        User user = new User();
        Pageable pageable = Pageable.unpaged();
        Double randomPercentage = 0.2;

        Video video = new Video();
        video.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        Page<Video> recommendedPage = new PageImpl<>(List.of(video));

        when(recommendationService.getRecommendedFeed(user, pageable, randomPercentage)).thenReturn(recommendedPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(video.getId())).thenReturn(metrics);

        Page<VideoResponse> result = videoService.getRecommendedFeed(user, pageable, randomPercentage);

        assertEquals(1, result.getContent().size());
        assertEquals(response, result.getContent().get(0));
        assertEquals(metrics, result.getContent().get(0).getMetrics());

        verify(recommendationService).getRecommendedFeed(user, pageable, randomPercentage);
    }

    @Test
    void getRecommendedFeed_metricsFail_setsNull() {
        User user = new User();
        Pageable pageable = Pageable.unpaged();
        Double randomPercentage = 0.2;

        Video video = new Video();
        video.setId(UUID.randomUUID());

        VideoResponse response = new VideoResponse();

        Page<Video> recommendedPage = new PageImpl<>(List.of(video));

        when(recommendationService.getRecommendedFeed(user, pageable, randomPercentage)).thenReturn(recommendedPage);
        when(videoMapper.toDTO(video)).thenReturn(response);
        when(videoMetricService.getByVideoId(video.getId())).thenThrow(new RuntimeException("fail"));

        Page<VideoResponse> result = videoService.getRecommendedFeed(user, pageable, randomPercentage);

        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getMetrics());
    }

    @Test
    void getRecommendedFeed_empty_returnsEmptyPage() {
        User user = new User();
        Pageable pageable = Pageable.unpaged();
        Double randomPercentage = 0.2;

        Page<Video> emptyPage = Page.empty();

        when(recommendationService.getRecommendedFeed(user, pageable, randomPercentage)).thenReturn(emptyPage);

        Page<VideoResponse> result = videoService.getRecommendedFeed(user, pageable, randomPercentage);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntityById_success() {
        UUID id = UUID.randomUUID();

        Video video = new Video();

        when(videoRepository.findById(id)).thenReturn(Optional.of(video));

        Video result = videoService.getEntityById(id);

        assertNotNull(result);
        assertEquals(video, result);

        verify(videoRepository).findById(id);
    }

    @Test
    void getEntityById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(videoRepository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> videoService.getEntityById(id)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(id);
    }

    @Test
    void createWithFiles_withThumbnail_success() {
        MultipartFile videoFile = mock(MultipartFile.class);
        MultipartFile thumbnailFile = mock(MultipartFile.class);
        User author = new User();
        UUID videoId = UUID.randomUUID();

        Video saved = new Video();
        saved.setId(videoId);

        VideoResponse response = new VideoResponse();
        VideoMetricsResponse metrics = new VideoMetricsResponse();

        when(fileStorageService.storeFile(videoFile, "video"))
                .thenReturn("/uploads/video.mp4");
        when(fileStorageService.storeFile(thumbnailFile, "thumb"))
                .thenReturn("/uploads/thumb.jpg");
        when(videoRepository.save(any(Video.class))).thenReturn(saved);
        when(videoMapper.toDTO(any(Video.class))).thenReturn(response);
        when(videoMetricService.getByVideoId(videoId)).thenReturn(metrics);

        VideoResponse result = videoService.createWithFiles(
                videoFile, thumbnailFile, "title", "desc", Set.of("tag1"), 120, author
        );

        assertNotNull(result);
        assertSame(response, result);
        assertEquals(metrics, result.getMetrics());

        verify(fileStorageService).storeFile(videoFile, "video");
        verify(fileStorageService).storeFile(thumbnailFile, "thumb");
        verify(fileStorageService, never()).getFilePath(anyString());
        verify(fileStorageService, never()).extractThumbnailFromVideo(any());
    }

    @Test
    void createWithFiles_generateThumbnail_success() throws Exception {
        MultipartFile videoFile = mock(MultipartFile.class);
        User author = new User();

        String videoUrl = "/uploads/video.mp4";
        Path videoPath = mock(Path.class);
        String generatedThumbnailUrl = "generated.jpg";

        when(fileStorageService.storeFile(videoFile, "video")).thenReturn(videoUrl);
        when(fileStorageService.getFilePath(videoUrl)).thenReturn(videoPath);
        when(fileStorageService.extractThumbnailFromVideo(videoPath))
                .thenReturn(generatedThumbnailUrl);
        when(videoRepository.save(any(Video.class))).thenReturn(new Video());
        when(videoMapper.toDTO(any())).thenReturn(new VideoResponse());
        when(videoMetricService.getByVideoId(any())).thenReturn(new VideoMetricsResponse());

        VideoResponse result = videoService.createWithFiles(
                videoFile, null, "title", "desc", Set.of(), 100, author
        );

        assertNotNull(result);
        verify(fileStorageService).storeFile(videoFile, "video");
        verify(fileStorageService).getFilePath(videoUrl);
        verify(fileStorageService).extractThumbnailFromVideo(videoPath);
        verify(fileStorageService, never()).storeFile(any(MultipartFile.class), eq("thumb"));
    }

    @Test
    void createWithFiles_thumbnailExtractionReturnsNull() throws Exception {
        MultipartFile videoFile = mock(MultipartFile.class);

        String videoUrl = "/uploads/video.mp4";
        Path videoPath = mock(Path.class);

        when(fileStorageService.storeFile(videoFile, "video")).thenReturn(videoUrl);
        when(fileStorageService.getFilePath(videoUrl)).thenReturn(videoPath);
        when(fileStorageService.extractThumbnailFromVideo(videoPath)).thenReturn(null);
        when(videoRepository.save(any())).thenReturn(new Video());
        when(videoMapper.toDTO(any())).thenReturn(new VideoResponse());
        when(videoMetricService.getByVideoId(any())).thenReturn(new VideoMetricsResponse());

        VideoResponse result = videoService.createWithFiles(
                videoFile, null, "title", "desc", null, 0, new User()
        );

        assertNotNull(result);
        verify(fileStorageService).extractThumbnailFromVideo(videoPath);
    }

    @Test
    void createWithFiles_thumbnailExtractionThrowsException() throws Exception {
        MultipartFile videoFile = mock(MultipartFile.class);

        String videoUrl = "/uploads/video.mp4";

        when(fileStorageService.storeFile(videoFile, "video")).thenReturn(videoUrl);
        when(fileStorageService.getFilePath(videoUrl)).thenThrow(new RuntimeException("fail"));
        when(videoRepository.save(any())).thenReturn(new Video());
        when(videoMapper.toDTO(any())).thenReturn(new VideoResponse());
        when(videoMetricService.getByVideoId(any())).thenReturn(new VideoMetricsResponse());

        VideoResponse result = videoService.createWithFiles(
                videoFile, null, "title", "desc", null, 0, new User()
        );

        assertNotNull(result);
        verify(fileStorageService).getFilePath(videoUrl);
        verify(fileStorageService, never()).extractThumbnailFromVideo(any());
    }
}