package com.vibeclip.dto.video;

import com.vibeclip.entity.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse {

    private UUID id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private VideoStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Информация об авторе
    private UUID authorId;
    private String authorUsername;

    // Хэштеги
    private Set<String> hashtags;

    // Метрики (опционально, можно загружать отдельно)
    private VideoMetricsResponse metrics;
}


