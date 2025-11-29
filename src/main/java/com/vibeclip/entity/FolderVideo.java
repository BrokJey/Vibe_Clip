package com.vibeclip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Связь many-to-many между Folder и Video
 * Хранит позицию видео в ленте и score для ранжирования
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "folder_videos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"folder_id", "video_id"})
})
public class FolderVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    /**
     * Позиция в ленте (для сортировки)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    /**
     * Score для ранжирования (вычисляется алгоритмом рекомендаций)
     */
    @Column(nullable = false)
    @Builder.Default
    private Double score = 0.0;

    /**
     * Показывалось ли видео пользователю
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean shown = false;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}


