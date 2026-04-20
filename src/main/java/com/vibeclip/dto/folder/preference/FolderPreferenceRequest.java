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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPreferenceRequest {

    private Set<String> allowedHashtags;

    private Set<String> blockedHashtags;

    private Set<String> allowedAuthorIds;

    private Set<String> blockedAuthorIds;

    @Min(value = 1, message = "Минимальная прололжительность не должна быть меньше 1 секунды")
    @Max(value = 3600, message = "Минимальная продолжительность не должна превышать 3600 секунд.")
    private Integer minDurationSeconds;

    @Min(value = 1, message = "Максимальная прололжительность не должна быть меньше 1 секунды")
    @Max(value = 3600, message = "Максимальная продолжительность не должна превышать 3600 секунд.")
    private Integer maxDurationSeconds;

    @DecimalMin(value = "0.0", message = "Вес свежести должен быть в пределах от 0,0 до 1,0.")
    @DecimalMax(value = "1.0", message = "Вес свежести должен быть в пределах от 0,0 до 1,0.")
    private Double freshnessWeight;

    @DecimalMin(value = "0.0", message = "Вес популярности должен быть в диапазоне от 0,0 до 1,0.")
    @DecimalMax(value = "1.0", message = "Вес популярности должен быть в диапазоне от 0,0 до 1,0.")
    private Double popularityWeight;
}

