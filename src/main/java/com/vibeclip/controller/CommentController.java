package com.vibeclip.controller;

import com.vibeclip.dto.comment.CommentRequest;
import com.vibeclip.dto.comment.CommentResponse;
import com.vibeclip.entity.User;
import com.vibeclip.service.CommentService;
import com.vibeclip.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class CommentController extends BaseController {

    private final CommentService commentService;

    public CommentController(UserService userService, CommentService commentService) {
        super(userService);
        this.commentService = commentService;
    }

    /**
     * Создание комментария к видео
     * POST /api/v1/videos/{videoId}/comments
     */
    @PostMapping("/{videoId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID videoId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        // Устанавливаем videoId из пути, если не указан в запросе
        request.setVideoId(videoId);
        CommentResponse response = commentService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение всех комментариев к видео (публичный доступ)
     * GET /api/v1/videos/{videoId}/comments
     */
    @GetMapping("/{videoId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable UUID videoId,
            @RequestParam(required = false) Boolean paginated,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (Boolean.TRUE.equals(paginated)) {
            // Возвращаем пагинированный результат
            Page<CommentResponse> page = commentService.getByVideoId(videoId, pageable);
            return ResponseEntity.ok(page.getContent());
        } else {
            // Возвращаем все комментарии
            List<CommentResponse> comments = commentService.getByVideoId(videoId);
            return ResponseEntity.ok(comments);
        }
    }

    /**
     * Получение комментария по ID
     * GET /api/v1/comments/{commentId}
     */
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable UUID commentId) {
        CommentResponse response = commentService.getById(commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление комментария (только свой)
     * DELETE /api/v1/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);
        commentService.delete(commentId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение всех комментариев текущего пользователя
     * GET /api/v1/comments/my
     */
    @GetMapping("/comments/my")
    public ResponseEntity<List<CommentResponse>> getMyComments(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<CommentResponse> comments = commentService.getByUser(user);
        return ResponseEntity.ok(comments);
    }
}

