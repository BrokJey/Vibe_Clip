package com.vibeclip.entity;

import com.vibeclip.util.HashtagUtil;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPreference {


    @ElementCollection
    @CollectionTable(name = "folder_allowed_hashtags", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "hashtag")
    @Builder.Default
    private Set<String> allowedHashtags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "folder_blocked_hashtags", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "hashtag")
    @Builder.Default
    private Set<String> blockedHashtags = new HashSet<>();

    public void setAllowedHashtags(Set<String> allowedHashtags) {
        if (allowedHashtags == null) {
            this.allowedHashtags = new HashSet<>();
        } else {
            this.allowedHashtags = allowedHashtags.stream()
                    .map(HashtagUtil::normalize)
                    .filter(h -> h != null && !h.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    public void setBlockedHashtags(Set<String> blockedHashtags) {
        if (blockedHashtags == null) {
            this.blockedHashtags = new HashSet<>();
        } else {
            this.blockedHashtags = blockedHashtags.stream()
                    .map(HashtagUtil::normalize)
                    .filter(h -> h != null && !h.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    @ElementCollection
    @CollectionTable(name = "folder_allowed_authors", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "author_id")
    @Builder.Default
    private Set<String> allowedAuthorIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "folder_blocked_authors", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "author_id")
    @Builder.Default
    private Set<String> blockedAuthorIds = new HashSet<>();

    @Column(name = "min_duration_seconds")
    private Integer minDurationSeconds;

    @Column(name = "max_duration_seconds")
    private Integer maxDurationSeconds;

    @Column(name = "freshness_weight")
    @Builder.Default
    private Double freshnessWeight = 0.5;

    @Column(name = "popularity_weight")
    @Builder.Default
    private Double popularityWeight = 0.5;
}


