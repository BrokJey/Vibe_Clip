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

    /**
     * Находит все видео в папке, отсортированные по позиции
     */
    List<FolderVideo> findByFolderOrderByPositionAsc(Folder folder);

    /**
     * Находит все видео в папке, отсортированные по score (для рекомендаций)
     */
    List<FolderVideo> findByFolderOrderByScoreDesc(Folder folder);

    /**
     * Находит видео в папке с пагинацией
     */
    Page<FolderVideo> findByFolderOrderByPositionAsc(Folder folder, Pageable pageable);

    /**
     * Находит видео в папке, которые еще не показывались
     */
    List<FolderVideo> findByFolderAndShownFalseOrderByScoreDesc(Folder folder);

    /**
     * Находит видео в папке, которые еще не показывались, с пагинацией
     */
    Page<FolderVideo> findByFolderAndShownFalseOrderByScoreDesc(Folder folder, Pageable pageable);

    /**
     * Проверяет существование связи между папкой и видео
     */
    boolean existsByFolderAndVideo(Folder folder, Video video);

    /**
     * Находит связь по папке и видео
     */
    Optional<FolderVideo> findByFolderAndVideo(Folder folder, Video video);

    /**
     * Находит все связи по папке
     */
    List<FolderVideo> findByFolder(Folder folder);

    /**
     * Удаляет все связи по папке
     */
    void deleteByFolder(Folder folder);

    /**
     * Удаляет связь по папке и видео
     */
    void deleteByFolderAndVideo(Folder folder, Video video);

    /**
     * Помечает видео как показанное
     */
    @Modifying
    @Query("UPDATE FolderVideo fv SET fv.shown = true WHERE fv.folder.id = :folderId AND fv.video.id = :videoId")
    void markAsShown(@Param("folderId") UUID folderId, @Param("videoId") UUID videoId);

    /**
     * Обновляет score для видео в папке
     */
    @Modifying
    @Query("UPDATE FolderVideo fv SET fv.score = :score WHERE fv.id = :id")
    void updateScore(@Param("id") UUID id, @Param("score") Double score);

    /**
     * Обновляет позицию для видео в папке
     */
    @Modifying
    @Query("UPDATE FolderVideo fv SET fv.position = :position WHERE fv.id = :id")
    void updatePosition(@Param("id") UUID id, @Param("position") Integer position);

    /**
     * Находит максимальную позицию в папке
     */
    @Query("SELECT COALESCE(MAX(fv.position), 0) FROM FolderVideo fv WHERE fv.folder.id = :folderId")
    Integer findMaxPositionByFolderId(@Param("folderId") UUID folderId);

    /**
     * Подсчитывает количество видео в папке
     */
    long countByFolder(Folder folder);

    /**
     * Удаляет все связи по видео
     */
    void deleteByVideo(Video video);
}


