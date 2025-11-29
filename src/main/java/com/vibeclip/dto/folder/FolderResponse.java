package com.vibeclip.dto.folder;

import com.vibeclip.dto.folder.preference.FolderPreferenceRequest;
import com.vibeclip.entity.FolderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {

    private UUID id;
    private String name;
    private String description;
    private FolderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Информация о владельце
    private UUID ownerId;
    private String ownerUsername;

    // Настройки рекомендаций
    private FolderPreferenceRequest preference;

    // Статистика (опционально)
    private Integer videoCount;
}

