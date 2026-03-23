package com.vibeclip.mapper;

import com.vibeclip.dto.folder.preference.FolderPreferenceRequest;
import com.vibeclip.entity.FolderPreference;
import com.vibeclip.util.HashtagUtil;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface FolderPreferenceMapper {

    @Named("toPreferenceEntity")
    default FolderPreference fromDTO(FolderPreferenceRequest request) {
        if (request == null) {
            return null;
        }
        
        FolderPreference preference = new FolderPreference();
        preference.setAllowedHashtags(normalizeHashtags(request.getAllowedHashtags()));
        preference.setBlockedHashtags(normalizeHashtags(request.getBlockedHashtags()));
        preference.setAllowedAuthorIds(request.getAllowedAuthorIds());
        preference.setBlockedAuthorIds(request.getBlockedAuthorIds());
        preference.setMinDurationSeconds(request.getMinDurationSeconds());
        preference.setMaxDurationSeconds(request.getMaxDurationSeconds());
        preference.setFreshnessWeight(request.getFreshnessWeight());
        preference.setPopularityWeight(request.getPopularityWeight());
        
        return preference;
    }

    @Named("toPreferenceRequest")
    FolderPreferenceRequest toDTO(FolderPreference preference);

    @Named("updatePreferenceEntity")
    default void updateEntity(@MappingTarget FolderPreference preference, FolderPreferenceRequest request) {
        if (request == null) {
            return;
        }
        
        if (request.getAllowedHashtags() != null) {
            preference.setAllowedHashtags(normalizeHashtags(request.getAllowedHashtags()));
        }
        if (request.getBlockedHashtags() != null) {
            preference.setBlockedHashtags(normalizeHashtags(request.getBlockedHashtags()));
        }
        if (request.getAllowedAuthorIds() != null) {
            preference.setAllowedAuthorIds(request.getAllowedAuthorIds());
        }
        if (request.getBlockedAuthorIds() != null) {
            preference.setBlockedAuthorIds(request.getBlockedAuthorIds());
        }
        if (request.getMinDurationSeconds() != null) {
            preference.setMinDurationSeconds(request.getMinDurationSeconds());
        }
        if (request.getMaxDurationSeconds() != null) {
            preference.setMaxDurationSeconds(request.getMaxDurationSeconds());
        }
        if (request.getFreshnessWeight() != null) {
            preference.setFreshnessWeight(request.getFreshnessWeight());
        }
        if (request.getPopularityWeight() != null) {
            preference.setPopularityWeight(request.getPopularityWeight());
        }
    }

    default Set<String> normalizeHashtags(Set<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return Set.of();
        }
        return hashtags.stream()
                .map(HashtagUtil::normalize)
                .filter(h -> h != null && !h.isEmpty())
                .collect(Collectors.toSet());
    }
}


