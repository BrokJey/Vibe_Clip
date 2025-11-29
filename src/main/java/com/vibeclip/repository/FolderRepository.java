package com.vibeclip.repository;

import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderStatus;
import com.vibeclip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    /**
     * Находит все папки пользователя
     */
    List<Folder> findByOwnerAndStatus(User owner, FolderStatus status);

    /**
     * Находит все активные папки пользователя
     */
    List<Folder> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, FolderStatus status);

    /**
     * Проверяет существование папки по имени и владельцу
     */
    boolean existsByNameAndOwner(String name, User owner);

    /**
     * Находит папку по ID и владельцу
     */
    Optional<Folder> findByIdAndOwner(UUID id, User owner);

    /**
     * Находит папку по ID и владельцу со статусом
     */
    Optional<Folder> findByIdAndOwnerAndStatus(UUID id, User owner, FolderStatus status);

    /**
     * Подсчитывает количество активных папок пользователя
     */
    long countByOwnerAndStatus(User owner, FolderStatus status);

    /**
     * Находит папки по ID владельца
     */
    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId AND f.status = :status")
    List<Folder> findByOwnerIdAndStatus(@Param("ownerId") UUID ownerId, @Param("status") FolderStatus status);
}


