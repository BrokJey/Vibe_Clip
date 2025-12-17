package com.vibeclip.util;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Утилитный класс для нормализации хэштегов.
 * Нормализует хэштеги: приводит к нижнему регистру, удаляет символ "#" в начале, обрезает пробелы.
 */
public class HashtagUtil {

    /**
     * Нормализует один хэштег:
     * - Удаляет символ "#" в начале (если есть)
     * - Удаляет кавычки (одинарные и двойные) в начале и конце
     * - Обрезает пробелы
     * - Приводит к нижнему регистру
     *
     * @param hashtag исходный хэштег
     * @return нормализованный хэштег или null, если после нормализации строка пустая
     */
    public static String normalize(String hashtag) {
        if (hashtag == null) {
            return null;
        }

        String normalized = hashtag.trim();

        // Удаляем символ "#" в начале, если есть
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1).trim();
        }

        // Агрессивно удаляем все кавычки (одинарные и двойные) в начале
        while (!normalized.isEmpty() && (normalized.startsWith("\"") || normalized.startsWith("'"))) {
            normalized = normalized.substring(1).trim();
        }
        
        // Агрессивно удаляем все кавычки (одинарные и двойные) в конце
        while (!normalized.isEmpty() && (normalized.endsWith("\"") || normalized.endsWith("'"))) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        // Приводим к нижнему регистру
        normalized = normalized.toLowerCase();

        // Возвращаем null, если после нормализации строка пустая
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Нормализует множество хэштегов, удаляя пустые и дубликаты.
     *
     * @param hashtags исходное множество хэштегов
     * @return нормализованное множество хэштегов
     */
    public static Set<String> normalizeSet(Set<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return Set.of();
        }

        return hashtags.stream()
                .map(HashtagUtil::normalize)
                .filter(h -> h != null && !h.isEmpty())
                .collect(Collectors.toSet());
    }
}

