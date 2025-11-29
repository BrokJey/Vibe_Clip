package com.vibeclip.dto.reaction;

import com.vibeclip.entity.ReactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionRequest {

    @NotNull(message = "Требуется ID видео.")
    private UUID videoId;

    @NotNull(message = "Требуется тип реакции")
    private ReactionType reactionType;

    /**
     * Длительность просмотра в секундах (только для VIEW)
     */
    @PositiveOrZero(message = "Длительность просмотра должна быть положительной или нулевой.")
    private Integer watchDurationSeconds;
}


