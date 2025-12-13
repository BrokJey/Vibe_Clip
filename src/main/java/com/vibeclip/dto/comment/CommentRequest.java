package com.vibeclip.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {

    @NotNull(message = "Требуется ID видео.")
    private UUID videoId;

    @NotBlank(message = "Текст комментария не может быть пустым.")
    @Size(min = 1, max = 2000, message = "Текст комментария должен быть от 1 до 2000 символов.")
    private String text;
}

