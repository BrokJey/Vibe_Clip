package com.vibeclip.repository;

import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    Page<Video> findByAuthorAndStatus(User author, VideoStatus status, Pageable pageable);

    Page<Video> findByStatus(VideoStatus status, Pageable pageable);

    List<Video> findByAuthorAndStatus(User author, VideoStatus status);

    boolean existsByAuthor(User author);

    @Query("SELECT v FROM Video v JOIN v.hashtags h WHERE h = :hashtag AND v.status = :status")
    Page<Video> findByHashtag(@Param("hashtag") String hashtag, @Param("status") VideoStatus status, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Video v JOIN v.hashtags h WHERE h IN :hashtags AND v.status = :status")
    Page<Video> findByHashtagsIn(@Param("hashtags") List<String> hashtags, @Param("status") VideoStatus status, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = :status AND NOT EXISTS " +
           "(SELECT 1 FROM v.hashtags h WHERE h IN :blockedHashtags)")
    Page<Video> findByStatusExcludingHashtags(@Param("status") VideoStatus status, 
                                               @Param("blockedHashtags") List<String> blockedHashtags, 
                                               Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = :status AND v.author.id NOT IN :blockedAuthorIds")
    Page<Video> findByStatusExcludingAuthors(@Param("status") VideoStatus status, 
                                            @Param("blockedAuthorIds") List<UUID> blockedAuthorIds, 
                                            Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = :status AND " +
           "(:minDuration IS NULL OR v.durationSeconds >= :minDuration) AND " +
           "(:maxDuration IS NULL OR v.durationSeconds <= :maxDuration)")
    Page<Video> findByStatusAndDurationBetween(@Param("status") VideoStatus status,
                                               @Param("minDuration") Integer minDuration,
                                               @Param("maxDuration") Integer maxDuration,
                                               Pageable pageable);

    Optional<Video> findByIdAndAuthorId(UUID videoId, UUID authorId);
}


