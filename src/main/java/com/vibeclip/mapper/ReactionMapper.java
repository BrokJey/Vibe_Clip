package com.vibeclip.mapper;

import com.vibeclip.dto.reaction.ReactionRequest;
import com.vibeclip.dto.reaction.ReactionResponse;
import com.vibeclip.entity.Reaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ReactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "video", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Reaction fromDTO(ReactionRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "videoId", source = "video.id")
    ReactionResponse toDTO(Reaction reaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "video", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Reaction reaction, ReactionRequest request);
}

