package com.vibeclip.entity;

import com.vibeclip.util.HashtagUtil;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String videoUrl; // URL к файлу видео (S3, CDN и т.д.)

    @Column
    private String thumbnailUrl; // URL к превью (опционально, если не указан - будет извлечен первый кадр)

    @Column(nullable = false)
    private Integer durationSeconds; // длительность в секундах

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ElementCollection
    @CollectionTable(name = "video_hashtags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "hashtag")
    @Builder.Default
    private Set<String> hashtags = new HashSet<>();

    /**
     * Переопределяем сеттер для автоматической нормализации всех хэштегов
     */
    public void setHashtags(Set<String> hashtags) {
        if (hashtags == null) {
            this.hashtags = new HashSet<>();
        } else {
            this.hashtags = hashtags.stream()
                    .map(HashtagUtil::normalize)
                    .filter(h -> h != null && !h.isEmpty())
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VideoStatus status = VideoStatus.PUBLISHED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Вспомогательный метод для добавления хэштега
     * Автоматически нормализует хэштег перед добавлением
     */
    public void addHashtag(String hashtag) {
        if (hashtag != null) {
            String normalized = HashtagUtil.normalize(hashtag);
            if (normalized != null && !normalized.isEmpty()) {
                this.hashtags.add(normalized);
            }
        }
    }

    /**
     * Вспомогательный метод для удаления хэштега
     */
    public void removeHashtag(String hashtag) {
        this.hashtags.remove(hashtag);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}


