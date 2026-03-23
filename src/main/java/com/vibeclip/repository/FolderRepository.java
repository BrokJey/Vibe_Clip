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

    List<Folder> findByOwnerAndStatus(User owner, FolderStatus status);

    List<Folder> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, FolderStatus status);

    boolean existsByNameAndOwner(String name, User owner);

    Optional<Folder> findByIdAndOwner(UUID id, User owner);

    Optional<Folder> findByIdAndOwnerAndStatus(UUID id, User owner, FolderStatus status);

    long countByOwnerAndStatus(User owner, FolderStatus status);

    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId AND f.status = :status")
    List<Folder> findByOwnerIdAndStatus(@Param("ownerId") UUID ownerId, @Param("status") FolderStatus status);
}


