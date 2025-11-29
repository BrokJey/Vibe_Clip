package com.vibeclip.mapper;

import com.vibeclip.dto.folder.preference.FolderPreferenceRequest;
import com.vibeclip.entity.FolderPreference;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;


//MapStruct маппер для FolderPreference (Embeddable) ↔ DTO
@Mapper(componentModel = "spring")
public interface FolderPreferenceMapper {

     //Преобразует FolderPreferenceRequest в FolderPreference entity
    @Named("toPreferenceEntity")
    FolderPreference fromDTO(FolderPreferenceRequest request);

     //Преобразует FolderPreference entity в FolderPreferenceRequest
    @Named("toPreferenceRequest")
    FolderPreferenceRequest toDTO(FolderPreference preference);

    //Обновляет существующую FolderPreference данными из FolderPreferenceRequest
    @Named("updatePreferenceEntity")
    void updateEntity(@MappingTarget FolderPreference preference, FolderPreferenceRequest request);
}


