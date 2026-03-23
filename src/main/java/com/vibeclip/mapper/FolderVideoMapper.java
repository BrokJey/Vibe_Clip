package com.vibeclip.mapper;

import com.vibeclip.dto.folder.FolderVideoResponse;
import com.vibeclip.entity.FolderVideo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VideoMapper.class})
public interface FolderVideoMapper {

    @Mapping(target = "video", source = "video")
    FolderVideoResponse toDTO(FolderVideo folderVideo);
}

