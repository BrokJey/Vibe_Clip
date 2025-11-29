package com.vibeclip.dto.folder;

import com.vibeclip.dto.video.VideoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для видео в контексте папки (с позицией и score)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderVideoResponse {

    private UUID id;
    private Integer position;
    private Double score;
    private Boolean shown;
    private LocalDateTime addedAt;

    // Видео
    private VideoResponse video;
}


