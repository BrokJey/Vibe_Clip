package com.vibeclip.service;

import com.vibeclip.dto.reaction.ReactionRequest;
import com.vibeclip.dto.reaction.ReactionResponse;
import com.vibeclip.entity.Reaction;
import com.vibeclip.entity.ReactionType;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.mapper.ReactionMapper;
import com.vibeclip.repository.ReactionRepository;
import com.vibeclip.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReactionServiceTest {
    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private VideoMetricService videoMetricService;
    @Mock
    private ReactionMapper reactionMapper;

    @InjectMocks
    private ReactionService reactionService;

    @Test
    void create_like_shouldIncrementLikeCount() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.LIKE);

        Reaction reaction = new Reaction();

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.LIKE))
                .thenReturn(Optional.empty());
        when(reactionMapper.fromDTO(request)).thenReturn(reaction);
        when(reactionRepository.save(any())).thenReturn(reaction);
        when(reactionMapper.toDTO(reaction)).thenReturn(new ReactionResponse());

        reactionService.create(request, user);

        verify(videoMetricService).incrementLikeCount(videoId);
    }

    @Test
    void create_dislike_shouldNotAffectMetrics() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        Video video = new Video();
        video.setId(videoId);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.DISLIKE);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.DISLIKE))
                .thenReturn(Optional.empty());
        when(reactionMapper.fromDTO(request)).thenReturn(new Reaction());
        when(reactionRepository.save(any())).thenReturn(new Reaction());
        when(reactionMapper.toDTO(any())).thenReturn(new ReactionResponse());

        reactionService.create(request, user);

        verifyNoInteractions(videoMetricService);
    }

    @Test
    void create_videoNotFound_shouldThrow() {
        UUID videoId = UUID.randomUUID();

        User user = new User();

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> reactionService.create(request, user)
        );

        verify(reactionRepository, never()).findByUserAndVideoAndReactionType(any(), any(), any());
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void delete_success() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        reactionService.delete(videoId, ReactionType.LIKE, user);

        verify(videoRepository).findById(videoId);
        verify(reactionRepository).deleteByUserAndVideoAndReactionType(user, video, ReactionType.LIKE);
        verify(videoMetricService).decrementLikeCount(videoId);
    }

    @Test
    void delete_videoNotFound_shouldThrow() {
        UUID videoId = UUID.randomUUID();

        User user = new User();

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> reactionService.delete(videoId, ReactionType.LIKE, user)
        );

        verify(reactionRepository, never()).deleteByUserAndVideoAndReactionType(any(), any(), any());
        verify(videoMetricService, never()).decrementLikeCount(any());
    }

    @Test
    void getByUserAndVideo_success() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        Reaction likeReaction = new Reaction();
        likeReaction.setReactionType(ReactionType.LIKE);

        Reaction shareReaction = new Reaction();
        shareReaction.setReactionType(ReactionType.SHARE);

        ReactionResponse likeResponse = new ReactionResponse();
        likeResponse.setReactionType(ReactionType.LIKE);

        ReactionResponse shareResponse = new ReactionResponse();
        shareResponse.setReactionType(ReactionType.SHARE);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideo(user, video))
                .thenReturn(List.of(likeReaction, shareReaction));

        when(reactionMapper.toDTO(likeReaction)).thenReturn(likeResponse);
        when(reactionMapper.toDTO(shareReaction)).thenReturn(shareResponse);

        List<ReactionResponse> result = reactionService.getByUserAndVideo(user, videoId);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(ReactionType.LIKE, result.get(0).getReactionType());
        assertNull(result.get(0).getShareUrl());

        assertEquals(ReactionType.SHARE, result.get(1).getReactionType());
        assertNotNull(result.get(1).getShareUrl());
        assertTrue(result.get(1).getShareUrl().contains(videoId.toString()));

        verify(videoRepository).findById(videoId);
        verify(reactionRepository).findByUserAndVideo(user, video);
        verify(reactionMapper).toDTO(likeReaction);
        verify(reactionMapper).toDTO(shareReaction);
    }

    @Test
    void getByUserAndVideo_videoNotFound_shouldThrow() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reactionService.getByUserAndVideo(user, videoId)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(videoId);
        verify(reactionRepository, never()).findByUserAndVideo(any(), any());
        verify(reactionMapper, never()).toDTO(any());
    }

    @Test
    void hasReaction_success_true() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.existsByUserAndVideoAndReactionType(user, video, ReactionType.LIKE))
                .thenReturn(true);

        boolean result = reactionService.hasReaction(user, videoId, ReactionType.LIKE);

        assertTrue(result);

        verify(videoRepository).findById(videoId);
        verify(reactionRepository).existsByUserAndVideoAndReactionType(user, video, ReactionType.LIKE);
    }

    @Test
    void hasReaction_success_false() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.existsByUserAndVideoAndReactionType(user, video, ReactionType.LIKE))
                .thenReturn(false);

        boolean result = reactionService.hasReaction(user, videoId, ReactionType.LIKE);

        assertFalse(result);

        verify(videoRepository).findById(videoId);
        verify(reactionRepository).existsByUserAndVideoAndReactionType(user, video, ReactionType.LIKE);
    }

    @Test
    void hasReaction_videoNotFound_shouldThrow() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reactionService.hasReaction(user, videoId, ReactionType.LIKE)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(videoId);
        verify(reactionRepository, never()).existsByUserAndVideoAndReactionType(any(), any(), any());
    }

    @Test
    void getByUser_success_like() {
        User user = new User();
        user.setId(UUID.randomUUID());

        Reaction reaction1 = new Reaction();
        reaction1.setReactionType(ReactionType.LIKE);
        reaction1.setVideo(new Video());

        Reaction reaction2 = new Reaction();
        reaction2.setReactionType(ReactionType.LIKE);
        reaction2.setVideo(new Video());

        ReactionResponse response1 = new ReactionResponse();
        response1.setReactionType(ReactionType.LIKE);

        ReactionResponse response2 = new ReactionResponse();
        response2.setReactionType(ReactionType.LIKE);

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.LIKE)).thenReturn(List.of(reaction1, reaction2));
        when(reactionMapper.toDTO(reaction1)).thenReturn(response1);
        when(reactionMapper.toDTO(reaction2)).thenReturn(response2);

        List<ReactionResponse> result = reactionService.getByUser(user, ReactionType.LIKE);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertNull(result.get(0).getShareUrl());
        assertNull(result.get(1).getShareUrl());

        verify(reactionRepository).findByUserAndReactionType(user, ReactionType.LIKE);
        verify(reactionMapper).toDTO(reaction1);
        verify(reactionMapper).toDTO(reaction2);
    }

    @Test
    void getByUser_success_share() {
        User user = new User();
        user.setId(UUID.randomUUID());

        UUID videoId1 = UUID.randomUUID();
        UUID videoId2 = UUID.randomUUID();

        Video video1 = new Video();
        video1.setId(videoId1);

        Video video2 = new Video();
        video2.setId(videoId2);

        Reaction reaction1 = new Reaction();
        reaction1.setReactionType(ReactionType.SHARE);
        reaction1.setVideo(video1);

        Reaction reaction2 = new Reaction();
        reaction2.setReactionType(ReactionType.SHARE);
        reaction2.setVideo(video2);

        ReactionResponse response1 = new ReactionResponse();
        response1.setReactionType(ReactionType.SHARE);

        ReactionResponse response2 = new ReactionResponse();
        response2.setReactionType(ReactionType.SHARE);

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.SHARE)).thenReturn(List.of(reaction1, reaction2));
        when(reactionMapper.toDTO(reaction1)).thenReturn(response1);
        when(reactionMapper.toDTO(reaction2)).thenReturn(response2);

        List<ReactionResponse> result = reactionService.getByUser(user, ReactionType.SHARE);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertNotNull(result.get(0).getShareUrl());
        assertNotNull(result.get(1).getShareUrl());

        assertTrue(result.get(0).getShareUrl().contains(videoId1.toString()));
        assertTrue(result.get(1).getShareUrl().contains(videoId2.toString()));

        verify(reactionRepository).findByUserAndReactionType(user, ReactionType.SHARE);
        verify(reactionMapper).toDTO(reaction1);
        verify(reactionMapper).toDTO(reaction2);
    }

    @Test
    void getByUser_emptyList() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.LIKE)).thenReturn(List.of());

        List<ReactionResponse> result = reactionService.getByUser(user, ReactionType.LIKE);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(reactionRepository).findByUserAndReactionType(user, ReactionType.LIKE);
        verify(reactionMapper, never()).toDTO(any());
    }

    @Test
    void create_likeToggle_shouldDecrementLikeCount() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.LIKE);

        Reaction existing = new Reaction();

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.LIKE)).thenReturn(Optional.of(existing));

        ReactionResponse result = reactionService.create(request, user);
        assertNull(result);

        verify(reactionRepository).delete(existing);
        verify(videoMetricService).decrementLikeCount(videoId);
    }

    @Test
    void create_view_shouldIncrementViewCount() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.VIEW);

        Reaction reaction = new Reaction();

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.VIEW))
                .thenReturn(Optional.empty());
        when(reactionMapper.fromDTO(request)).thenReturn(reaction);
        when(reactionRepository.save(any())).thenReturn(reaction);
        when(reactionMapper.toDTO(reaction)).thenReturn(new ReactionResponse());

        reactionService.create(request, user);

        verify(videoMetricService).incrementViewCount(videoId);
    }

    @Test
    void create_share_shouldIncrementShareCount() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.SHARE);

        Reaction reaction = new Reaction();

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.SHARE))
                .thenReturn(Optional.empty());
        when(reactionMapper.fromDTO(request)).thenReturn(reaction);
        when(reactionRepository.save(any())).thenReturn(reaction);
        when(reactionMapper.toDTO(reaction)).thenReturn(new ReactionResponse());

        reactionService.create(request, user);

        verify(videoMetricService).incrementShareCount(videoId);
    }

    @Test
    void delete_view_shouldNotDecrementViewCount() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        reactionService.delete(videoId, ReactionType.VIEW, user);

        verifyNoInteractions(videoMetricService);
    }

    @Test
    void getByUser_share_withBaseUrl() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        Reaction reaction = new Reaction();
        reaction.setReactionType(ReactionType.SHARE);
        reaction.setVideo(video);

        ReactionResponse response = new ReactionResponse();

        ReflectionTestUtils.setField(reactionService, "appBaseUrl", "http://localhost/");

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.SHARE))
                .thenReturn(List.of(reaction));

        when(reactionMapper.toDTO(reaction)).thenReturn(response);

        List<ReactionResponse> result = reactionService.getByUser(user, ReactionType.SHARE);

        assertNotNull(result);
        assertEquals(1, result.size());

        String expectedUrl = "http://localhost/api/v1/videos/" + videoId;
        assertEquals(expectedUrl, result.get(0).getShareUrl());
    }

    @Test
    void getByUser_share_withoutBaseUrl() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        Reaction reaction = new Reaction();
        reaction.setReactionType(ReactionType.SHARE);
        reaction.setVideo(video);

        ReactionResponse response = new ReactionResponse();

        // baseUrl НЕ задан
        ReflectionTestUtils.setField(reactionService, "appBaseUrl", null);

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.SHARE))
                .thenReturn(List.of(reaction));

        when(reactionMapper.toDTO(reaction)).thenReturn(response);

        List<ReactionResponse> result = reactionService.getByUser(user, ReactionType.SHARE);

        assertNotNull(result);
        assertEquals(1, result.size());

        String expectedUrl = "/api/v1/videos/" + videoId;
        assertEquals(expectedUrl, result.get(0).getShareUrl());
    }

    @Test
    void getByUser_share_baseUrlWithTrailingSlash() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        Video video = new Video();
        video.setId(videoId);

        Reaction reaction = new Reaction();
        reaction.setReactionType(ReactionType.SHARE);
        reaction.setVideo(video);

        ReactionResponse response = new ReactionResponse();

        ReflectionTestUtils.setField(reactionService, "appBaseUrl", "http://localhost/"); // со слэшем

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.SHARE))
                .thenReturn(List.of(reaction));

        when(reactionMapper.toDTO(reaction)).thenReturn(response);

        List<ReactionResponse> result = reactionService.getByUser(user, ReactionType.SHARE);

        assertEquals("http://localhost/api/v1/videos/" + videoId,
                result.get(0).getShareUrl());
    }

    @Test
    void create_share_existing_shouldReturnSameWithUrl() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        Video video = new Video();
        video.setId(videoId);

        Reaction existing = new Reaction();
        existing.setReactionType(ReactionType.SHARE);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.SHARE);

        ReactionResponse response = new ReactionResponse();

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.SHARE))
                .thenReturn(Optional.of(existing));
        when(reactionMapper.toDTO(existing)).thenReturn(response);

        ReactionResponse result = reactionService.create(request, user);

        assertNotNull(result);
        assertNotNull(result.getShareUrl());

        verify(videoMetricService, never()).incrementShareCount(any());
    }

    @Test
    void create_existingView_shouldUpdateDuration() {
        UUID videoId = UUID.randomUUID();

        User user = new User();
        Video video = new Video();
        video.setId(videoId);

        Reaction existing = new Reaction();
        existing.setReactionType(ReactionType.VIEW);

        ReactionRequest request = new ReactionRequest();
        request.setVideoId(videoId);
        request.setReactionType(ReactionType.VIEW);
        request.setWatchDurationSeconds(120);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(reactionRepository.findByUserAndVideoAndReactionType(user, video, ReactionType.VIEW))
                .thenReturn(Optional.of(existing));
        when(reactionRepository.save(existing)).thenReturn(existing);
        when(reactionMapper.toDTO(existing)).thenReturn(new ReactionResponse());

        ReactionResponse result = reactionService.create(request, user);

        assertNotNull(result);
        assertEquals(120, existing.getWatchDurationSeconds());
    }
}
