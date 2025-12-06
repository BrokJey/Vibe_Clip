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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final VideoRepository videoRepository;
    private final VideoMetricService videoMetricService;
    private final ReactionMapper reactionMapper;

    @Transactional
    public ReactionResponse create(ReactionRequest request, User user) {
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> {
                    log.error("Видео не найдено {} ", request.getVideoId());
                    return new IllegalArgumentException("Видео не найдено: " + request.getVideoId());
                });

        // Проверяем, не существует ли уже такая реакция
        Reaction existing = reactionRepository
                .findByUserAndVideoAndReactionType(user, video, request.getReactionType())
                .orElse(null);

        if (existing != null) {
            // Если реакция уже есть, обновляем её
            if (request.getWatchDurationSeconds() != null) {
                existing.setWatchDurationSeconds(request.getWatchDurationSeconds());
            }
            Reaction updated = reactionRepository.save(existing);
            return reactionMapper.toDTO(updated);
        }

        // Создаем новую реакцию
        Reaction reaction = reactionMapper.fromDTO(request);
        reaction.setUser(user);
        reaction.setVideo(video);

        Reaction saved = reactionRepository.save(reaction);

        // Обновляем метрики видео
        updateVideoMetrics(video.getId(), request.getReactionType(), true);

        return reactionMapper.toDTO(saved);
    }

    @Transactional
    public void delete(UUID videoId, ReactionType reactionType, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.error("Видео не найдено {} ", videoId);
                    return new IllegalArgumentException("Видео не найдено: " + videoId);
                });

        reactionRepository.deleteByUserAndVideoAndReactionType(user, video, reactionType);

        // Обновляем метрики видео
        updateVideoMetrics(videoId, reactionType, false);
    }

    public List<ReactionResponse> getByUserAndVideo(User user, UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.error("Видео не найдено {} ", videoId);
                    return new IllegalArgumentException("Видео не найдено: " + videoId);
                });

        List<Reaction> reactions = reactionRepository.findByUserAndVideo(user, video);
        return reactions.stream()
                .map(reactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public boolean hasReaction(User user, UUID videoId, ReactionType reactionType) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.error("Видео не найдено {} ", videoId);
                    return new IllegalArgumentException("Видео не найдено: " + videoId);
                });
        return reactionRepository.existsByUserAndVideoAndReactionType(user, video, reactionType);
    }

    public List<ReactionResponse> getByUser(User user, ReactionType reactionType) {
        List<Reaction> reactions = reactionRepository.findByUserAndReactionType(user, reactionType);
        return reactions.stream()
                .map(reactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Обновляет метрики видео в зависимости от типа реакции
    private void updateVideoMetrics(UUID videoId, ReactionType reactionType, boolean increment) {
        switch (reactionType) {
            case LIKE:
                if (increment) {
                    videoMetricService.incrementLikeCount(videoId);
                } else {
                    videoMetricService.decrementLikeCount(videoId);
                }
                break;
            case VIEW:
                if (increment) {
                    videoMetricService.incrementViewCount(videoId);
                }
                break;
            case SHARE:
                if (increment) {
                    videoMetricService.incrementShareCount(videoId);
                }
                break;
            // DISLIKE, REPORT, SKIP не влияют на метрики
        }
    }
}

