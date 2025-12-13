package com.vibeclip.controller;

import com.vibeclip.dto.reaction.ReactionRequest;
import com.vibeclip.dto.reaction.ReactionResponse;
import com.vibeclip.entity.ReactionType;
import com.vibeclip.entity.User;
import com.vibeclip.service.ReactionService;
import com.vibeclip.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reactions")
public class ReactionController extends BaseController {

    private final ReactionService reactionService;

    public ReactionController(UserService userService, ReactionService reactionService) {
        super(userService);
        this.reactionService = reactionService;
    }

    // Создание реакции на видео (лайк, просмотр, репорт и т.д.)
    // Для LIKE реакций работает toggle: если лайк уже есть, он удаляется (возвращается null)
    @PostMapping
    public ResponseEntity<ReactionResponse> create(
            @Valid @RequestBody ReactionRequest request,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        ReactionResponse response = reactionService.create(request, user);
        
        // Если реакция была удалена (toggle для LIKE), возвращаем 204 No Content
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Удаление реакции на видео
    @DeleteMapping("/video/{videoId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID videoId,
            @RequestParam ReactionType reactionType,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        reactionService.delete(videoId, reactionType, user);
        return ResponseEntity.noContent().build();
    }

    // Получение всех реакций текущего пользователя на конкретное видео
    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<ReactionResponse>> getByVideo(
            @PathVariable UUID videoId,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        List<ReactionResponse> response = reactionService.getByUserAndVideo(user, videoId);
        return ResponseEntity.ok(response);
    }

    // Проверка наличия конкретной реакции у пользователя на видео
    @GetMapping("/video/{videoId}/check")
    public ResponseEntity<Boolean> hasReaction(
            @PathVariable UUID videoId,
            @RequestParam ReactionType reactionType,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        boolean hasReaction = reactionService.hasReaction(user, videoId, reactionType);
        return ResponseEntity.ok(hasReaction);
    }

    // Получение всех реакций текущего пользователя определенного типа
    @GetMapping("/my")
    public ResponseEntity<List<ReactionResponse>> getMyReactions(
            @RequestParam ReactionType reactionType,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        List<ReactionResponse> response = reactionService.getByUser(user, reactionType);
        return ResponseEntity.ok(response);
    }
}

