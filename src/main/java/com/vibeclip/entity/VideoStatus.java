package com.vibeclip.entity;

/**
 * Статус видео для модерации и публикации
 */
public enum VideoStatus {
    DRAFT,          // Черновик
    PENDING,        // На модерации
    PUBLISHED,      // Опубликовано
    REJECTED,       // Отклонено модератором
    DELETED         // Удалено
}


