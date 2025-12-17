package com.vibeclip.mapper;

import com.vibeclip.dto.video.VideoMetricsResponse;
import com.vibeclip.dto.video.VideoRequest;
import com.vibeclip.dto.video.VideoResponse;
import com.vibeclip.entity.Video;
import com.vibeclip.entity.VideoMetric;
import com.vibeclip.util.HashtagUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MapStruct маппер для Video entity ↔ DTO
 */
@Mapper(componentModel = "spring")
public interface VideoMapper {

    /**
     * Преобразует VideoRequest в Video entity (без author и status)
     * Author и status должны быть установлены отдельно в сервисе
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hashtags", source = "hashtags", qualifiedByName = "normalizeHashtags")
    Video fromDTO(VideoRequest request);

    /**
     * Нормализует множество хэштегов
     */
    @Named("normalizeHashtags")
    default Set<String> normalizeHashtags(Set<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return Set.of();
        }
        return hashtags.stream()
                .map(HashtagUtil::normalize)
                .filter(h -> h != null && !h.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Преобразует Video entity в VideoResponse
     */
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorUsername", source = "author.username")
    @Mapping(target = "metrics", ignore = true) // Метрики загружаются отдельно
    VideoResponse toDTO(Video video);

    /**
     * Обновляет существующее Video данными из VideoRequest
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "videoUrl", ignore = true) // URL не обновляется
    @Mapping(target = "durationSeconds", ignore = true) // Длительность не обновляется
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Video video, VideoRequest request);

    /**
     * Преобразует VideoMetric entity в VideoMetricsResponse
     */
    VideoMetricsResponse toMetricsResponse(VideoMetric metric);
}

