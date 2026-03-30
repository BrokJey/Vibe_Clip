package com.vibeclip.service;

import com.vibeclip.dto.comment.CommentRequest;
import com.vibeclip.dto.comment.CommentResponse;
import com.vibeclip.entity.Comment;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.mapper.CommentMapper;
import com.vibeclip.repository.CommentRepository;
import com.vibeclip.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final VideoMetricService videoMetricService;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse create(CommentRequest request, User user) {
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> {
                    log.error("Видео не найдено: {}", request.getVideoId());
                    return new IllegalArgumentException("Видео не найдено: " + request.getVideoId());
                });

        // Создаем комментарий
        Comment comment = commentMapper.fromDTO(request);
        comment.setUser(user);
        comment.setVideo(video);

        Comment saved = commentRepository.save(comment);

        // Увеличиваем счетчик комментариев в метриках видео
        videoMetricService.incrementCommentCount(video.getId());

        log.info("Создан комментарий {} пользователем {} к видео {}", saved.getId(), user.getUsername(), video.getId());

        return commentMapper.toDTO(saved);
    }

    public List<CommentResponse> getByVideoId(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.error("Видео не найдено: {}", videoId);
                    return new IllegalArgumentException("Видео не найдено: " + videoId);
                });

        List<Comment> comments = commentRepository.findByVideoOrderByCreatedAtDesc(video);
        return comments.stream()
                .map(commentMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<CommentResponse> getByVideoId(UUID videoId, Pageable pageable) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.error("Видео не найдено: {}", videoId);
                    return new IllegalArgumentException("Видео не найдено: " + videoId);
                });

        Page<Comment> comments = commentRepository.findByVideoOrderByCreatedAtDesc(video, pageable);
        return comments.map(commentMapper::toDTO);
    }

    public CommentResponse getById(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Комментарий не найден: {}", commentId);
                    return new IllegalArgumentException("Комментарий не найден: " + commentId);
                });

        return commentMapper.toDTO(comment);
    }

    @Transactional
    public void delete(UUID commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Комментарий не найден: {}", commentId);
                    return new IllegalArgumentException("Комментарий не найден: " + commentId);
                });

        // Проверяем, что пользователь является автором комментария
        if (!comment.getUser().getId().equals(user.getId())) {
            log.error("Пользователь {} пытается удалить чужой комментарий {}", user.getUsername(), commentId);
            throw new IllegalStateException("Вы можете удалить только свой комментарий");
        }

        UUID videoId = comment.getVideo().getId();
        commentRepository.delete(comment);

        // Уменьшаем счетчик комментариев в метриках видео
        videoMetricService.decrementCommentCount(videoId);

        log.info("Комментарий {} удален пользователем {}", commentId, user.getUsername());
    }

    /**
     * Получает все комментарии пользователя
     */
    public List<CommentResponse> getByUser(User user) {
        List<Comment> comments = commentRepository.findByUserOrderByCreatedAtDesc(user);
        return comments.stream()
                .map(commentMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество комментариев к видео
     */
    public long countByVideoId(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.error("Видео не найдено: {}", videoId);
                    return new IllegalArgumentException("Видео не найдено: " + videoId);
                });

        return commentRepository.countByVideo(video);
    }
}

