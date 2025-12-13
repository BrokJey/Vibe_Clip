package com.vibeclip.repository;

import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VideoMetricRepository extends JpaRepository<VideoMetric, UUID> {

    /**
     * Находит метрики по видео
     */
    Optional<VideoMetric> findByVideo(Video video);

    /**
     * Находит метрики по ID видео
     */
    Optional<VideoMetric> findByVideoId(UUID videoId);

    /**
     * Увеличивает счетчик просмотров
     */
    @Modifying
    @Query("UPDATE VideoMetric vm SET vm.viewCount = vm.viewCount + 1 WHERE vm.video.id = :videoId")
    void incrementViewCount(@Param("videoId") UUID videoId);

    /**
     * Увеличивает счетчик лайков
     */
    @Modifying
    @Query("UPDATE VideoMetric vm SET vm.likeCount = vm.likeCount + 1 WHERE vm.video.id = :videoId")
    void incrementLikeCount(@Param("videoId") UUID videoId);

    /**
     * Уменьшает счетчик лайков
     */
    @Modifying
    @Query("UPDATE VideoMetric vm SET vm.likeCount = vm.likeCount - 1 WHERE vm.video.id = :videoId AND vm.likeCount > 0")
    void decrementLikeCount(@Param("videoId") UUID videoId);

    /**
     * Увеличивает счетчик комментариев
     */
    @Modifying
    @Query("UPDATE VideoMetric vm SET vm.commentCount = vm.commentCount + 1 WHERE vm.video.id = :videoId")
    void incrementCommentCount(@Param("videoId") UUID videoId);

    /**
     * Уменьшает счетчик комментариев
     */
    @Modifying
    @Query("UPDATE VideoMetric vm SET vm.commentCount = vm.commentCount - 1 WHERE vm.video.id = :videoId AND vm.commentCount > 0")
    void decrementCommentCount(@Param("videoId") UUID videoId);

    /**
     * Увеличивает счетчик репостов
     */
    @Modifying
    @Query("UPDATE VideoMetric vm SET vm.shareCount = vm.shareCount + 1 WHERE vm.video.id = :videoId")
    void incrementShareCount(@Param("videoId") UUID videoId);
}


