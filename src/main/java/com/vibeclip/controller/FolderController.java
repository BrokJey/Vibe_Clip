package com.vibeclip.controller;

import com.vibeclip.dto.folder.FolderRequest;
import com.vibeclip.dto.folder.FolderResponse;
import com.vibeclip.dto.folder.FolderFeedResponse;
import com.vibeclip.dto.folder.FolderVideoResponse;
import com.vibeclip.entity.Folder;
import com.vibeclip.entity.FolderVideo;
import com.vibeclip.entity.User;
import com.vibeclip.mapper.FolderVideoMapper;
import com.vibeclip.service.FolderService;
import com.vibeclip.service.RecommendationService;
import com.vibeclip.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/folders")
public class FolderController extends BaseController {

    private final FolderService folderService;
    private final RecommendationService recommendationService;
    private final FolderVideoMapper folderVideoMapper;

    public FolderController(
            UserService userService,
            FolderService folderService,
            RecommendationService recommendationService,
            FolderVideoMapper folderVideoMapper
    ) {
        super(userService);
        this.folderService = folderService;
        this.recommendationService = recommendationService;
        this.folderVideoMapper = folderVideoMapper;
    }

    // Создание новой папки с настройками рекомендаций
    @PostMapping
    public ResponseEntity<FolderResponse> create(
            @Valid @RequestBody FolderRequest request,
            Authentication authentication
    ) {
        User owner = getCurrentUser(authentication);
        FolderResponse response = folderService.create(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Получение списка всех папок текущего пользователя
    @GetMapping
    public ResponseEntity<List<FolderResponse>> getMyFolders(Authentication authentication) {
        User owner = getCurrentUser(authentication);
        List<FolderResponse> response = folderService.getByOwner(owner);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение папки по идентификатору
     * Доступно только владельцу папки
     * 
     * @param id уникальный идентификатор папки
     * @param authentication данные аутентификации текущего пользователя
     * @return информация о папке
     */
    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User owner = getCurrentUser(authentication);
        FolderResponse response = folderService.getById(id, owner);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление информации о папке и её настройках рекомендаций
     * Доступно только владельцу папки
     * 
     * @param id уникальный идентификатор папки
     * @param request обновленные данные папки
     * @param authentication данные аутентификации текущего пользователя
     * @return обновленная папка
     */
    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody FolderRequest request,
            Authentication authentication
    ) {
        User owner = getCurrentUser(authentication);
        FolderResponse response = folderService.update(id, request, owner);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление папки (мягкое удаление - изменение статуса на DELETED)
     * Доступно только владельцу папки
     * 
     * @param id уникальный идентификатор папки
     * @param authentication данные аутентификации текущего пользователя
     * @return пустой ответ со статусом 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        User owner = getCurrentUser(authentication);
        folderService.delete(id, owner);
        return ResponseEntity.noContent().build();
    }

    /**
     * Архивирование папки (изменение статуса на ARCHIVED)
     * Доступно только владельцу папки
     * 
     * @param id уникальный идентификатор папки
     * @param authentication данные аутентификации текущего пользователя
     * @return пустой ответ со статусом 204
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID id, Authentication authentication) {
        User owner = getCurrentUser(authentication);
        folderService.archive(id, owner);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение ленты рекомендаций для папки
     * Возвращает непоказанные видео, отсортированные по score
     * Если видео недостаточно, автоматически генерируются новые рекомендации
     * 
     * @param id уникальный идентификатор папки
     * @param limit максимальное количество видео в ленте (по умолчанию 20)
     * @param authentication данные аутентификации текущего пользователя
     * @return лента папки с видео и метаданными
     */
    @GetMapping("/{id}/feed")
    public ResponseEntity<FolderFeedResponse> getFeed(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        User owner = getCurrentUser(authentication);
        Folder folder = folderService.getEntityById(id, owner);

        // Получаем ленту через RecommendationService
        List<FolderVideo> folderVideos = recommendationService.getFeedForFolder(folder, limit);

        // Преобразуем в DTO
        List<FolderVideoResponse> videoResponses = folderVideos.stream()
                .map(folderVideoMapper::toDTO)
                .collect(Collectors.toList());

        FolderFeedResponse response = FolderFeedResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getName())
                .videos(videoResponses)
                .totalCount(videoResponses.size())
                .page(0)
                .pageSize(limit)
                .hasMore(folderVideos.size() >= limit)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Принудительная перегенерация ленты рекомендаций для папки
     * Генерирует новую ленту на основе текущих настроек папки
     * 
     * @param id уникальный идентификатор папки
     * @param limit максимальное количество видео в ленте (по умолчанию 20)
     * @param authentication данные аутентификации текущего пользователя
     * @return новая лента папки с видео и метаданными
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<FolderFeedResponse> regenerateFeed(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        User owner = getCurrentUser(authentication);
        Folder folder = folderService.getEntityById(id, owner);

        // Генерируем новую ленту
        recommendationService.generateFeedForFolder(folder, limit);

        // Получаем сгенерированную ленту
        List<FolderVideo> folderVideos = recommendationService.getFeedForFolder(folder, limit);

        List<FolderVideoResponse> videoResponses = folderVideos.stream()
                .map(folderVideoMapper::toDTO)
                .collect(Collectors.toList());

        FolderFeedResponse response = FolderFeedResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getName())
                .videos(videoResponses)
                .totalCount(videoResponses.size())
                .page(0)
                .pageSize(limit)
                .hasMore(folderVideos.size() >= limit)
                .build();

        return ResponseEntity.ok(response);
    }
}

