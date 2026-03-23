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

    Optional<Reaction> findByUserAndVideoAndReactionType(User user, Video video, ReactionType reactionType);

    boolean existsByUserAndVideoAndReactionType(User user, Video video, ReactionType reactionType);

    List<Reaction> findByUserAndVideo(User user, Video video);

    List<Reaction> findByVideoAndReactionType(Video video, ReactionType reactionType);

    long countByVideoAndReactionType(Video video, ReactionType reactionType);

    List<Reaction> findByUserAndReactionType(User user, ReactionType reactionType);

    List<Reaction> findByUserAndReactionTypeOrderByCreatedAtDesc(User user, ReactionType reactionType);

    void deleteByUserAndVideoAndReactionType(User user, Video video, ReactionType reactionType);

    @Query("SELECT r FROM Reaction r WHERE r.video.id = :videoId ORDER BY r.createdAt DESC")
    List<Reaction> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") UUID videoId);

    List<Reaction> findByUserOrderByCreatedAtDesc(User user);

    long countByVideo(Video video);

    @Query("SELECT r FROM Reaction r WHERE r.user.id = :userId AND r.video.id = :videoId")
    List<Reaction> findByUserIdAndVideoId(@Param("userId") UUID userId, @Param("videoId") UUID videoId);

    void deleteByVideo(Video video);
}


