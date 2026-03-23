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

    List<Comment> findByVideoOrderByCreatedAtDesc(Video video);

    Page<Comment> findByVideoOrderByCreatedAtDesc(Video video, Pageable pageable);

    List<Comment> findByUserOrderByCreatedAtDesc(User user);

    long countByVideo(Video video);

    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId ORDER BY c.createdAt DESC")
    List<Comment> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") UUID videoId);

    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId ORDER BY c.createdAt DESC")
    Page<Comment> findByVideoIdOrderByCreatedAtDesc(@Param("videoId") UUID videoId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    void deleteByVideo(Video video);
    
    void deleteByUser(User user);
}

