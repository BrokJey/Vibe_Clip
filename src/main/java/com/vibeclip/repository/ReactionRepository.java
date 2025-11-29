package com.vibeclip.repository;

import com.vibeclip.entity.Reaction;
import com.vibeclip.entity.ReactionType;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    /**
     * Находит реакцию по пользователю, видео и типу
     */
    Optional<Reaction> findByUserAndVideoAndReactionType(User user, Video video, ReactionType reactionType);

    /**
     * Проверяет существование реакции
     */
    boolean existsByUserAndVideoAndReactionType(User user, Video video, ReactionType reactionType);

    /**
     * Находит все реакции пользователя на видео
     */
    List<Reaction> findByUserAndVideo(User user, Video video);

    /**
     * Находит все реакции определенного типа на видео
     */
    List<Reaction> findByVideoAndReactionType(Video video, ReactionType reactionType);

    /**
     * Подсчитывает количество реакций определенного типа на видео
     */
    long countByVideoAndReactionType(Video video, ReactionType reactionType);

    /**
     * Находит все лайки пользователя
     */
    List<Reaction> findByUserAndReactionType(User user, ReactionType reactionType);

    /**
     * Находит все просмотры пользователя
     */
    List<Reaction> findByUserAndReactionTypeOrderByCreatedAtDesc(User user, ReactionType reactionType);

    /**
     * Удаляет реакцию по пользователю, видео и типу
     */
    void deleteByUserAndVideoAndReactionType(User user, Video video, ReactionType reactionType);

    /**
     * Находит все реакции на видео с пагинацией
     */
    @Query("SELECT r FROM Reaction r WHERE r.video.id = :videoId ORDER BY r.createdAt DESC")
    List<Reaction> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") UUID videoId);

    /**
     * Находит все реакции пользователя
     */
    List<Reaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Подсчитывает общее количество реакций на видео
     */
    long countByVideo(Video video);

    /**
     * Находит реакции по ID пользователя и видео
     */
    @Query("SELECT r FROM Reaction r WHERE r.user.id = :userId AND r.video.id = :videoId")
    List<Reaction> findByUserIdAndVideoId(@Param("userId") UUID userId, @Param("videoId") UUID videoId);
}


