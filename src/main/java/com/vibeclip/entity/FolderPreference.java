package com.vibeclip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Настройки рекомендаций для папки (Embeddable)
 * Хранит фильтры и правила для алгоритма рекомендаций
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPreference {

    /**
     * Разрешенные хэштеги (если пусто - все разрешены)
     */
    @ElementCollection
    @CollectionTable(name = "folder_allowed_hashtags", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "hashtag")
    @Builder.Default
    private Set<String> allowedHashtags = new HashSet<>();

    /**
     * Запрещенные хэштеги
     */
    @ElementCollection
    @CollectionTable(name = "folder_blocked_hashtags", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "hashtag")
    @Builder.Default
    private Set<String> blockedHashtags = new HashSet<>();

    /**
     * ID авторов, чьи видео разрешены (если пусто - все разрешены)
     */
    @ElementCollection
    @CollectionTable(name = "folder_allowed_authors", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "author_id")
    @Builder.Default
    private Set<String> allowedAuthorIds = new HashSet<>();

    /**
     * ID авторов, чьи видео запрещены
     */
    @ElementCollection
    @CollectionTable(name = "folder_blocked_authors", joinColumns = @JoinColumn(name = "folder_id"))
    @Column(name = "author_id")
    @Builder.Default
    private Set<String> blockedAuthorIds = new HashSet<>();

    /**
     * Минимальная длительность видео в секундах (null = без ограничений)
     */
    @Column(name = "min_duration_seconds")
    private Integer minDurationSeconds;

    /**
     * Максимальная длительность видео в секундах (null = без ограничений)
     */
    @Column(name = "max_duration_seconds")
    private Integer maxDurationSeconds;

    /**
     * Приоритет свежести контента (0.0 - 1.0, где 1.0 = только новые)
     */
    @Column(name = "freshness_weight")
    @Builder.Default
    private Double freshnessWeight = 0.5;

    /**
     * Приоритет популярности (0.0 - 1.0, где 1.0 = только популярные)
     */
    @Column(name = "popularity_weight")
    @Builder.Default
    private Double popularityWeight = 0.5;
}


