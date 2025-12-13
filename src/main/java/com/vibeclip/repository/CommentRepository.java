package com.vibeclip.repository;

import com.vibeclip.entity.Comment;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Находит все комментарии к видео, отсортированные по дате создания (новые сначала)
     */
    List<Comment> findByVideoOrderByCreatedAtDesc(Video video);

    /**
     * Находит все комментарии к видео с пагинацией
     */
    Page<Comment> findByVideoOrderByCreatedAtDesc(Video video, Pageable pageable);

    /**
     * Находит все комментарии пользователя
     */
    List<Comment> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Подсчитывает количество комментариев к видео
     */
    long countByVideo(Video video);

    /**
     * Находит комментарии по ID видео
     */
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId ORDER BY c.createdAt DESC")
    List<Comment> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") UUID videoId);

    /**
     * Находит комментарии по ID видео с пагинацией
     */
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId ORDER BY c.createdAt DESC")
    Page<Comment> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") UUID videoId, Pageable pageable);

    /**
     * Находит комментарии по ID пользователя
     */
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    /**
     * Удаляет все комментарии к видео
     */
    void deleteByVideo(Video video);

    /**
     * Удаляет все комментарии пользователя
     */
    void deleteByUser(User user);
}

