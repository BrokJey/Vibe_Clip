package com.vibeclip.service;

import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderPreference;
import com.vibeclip.entity.FolderVideo;
import com.vibeclip.entity.Reaction;
import com.vibeclip.entity.ReactionType;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.repository.FolderVideoRepository;
import com.vibeclip.repository.ReactionRepository;
import com.vibeclip.repository.VideoRepository;
import com.vibeclip.repository.VideoMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final VideoRepository videoRepository;
    private final FolderVideoRepository folderVideoRepository;
    private final VideoMetricRepository videoMetricRepository;
    private final ReactionRepository reactionRepository;


    @Transactional
    public List<FolderVideo> generateFeedForFolder(Folder folder, int limit) {
        FolderPreference preference = folder.getPreference();

        List<FolderVideo> existingFolderVideos = folderVideoRepository.findByFolder(folder);
        java.util.Set<UUID> existingVideoIds = existingFolderVideos.stream()
                .map(fv -> fv.getVideo().getId())
                .collect(Collectors.toSet());

        Pageable pageable = PageRequest.of(0, limit * 5);
        Page<Video> candidates;
        
        if (preference != null && preference.getAllowedHashtags() != null && !preference.getAllowedHashtags().isEmpty()) {
            candidates = videoRepository.findByHashtagsIn(
                    preference.getAllowedHashtags().stream().collect(Collectors.toList()),
                    VideoStatus.PUBLISHED,
                    pageable
            );
        } else {
            candidates = videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable);
        }

        List<Video> newCandidates = candidates.getContent().stream()
                .filter(video -> !existingVideoIds.contains(video.getId()))
                .collect(Collectors.toList());
        
        log.debug("После исключения существующих видео осталось {} новых кандидатов", newCandidates.size());

        java.util.Collections.shuffle(newCandidates);

        newCandidates = newCandidates.stream()
                .limit(limit)
                .collect(Collectors.toList());

        int maxPosition = existingFolderVideos.stream()
                .mapToInt(FolderVideo::getPosition)
                .max()
                .orElse(-1);
        
        List<FolderVideo> folderVideos = new java.util.ArrayList<>();
        for (int i = 0; i < newCandidates.size(); i++) {
            FolderVideo folderVideo = FolderVideo.builder()
                    .folder(folder)
                    .video(newCandidates.get(i))
                    .score(1.0)
                    .position(maxPosition + 1 + i)
                    .shown(false)
                    .build();
            folderVideos.add(folderVideo);
        }

        folderVideos.forEach(folderVideoRepository::save);

        log.info("Сгенерировано {} новых рекомендаций для папки {}", folderVideos.size(), folder.getId());
        return folderVideos;
    }

    public List<FolderVideo> getFeedForFolder(Folder folder, int limit) {
        List<FolderVideo> folderVideos = folderVideoRepository
                .findByFolderAndShownFalse(folder);

        if (folderVideos.isEmpty() || folderVideos.size() < limit) {
            generateFeedForFolder(folder, limit);
            folderVideos = folderVideoRepository
                    .findByFolderAndShownFalse(folder);
        }

        java.util.Collections.shuffle(folderVideos);
        return folderVideos.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получает рекомендованную ленту для пользователя на основе его лайков
     * Простая система: видео с хэштегами из лайков попадаются чаще, остальные - для разнообразия
     * 
     * @param user пользователь для которого формируется лента
     * @param pageable пагинация
     * @param randomPercentage процент случайных видео (0.0 - 1.0), по умолчанию 0.3 (30%)
     * @return страница с рекомендованными видео
     */
    public Page<Video> getRecommendedFeed(User user, Pageable pageable, Double randomPercentage) {
        if (randomPercentage == null) {
            randomPercentage = 0.3; // По умолчанию 30% случайных видео
        }

        // Получаем все лайки пользователя
        List<Reaction> userLikes = reactionRepository.findByUserAndReactionType(user, ReactionType.LIKE);
        // Собираем лайкнутые видео (только опубликованные) и их хэштеги
        List<Video> likedVideos = new ArrayList<>();
        Set<String> likedHashtags = new HashSet<>();
        Set<UUID> likedVideoIds = new HashSet<>();

        for (Reaction reaction : userLikes) {
            Video video = reaction.getVideo();
            if (video != null && video.getStatus() == VideoStatus.PUBLISHED) {
                likedVideos.add(video);
                likedVideoIds.add(video.getId());
                if (video.getHashtags() != null) {
                    likedHashtags.addAll(video.getHashtags());
                }
            }
        }
        
        // Получаем все опубликованные видео
        int totalSize = pageable.getPageSize();
        Pageable extendedPageable = PageRequest.of(0, totalSize * 10); // Берем больше для выбора
        Page<Video> allVideos = videoRepository.findByStatus(VideoStatus.PUBLISHED, extendedPageable);
        
        // Разделяем на видео с любимыми хэштегами и остальные
        List<Video> videosWithLikedHashtags = new ArrayList<>();
        List<Video> otherVideos = new ArrayList<>();
        
        for (Video video : allVideos.getContent()) {
            // Проверяем, есть ли у видео хэштеги из лайков
            boolean hasLikedHashtag = false;
            if (video.getHashtags() != null && !video.getHashtags().isEmpty()) {
                for (String hashtag : video.getHashtags()) {
                    if (likedHashtags.contains(hashtag)) {
                        hasLikedHashtag = true;
                        break;
                    }
                }
            }

            if (hasLikedHashtag) {
                videosWithLikedHashtags.add(video);
            } else {
                otherVideos.add(video);
            }
        }
        
        // Перемешиваем оба списка
        Collections.shuffle(videosWithLikedHashtags);
        Collections.shuffle(otherVideos);
        Collections.shuffle(likedVideos);
        
        // Вычисляем количество каждого типа видео
        int randomCount = (int) Math.round(totalSize * randomPercentage);
        int recommendedCount = totalSize - randomCount;
        
        // Убираем дубликаты между списками
        Set<UUID> alreadySelectedIds = new HashSet<>();

        // 1) Сначала добавляем лайкнутые видео (приоритет №1)
        List<Video> recommendedVideos = likedVideos.stream()
                .filter(v -> alreadySelectedIds.add(v.getId()))
                .collect(Collectors.toList());

        // 2) Затем видео с любимыми хэштегами (приоритет №2)
        recommendedVideos.addAll(
                videosWithLikedHashtags.stream()
                        .filter(v -> alreadySelectedIds.add(v.getId()))
                        .limit(Math.max(0, recommendedCount - recommendedVideos.size()))
                        .collect(Collectors.toList())
        );

        // 3) Случайные видео для разнообразия
        List<Video> randomVideos = otherVideos.stream()
                .filter(v -> alreadySelectedIds.add(v.getId()))
                .limit(randomCount)
                .collect(Collectors.toList());
        
        // Если не хватает рекомендованных, добавляем из остальных
        if (recommendedVideos.size() < recommendedCount && !otherVideos.isEmpty()) {
            int needed = recommendedCount - recommendedVideos.size();
            List<Video> additional = otherVideos.stream()
                    .filter(v -> !randomVideos.contains(v) && alreadySelectedIds.add(v.getId()))
                    .limit(needed)
                    .collect(Collectors.toList());
            recommendedVideos.addAll(additional);
        }
        
        // Если не хватает случайных, добавляем из рекомендованных
        if (randomVideos.size() < randomCount && !videosWithLikedHashtags.isEmpty()) {
            int needed = randomCount - randomVideos.size();
            List<Video> additional = videosWithLikedHashtags.stream()
                    .filter(v -> !recommendedVideos.contains(v) && alreadySelectedIds.add(v.getId()))
                    .limit(needed)
                    .collect(Collectors.toList());
            randomVideos.addAll(additional);
        }
        
        // Объединяем и перемешиваем для разнообразия
        List<Video> finalVideos = new ArrayList<>(recommendedVideos);
        finalVideos.addAll(randomVideos);
        Collections.shuffle(finalVideos);
        
        // Ограничиваем размером страницы
        finalVideos = finalVideos.stream()
                .limit(totalSize)
                .collect(Collectors.toList());
        
        log.info("Сформирована лента для пользователя {}: {} с любимыми хэштегами, {} случайных",
                user.getUsername(), recommendedVideos.size(), randomVideos.size());
        
        // Создаем Page из списка
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), finalVideos.size());
        List<Video> pageContent = start < finalVideos.size() 
                ? finalVideos.subList(start, end) 
                : Collections.emptyList();
        
        return new org.springframework.data.domain.PageImpl<>(
                pageContent,
                pageable,
                finalVideos.size()
        );
    }

}

