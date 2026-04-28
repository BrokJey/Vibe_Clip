package com.vibeclip.service;

import com.vibeclip.entity.*;
import com.vibeclip.repository.FolderVideoRepository;
import com.vibeclip.repository.ReactionRepository;
import com.vibeclip.repository.VideoMetricRepository;
import com.vibeclip.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private FolderVideoRepository folderVideoRepository;
    @Mock
    private VideoMetricRepository videoMetricRepository;
    @Mock
    private ReactionRepository reactionRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void generateFeedForFolder_success_withoutHashtags() {
        Folder folder = new Folder();
        folder.setId(UUID.randomUUID());

        FolderPreference preference = new FolderPreference();
        preference.setAllowedHashtags(null);
        folder.setPreference(preference);

        Video video1 = new Video();
        video1.setId(UUID.randomUUID());
        Video video2 = new Video();
        video2.setId(UUID.randomUUID());

        Page<Video> page = new PageImpl<>(List.of(video1, video2));

        when(folderVideoRepository.findByFolder(folder)).thenReturn(List.of());
        when(videoRepository.findByStatus(eq(VideoStatus.PUBLISHED), any())).thenReturn(page);
        when(folderVideoRepository.save(any(FolderVideo.class))).thenAnswer(inv -> inv.getArgument(0));

        List<FolderVideo> result = recommendationService.generateFeedForFolder(folder, 2);

        assertEquals(2, result.size());

        verify(videoRepository).findByStatus(eq(VideoStatus.PUBLISHED), any());
        verify(folderVideoRepository, times(2)).save(any(FolderVideo.class));
    }

    @Test
    void generateFeedForFolder_withHashtags() {
        Folder folder = new Folder();
        folder.setId(UUID.randomUUID());

        FolderPreference preference = new FolderPreference();
        preference.setAllowedHashtags(Set.of("java", "spring"));
        folder.setPreference(preference);

        Video video = new Video();
        video.setId(UUID.randomUUID());

        Page<Video> page = new PageImpl<>(List.of(video));

        when(folderVideoRepository.findByFolder(folder)).thenReturn(List.of());
        when(videoRepository.findByHashtagsIn(anyList(), eq(VideoStatus.PUBLISHED), any())).thenReturn(page);
        when(folderVideoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<FolderVideo> result = recommendationService.generateFeedForFolder(folder, 1);

        assertEquals(1, result.size());

        verify(videoRepository).findByHashtagsIn(anyList(), eq(VideoStatus.PUBLISHED), any());
    }

    @Test
    void generateFeedForFolder_shouldExcludeExistingVideos() {
        Folder folder = new Folder();
        folder.setId(UUID.randomUUID());

        Video existingVideo = new Video();
        existingVideo.setId(UUID.randomUUID());

        FolderVideo existing = new FolderVideo();
        existing.setVideo(existingVideo);

        Video newVideo = new Video();
        newVideo.setId(UUID.randomUUID());

        Page<Video> page = new PageImpl<>(List.of(existingVideo, newVideo));

        when(folderVideoRepository.findByFolder(folder)).thenReturn(List.of(existing));
        when(videoRepository.findByStatus(any(), any())).thenReturn(page);
        when(folderVideoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<FolderVideo> result = recommendationService.generateFeedForFolder(folder, 5);

        assertEquals(1, result.size());
        assertEquals(newVideo.getId(), result.get(0).getVideo().getId());
    }

    @Test
    void generateFeedForFolder_shouldSetCorrectPosition() {
        Folder folder = new Folder();
        folder.setId(UUID.randomUUID());

        Video existingVideo = new Video();
        existingVideo.setId(UUID.randomUUID());

        FolderVideo existing = new FolderVideo();
        existing.setPosition(10);
        existing.setVideo(existingVideo);

        Video video = new Video();
        video.setId(UUID.randomUUID());

        Page<Video> page = new PageImpl<>(List.of(video));

        when(folderVideoRepository.findByFolder(folder)).thenReturn(List.of(existing));
        when(videoRepository.findByStatus(any(), any())).thenReturn(page);
        when(folderVideoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        List<FolderVideo> result = recommendationService.generateFeedForFolder(folder, 1);

        assertEquals(11, result.get(0).getPosition());
    }

    @Test
    void getRecommendedFeed_success_basic() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("test");

        Pageable pageable = PageRequest.of(0, 5);

        Video likedVideo = new Video();
        likedVideo.setId(UUID.randomUUID());
        likedVideo.setStatus(VideoStatus.PUBLISHED);
        likedVideo.setHashtags(Set.of("java"));

        Reaction reaction = new Reaction();
        reaction.setVideo(likedVideo);

        Video otherVideo = new Video();
        otherVideo.setId(UUID.randomUUID());
        otherVideo.setStatus(VideoStatus.PUBLISHED);
        otherVideo.setHashtags(Set.of("python"));

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.LIKE)).thenReturn(List.of(reaction));
        when(videoRepository.findByStatus(eq(VideoStatus.PUBLISHED), any())).thenReturn(new PageImpl<>(List.of(likedVideo, otherVideo)));

        Page<Video> result = recommendationService.getRecommendedFeed(user, pageable, 0.3);

        assertNotNull(result);
        assertTrue(result.getContent().size() <= 5);
    }

    @Test
    void getRecommendedFeed_shouldUseLikedVideos() {
        User user = new User();

        Video likedVideo = new Video();
        likedVideo.setId(UUID.randomUUID());
        likedVideo.setStatus(VideoStatus.PUBLISHED);
        likedVideo.setHashtags(Set.of("spring"));

        Reaction reaction = new Reaction();
        reaction.setVideo(likedVideo);

        when(reactionRepository.findByUserAndReactionType(user, ReactionType.LIKE)).thenReturn(List.of(reaction));
        when(videoRepository.findByStatus(any(), any())).thenReturn(new PageImpl<>(List.of(likedVideo)));

        Page<Video> result = recommendationService.getRecommendedFeed(user, PageRequest.of(0, 5), 0.3);

        assertFalse(result.getContent().isEmpty());
    }

    @Test
    void getRecommendedFeed_shouldRespectPageSize() {
        User user = new User();

        Video video = new Video();
        video.setId(UUID.randomUUID());
        video.setStatus(VideoStatus.PUBLISHED);

        when(reactionRepository.findByUserAndReactionType(any(), any())).thenReturn(List.of());
        when(videoRepository.findByStatus(any(), any())).thenReturn(new PageImpl<>(List.of(video)));

        Pageable pageable = PageRequest.of(0, 2);

        Page<Video> result = recommendationService.getRecommendedFeed(user, pageable, 0.3);

        assertTrue(result.getContent().size() <= 2);
    }
}
