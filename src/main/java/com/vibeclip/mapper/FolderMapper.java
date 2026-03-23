package com.vibeclip.mapper;

import com.vibeclip.dto.folder.FolderRequest;
import com.vibeclip.dto.folder.FolderResponse;
import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring", uses = {FolderPreferenceMapper.class})
public interface FolderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "folderVideos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "preference", source = "preference", qualifiedByName = "toPreferenceEntity")
    Folder fromDTO(FolderRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    @Mapping(target = "videoCount", source = "folderVideos", qualifiedByName = "countVideos")
    @Mapping(target = "preference", source = "preference", qualifiedByName = "toPreferenceRequest")
    FolderResponse toDTO(Folder folder);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "folderVideos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "preference", source = "preference", qualifiedByName = "updatePreferenceEntity")
    void updateEntity(@MappingTarget Folder folder, FolderRequest request);


    @Named("countVideos")
    default Integer countVideos(java.util.Set<?> folderVideos) {
        return folderVideos != null ? folderVideos.size() : 0;
    }
}

