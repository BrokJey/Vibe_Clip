package com.vibeclip.dto.video;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Универсальный DTO для создания и обновления видео.
 * Все поля опциональны - валидация обязательности выполняется в сервисе при создании.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoRequest {

    @Size(max = 255, message = "Заголовок не должен превышать 255 символов.")
    private String title;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов.")
    private String description;

    @Size(max = 500, message = "URL-адрес видео не должен превышать 500 символов.")
    private String videoUrl;

    @Size(max = 500, message = "URL-адрес миниатюры не должен превышать 500 символов.")
    private String thumbnailUrl;

    @Min(value = 1, message = "Длительность должна быть не менее 1 секунды.")
    @Max(value = 180, message = "Продолжительность не должна превышать 180 секунд.")
    private Integer durationSeconds;

    private Set<@Size(max = 100) String> hashtags;
}

