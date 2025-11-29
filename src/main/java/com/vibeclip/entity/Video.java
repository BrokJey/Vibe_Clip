package com.vibeclip.entity;

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

    @Column(nullable = false)
    private String thumbnailUrl; // URL к превью

    @Column(nullable = false)
    private Integer durationSeconds; // длительность в секундах

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ElementCollection
    @CollectionTable(name = "video_hashtags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "hashtag")
    private Set<String> hashtags = new HashSet<>();

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
     */
    public void addHashtag(String hashtag) {
        this.hashtags.add(hashtag);
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


