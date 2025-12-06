package com.vibeclip.service;

import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderPreference;
import com.vibeclip.entity.FolderVideo;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoStatus;
import com.vibeclip.repository.FolderVideoRepository;
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
import java.util.List;
import java.util.UUID;
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

        // Получаем кандидатов с учетом фильтров
        Pageable pageable = PageRequest.of(0, limit * 2); // Берем больше для ранжирования
        Page<Video> candidates = findCandidates(preference, pageable);

        // Ранжируем и выбираем лучшие
        List<FolderVideo> folderVideos = candidates.getContent().stream()
                .map(video -> {
                    double score = calculateScore(video, preference);
                    return FolderVideo.builder()
                            .folder(folder)
                            .video(video)
                            .score(score)
                            .position(0)
                            .shown(false)
                            .build();
                })
                .sorted((fv1, fv2) -> Double.compare(fv2.getScore(), fv1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        // Устанавливаем позиции
        for (int i = 0; i < folderVideos.size(); i++) {
            folderVideos.get(i).setPosition(i);
        }

        // Сохраняем в БД
        folderVideos.forEach(folderVideoRepository::save);

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
     * Получает ленту папки (непоказанные видео, отсортированные по score)
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

        return folderVideos.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

