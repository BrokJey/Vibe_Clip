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


// Сервис для формирования рекомендаций и лент папок
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final VideoRepository videoRepository;
    private final FolderVideoRepository folderVideoRepository;
    private final VideoMetricRepository videoMetricRepository;
    private final ReactionRepository reactionRepository;


    // Формирует ленту рекомендаций для папки на основе её настроек
    @Transactional
    public List<FolderVideo> generateFeedForFolder(Folder folder, int limit) {
        // Делаем preference effectively final для использования в лямбде
        final FolderPreference preference = folder.getPreference() != null 
                ? folder.getPreference()
                : FolderPreference.builder()
                        .freshnessWeight(0.5)
                        .popularityWeight(0.5)
                        .build();

        // Получаем список уже существующих видео в папке (чтобы не дублировать)
        List<FolderVideo> existingFolderVideos = folderVideoRepository.findByFolder(folder);
        java.util.Set<UUID> existingVideoIds = existingFolderVideos.stream()
                .map(fv -> fv.getVideo().getId())
                .collect(Collectors.toSet());

        // Получаем кандидатов с учетом фильтров
        Pageable pageable = PageRequest.of(0, limit * 3); // Берем больше для ранжирования и фильтрации
        Page<Video> candidates = findCandidates(preference, pageable);

        // Фильтруем кандидатов, исключая уже существующие видео
        List<Video> newCandidates = candidates.getContent().stream()
                .filter(video -> !existingVideoIds.contains(video.getId()))
                .collect(Collectors.toList());
        log.debug("После исключения существующих видео осталось {} новых кандидатов", newCandidates.size());

        // Перемешиваем кандидатов случайным образом
        java.util.Collections.shuffle(newCandidates);
        
        // Ограничиваем лимитом после перемешивания
        newCandidates = newCandidates.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // Создаем FolderVideo записи в случайном порядке
        int maxPosition = existingFolderVideos.stream()
                .mapToInt(FolderVideo::getPosition)
                .max()
                .orElse(-1);
        
        List<FolderVideo> folderVideos = new java.util.ArrayList<>();
        for (int i = 0; i < newCandidates.size(); i++) {
            FolderVideo folderVideo = FolderVideo.builder()
                    .folder(folder)
                    .video(newCandidates.get(i))
                    .score(1.0) // Простой score, не используется для ранжирования
                    .position(maxPosition + 1 + i)
                    .shown(false)
                    .build();
            folderVideos.add(folderVideo);
        }

        // Сохраняем в БД только новые записи
        folderVideos.forEach(folderVideoRepository::save);

        log.info("Сгенерировано {} новых рекомендаций для папки {}", folderVideos.size(), folder.getId());
        return folderVideos;
    }

    // Находит кандидатов для рекомендаций на основе настроек папки
    private Page<Video> findCandidates(FolderPreference preference, Pageable pageable) {
        // Начинаем с опубликованных видео
        Page<Video> videos = videoRepository.findByStatus(VideoStatus.PUBLISHED, pageable);

        // Применяем фильтры
        if (preference.getBlockedHashtags() != null && !preference.getBlockedHashtags().isEmpty()) {
            videos = videoRepository.findByStatusExcludingHashtags(
                    VideoStatus.PUBLISHED,
                    preference.getBlockedHashtags().stream().collect(Collectors.toList()),
                    pageable
            );
        }

        if (preference.getBlockedAuthorIds() != null && !preference.getBlockedAuthorIds().isEmpty()) {
            List<UUID> blockedIds = preference.getBlockedAuthorIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            videos = videoRepository.findByStatusExcludingAuthors(
                    VideoStatus.PUBLISHED,
                    blockedIds,
                    pageable
            );
        }

        // Фильтр по длительности
        if (preference.getMinDurationSeconds() != null || preference.getMaxDurationSeconds() != null) {
            videos = videoRepository.findByStatusAndDurationBetween(
                    VideoStatus.PUBLISHED,
                    preference.getMinDurationSeconds(),
                    preference.getMaxDurationSeconds(),
                    pageable
            );
        }

        // Фильтр по разрешенным хэштегам (если указаны)
        if (preference.getAllowedHashtags() != null && !preference.getAllowedHashtags().isEmpty()) {
            videos = videoRepository.findByHashtagsIn(
                    preference.getAllowedHashtags().stream().collect(Collectors.toList()),
                    VideoStatus.PUBLISHED,
                    pageable
            );
        }

        return videos;
    }

    // Вычисляет score для видео на основе настроек папки
    private double calculateScore(Video video, FolderPreference preference) {
        double freshnessScore = calculateFreshnessScore(video);
        double popularityScore = calculatePopularityScore(video);

        double freshnessWeight = preference.getFreshnessWeight() != null ? preference.getFreshnessWeight() : 0.5;
        double popularityWeight = preference.getPopularityWeight() != null ? preference.getPopularityWeight() : 0.5;

        // Нормализуем веса (чтобы сумма была 1.0)
        double totalWeight = freshnessWeight + popularityWeight;
        if (totalWeight > 0) {
            freshnessWeight /= totalWeight;
            popularityWeight /= totalWeight;
        }

        return (freshnessScore * freshnessWeight) + (popularityScore * popularityWeight);
    }

    /* Вычисляет score свежести (0.0 - 1.0)
     * Новые видео получают больший score */
    private double calculateFreshnessScore(Video video) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = video.getCreatedAt();

        long daysSinceCreation = java.time.Duration.between(createdAt, now).toDays();

        // Видео младше 7 дней получают максимальный score
        if (daysSinceCreation <= 7) {
            return 1.0;
        }
        // Видео старше 30 дней получают минимальный score
        if (daysSinceCreation >= 30) {
            return 0.1;
        }
        // Линейная интерполяция между 7 и 30 днями
        return 1.0 - ((daysSinceCreation - 7) / 23.0) * 0.9;
    }

    /* Вычисляет score популярности (0.0 - 1.0)
       Популярные видео получают больший score */
    private double calculatePopularityScore(Video video) {
        return videoMetricRepository.findByVideoId(video.getId())
                .map(metric -> {
                    // Комбинируем метрики: лайки, просмотры, комментарии
                    long totalEngagement = metric.getLikeCount() + 
                                          (metric.getViewCount() / 10) + // Просмотры весят меньше
                                          (metric.getCommentCount() * 2); // Комментарии весят больше

                    // Нормализуем до 0.0 - 1.0 (логарифмическая шкала)
                    if (totalEngagement == 0) {
                        return 0.1; // Минимальный score для видео без взаимодействий
                    }
                    return Math.min(1.0, Math.log10(totalEngagement + 1) / 5.0);
                })
                .orElse(0.1);
    }

    // Обновляет score для всех видео в папке
    @Transactional
    public void updateScoresForFolder(Folder folder) {
        List<FolderVideo> folderVideos = folderVideoRepository.findByFolder(folder);
        FolderPreference preference = folder.getPreference();

        if (preference == null) {
            preference = FolderPreference.builder()
                    .freshnessWeight(0.5)
                    .popularityWeight(0.5)
                    .build();
        }

        for (FolderVideo folderVideo : folderVideos) {
            double newScore = calculateScore(folderVideo.getVideo(), preference);
            folderVideoRepository.updateScore(folderVideo.getId(), newScore);
        }
    }

    /**
     * Получает ленту папки (непоказанные видео, в случайном порядке)
     */
    public List<FolderVideo> getFeedForFolder(Folder folder, int limit) {
        List<FolderVideo> folderVideos = folderVideoRepository
                .findByFolderAndShownFalseOrderByScoreDesc(folder);

        if (folderVideos.isEmpty() || folderVideos.size() < limit) {
            // Если непоказанных видео мало, генерируем новые рекомендации
            generateFeedForFolder(folder, limit);
            folderVideos = folderVideoRepository
                    .findByFolderAndShownFalseOrderByScoreDesc(folder);
        }

        // Перемешиваем видео случайным образом и ограничиваем лимитом
        java.util.Collections.shuffle(folderVideos);
        return folderVideos.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получает рекомендованную ленту для пользователя на основе его лайков
     * Если пользователь лайкал видео с определенными хэштегами, то видео с такими же хэштегами
     * будут показываться чаще. Также добавляются случайные видео для разнообразия.
     * 
     * @param user пользователь для которого формируется лента
     * @param pageable пагинация
     * @param randomPercentage процент случайных видео (0.0 - 1.0), по умолчанию 0.25 (25%)
     * @return страница с рекомендованными видео
     */
    public Page<Video> getRecommendedFeed(User user, Pageable pageable, Double randomPercentage) {
        if (randomPercentage == null) {
            randomPercentage = 0.25; // По умолчанию 25% случайных видео
        }

        // Получаем все лайки пользователя
        List<Reaction> userLikes = reactionRepository.findByUserAndReactionType(user, ReactionType.LIKE);
        
        // Анализируем хэштеги из лайкнутых видео
        Map<String, Integer> hashtagWeights = analyzeHashtagsFromLikes(userLikes);
        
        // Получаем все опубликованные видео (кандидаты)
        int totalSize = pageable.getPageSize();
        
        // Получаем больше кандидатов для ранжирования
        Pageable extendedPageable = PageRequest.of(0, totalSize * 5);
        Page<Video> allVideos = videoRepository.findByStatus(VideoStatus.PUBLISHED, extendedPageable);
        
        // Исключаем видео, которые пользователь уже лайкал
        Set<UUID> likedVideoIds = userLikes.stream()
                .map(reaction -> reaction.getVideo().getId())
                .collect(Collectors.toSet());
        
        List<Video> candidates = allVideos.getContent().stream()
                .filter(video -> !likedVideoIds.contains(video.getId())) // Исключаем уже лайкнутые
                .collect(Collectors.toList());
        
        // Если у пользователя нет лайков или нет предпочтений, возвращаем случайную ленту
        if (hashtagWeights.isEmpty() || candidates.isEmpty()) {
            Collections.shuffle(candidates);
            List<Video> randomVideos = candidates.stream()
                    .limit(totalSize)
                    .collect(Collectors.toList());
            
            log.info("Сформирована случайная лента для пользователя {} (нет лайков или предпочтений)", 
                    user.getUsername());
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), randomVideos.size());
            List<Video> pageContent = start < randomVideos.size() 
                    ? randomVideos.subList(start, end) 
                    : Collections.emptyList();
            
            return new org.springframework.data.domain.PageImpl<>(
                    pageContent,
                    pageable,
                    randomVideos.size()
            );
        }
        
        // Ранжируем видео на основе хэштегов
        List<VideoWithScore> scoredVideos = candidates.stream()
                .map(video -> {
                    double score = calculateHashtagScore(video, hashtagWeights);
                    return new VideoWithScore(video, score);
                })
                .sorted((v1, v2) -> Double.compare(v2.score, v1.score))
                .collect(Collectors.toList());
        
        // Если все видео имеют score = 0, возвращаем случайную ленту
        boolean allZeroScore = scoredVideos.stream().allMatch(vws -> vws.score == 0.0);
        if (allZeroScore) {
            Collections.shuffle(candidates);
            List<Video> randomVideos = candidates.stream()
                    .limit(totalSize)
                    .collect(Collectors.toList());
            
            log.info("Сформирована случайная лента для пользователя {} (все видео имеют score = 0)", 
                    user.getUsername());
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), randomVideos.size());
            List<Video> pageContent = start < randomVideos.size() 
                    ? randomVideos.subList(start, end) 
                    : Collections.emptyList();
            
            return new org.springframework.data.domain.PageImpl<>(
                    pageContent,
                    pageable,
                    randomVideos.size()
            );
        }
        
        // Разделяем на рекомендованные и случайные
        int randomCount = (int) Math.round(totalSize * randomPercentage);
        int recommendedCount = totalSize - randomCount;
        
        // Берем рекомендованные видео (с положительным score)
        List<Video> recommendedVideos = scoredVideos.stream()
                .filter(vws -> vws.score > 0.0) // Только видео с положительным score
                .limit(recommendedCount)
                .map(vws -> vws.video)
                .collect(Collectors.toList());
        
        // Добавляем случайные видео для разнообразия
        List<Video> randomVideos = new ArrayList<>();
        if (randomCount > 0) {
            List<Video> availableForRandom = candidates.stream()
                    .filter(video -> !recommendedVideos.contains(video))
                    .collect(Collectors.toList());
            
            Collections.shuffle(availableForRandom);
            randomVideos = availableForRandom.stream()
                    .limit(randomCount)
                    .collect(Collectors.toList());
        }
        
        // Объединяем рекомендованные и случайные видео
        List<Video> finalVideos = new ArrayList<>(recommendedVideos);
        finalVideos.addAll(randomVideos);
        Collections.shuffle(finalVideos); // Перемешиваем для разнообразия
        
        // Ограничиваем размером страницы
        finalVideos = finalVideos.stream()
                .limit(totalSize)
                .collect(Collectors.toList());
        
        log.info("Сформирована рекомендованная лента для пользователя {}: {} рекомендованных, {} случайных",
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

    /**
     * Анализирует хэштеги из лайкнутых видео и возвращает веса хэштегов
     * Чем чаще пользователь лайкал видео с определенным хэштегом, тем больше вес
     */
    private Map<String, Integer> analyzeHashtagsFromLikes(List<Reaction> likes) {
        Map<String, Integer> hashtagWeights = new HashMap<>();
        
        for (Reaction reaction : likes) {
            Video video = reaction.getVideo();
            if (video.getHashtags() != null) {
                for (String hashtag : video.getHashtags()) {
                    hashtagWeights.put(hashtag, hashtagWeights.getOrDefault(hashtag, 0) + 1);
                }
            }
        }
        
        log.debug("Проанализировано {} лайков, найдено {} уникальных хэштегов", 
                likes.size(), hashtagWeights.size());
        
        return hashtagWeights;
    }

    /**
     * Вычисляет score видео на основе весов хэштегов
     * Видео с хэштегами, которые пользователь часто лайкал, получают больший score
     */
    private double calculateHashtagScore(Video video, Map<String, Integer> hashtagWeights) {
        if (hashtagWeights.isEmpty() || video.getHashtags() == null || video.getHashtags().isEmpty()) {
            return 0.0; // Если нет данных о предпочтениях или хэштегов, score = 0
        }
        
        double totalScore = 0.0;
        int matchingHashtags = 0;
        
        for (String hashtag : video.getHashtags()) {
            if (hashtagWeights.containsKey(hashtag)) {
                int weight = hashtagWeights.get(hashtag);
                totalScore += weight;
                matchingHashtags++;
            }
        }
        
        if (matchingHashtags == 0) {
            return 0.0;
        }
        
        // Нормализуем score: средний вес хэштегов * количество совпадающих хэштегов
        double avgWeight = totalScore / matchingHashtags;
        return avgWeight * matchingHashtags;
    }

    /**
     * Вспомогательный класс для хранения видео с его score
     */
    private static class VideoWithScore {
        final Video video;
        final double score;

        VideoWithScore(Video video, double score) {
            this.video = video;
            this.score = score;
        }
    }
}

