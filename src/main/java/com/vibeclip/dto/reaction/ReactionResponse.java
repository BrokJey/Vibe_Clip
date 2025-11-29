package com.vibeclip.dto.reaction;

import com.vibeclip.entity.ReactionType;
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
public class ReactionResponse {

    private UUID id;
    private UUID userId;
    private UUID videoId;
    private ReactionType reactionType;
    private Integer watchDurationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


