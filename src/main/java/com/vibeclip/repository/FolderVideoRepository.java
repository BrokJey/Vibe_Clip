package com.vibeclip.repository;

import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderVideo;
import com.vibeclip.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderVideoRepository extends JpaRepository<FolderVideo, UUID> {

    List<FolderVideo> findByFolderOrderByPositionAsc(Folder folder);

    List<FolderVideo> findByFolderOrderByScoreDesc(Folder folder);

    Page<FolderVideo> findByFolderOrderByPositionAsc(Folder folder, Pageable pageable);

    List<FolderVideo> findByFolderAndShownFalseOrderByScoreDesc(Folder folder);

    List<FolderVideo> findByFolderAndShownFalse(Folder folder);

    Page<FolderVideo> findByFolderAndShownFalseOrderByScoreDesc(Folder folder, Pageable pageable);

    boolean existsByFolderAndVideo(Folder folder, Video video);

    Optional<FolderVideo> findByFolderAndVideo(Folder folder, Video video);

    List<FolderVideo> findByFolder(Folder folder);

    void deleteByFolder(Folder folder);

    void deleteByFolderAndVideo(Folder folder, Video video);

    @Modifying
    @Query("UPDATE FolderVideo fv SET fv.shown = true WHERE fv.folder.id = :folderId AND fv.video.id = :videoId")
    void markAsShown(@Param("folderId") UUID folderId, @Param("videoId") UUID videoId);

    @Modifying
    @Query("UPDATE FolderVideo fv SET fv.score = :score WHERE fv.id = :id")
    void updateScore(@Param("id") UUID id, @Param("score") Double score);

    @Modifying
    @Query("UPDATE FolderVideo fv SET fv.position = :position WHERE fv.id = :id")
    void updatePosition(@Param("id") UUID id, @Param("position") Integer position);

    @Query("SELECT COALESCE(MAX(fv.position), 0) FROM FolderVideo fv WHERE fv.folder.id = :folderId")
    Integer findMaxPositionByFolderId(@Param("folderId") UUID folderId);

    long countByFolder(Folder folder);

    void deleteByVideo(Video video);
}


