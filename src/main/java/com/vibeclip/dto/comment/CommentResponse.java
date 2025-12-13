package com.vibeclip.dto.comment;

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
public class CommentResponse {

    private UUID id;
    private UUID videoId;
    private UUID userId;
    private String username;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

