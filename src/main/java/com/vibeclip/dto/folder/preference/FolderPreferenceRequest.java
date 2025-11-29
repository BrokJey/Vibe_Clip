package com.vibeclip.dto.folder.preference;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Универсальный DTO для настроек рекомендаций папки.
 * Используется как для запросов (создание/обновление), так и для ответов.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPreferenceRequest {

    /**
     * Разрешенные хэштеги (если пусто - все разрешены)
     */
    private Set<String> allowedHashtags;

    /**
     * Запрещенные хэштеги
     */
    private Set<String> blockedHashtags;

    /**
     * ID авторов, чьи видео разрешены (если пусто - все разрешены)
     */
    private Set<String> allowedAuthorIds;

    /**
     * ID авторов, чьи видео запрещены
     */
    private Set<String> blockedAuthorIds;

    /**
     * Минимальная длительность видео в секундах (null = без ограничений)
     */
    @Min(value = 1, message = "Минимальная прололжительность не должна быть меньше 1 секунды")
    @Max(value = 3600, message = "Минимальная продолжительность не должна превышать 3600 секунд.")
    private Integer minDurationSeconds;

    /**
     * Максимальная длительность видео в секундах (null = без ограничений)
     */
    @Min(value = 1, message = "Максимальная прололжительность не должна быть меньше 1 секунды")
    @Max(value = 3600, message = "Максимальная продолжительность не должна превышать 3600 секунд.")
    private Integer maxDurationSeconds;

    /**
     * Приоритет свежести контента (0.0 - 1.0, где 1.0 = только новые)
     */
    @DecimalMin(value = "0.0", message = "Вес свежести должен быть в пределах от 0,0 до 1,0.")
    @DecimalMax(value = "1.0", message = "Вес свежести должен быть в пределах от 0,0 до 1,0.")
    private Double freshnessWeight;

    /**
     * Приоритет популярности (0.0 - 1.0, где 1.0 = только популярные)
     */
    @DecimalMin(value = "0.0", message = "Вес популярности должен быть в диапазоне от 0,0 до 1,0.")
    @DecimalMax(value = "1.0", message = "Вес популярности должен быть в диапазоне от 0,0 до 1,0.")
    private Double popularityWeight;
}

