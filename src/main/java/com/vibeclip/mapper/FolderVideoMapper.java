package com.vibeclip.mapper;

import com.vibeclip.dto.folder.FolderVideoResponse;
import com.vibeclip.entity.FolderVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct маппер для FolderVideo entity ↔ DTO
 */
@Mapper(componentModel = "spring", uses = {VideoMapper.class})
public interface FolderVideoMapper {

    /**
     * Преобразует FolderVideo entity в FolderVideoResponse
     * Video преобразуется через VideoMapper автоматически
     */
    @Mapping(target = "video", source = "video")
    FolderVideoResponse toDTO(FolderVideo folderVideo);
}

