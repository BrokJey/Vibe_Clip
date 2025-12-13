package com.vibeclip.mapper;

import com.vibeclip.dto.comment.CommentRequest;
import com.vibeclip.dto.comment.CommentResponse;
import com.vibeclip.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct маппер для Comment entity ↔ DTO
 */
@Mapper(componentModel = "spring")
public interface CommentMapper {

    /**
     * Преобразует CommentRequest в Comment entity (без user и video)
     * User и video должны быть установлены отдельно в сервисе
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "video", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Comment fromDTO(CommentRequest request);

    /**
     * Преобразует Comment entity в CommentResponse
     */
    @Mapping(target = "videoId", source = "video.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    CommentResponse toDTO(Comment comment);

    /**
     * Обновляет существующий Comment данными из CommentRequest
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "video", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Comment comment, CommentRequest request);
}

