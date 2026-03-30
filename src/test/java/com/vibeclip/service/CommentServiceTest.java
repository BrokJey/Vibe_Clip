package com.vibeclip.service;

import com.vibeclip.dto.comment.CommentRequest;
import com.vibeclip.dto.comment.CommentResponse;
import com.vibeclip.entity.Comment;
import com.vibeclip.entity.User;
import com.vibeclip.entity.Video;
import com.vibeclip.mapper.CommentMapper;
import com.vibeclip.repository.CommentRepository;
import com.vibeclip.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private VideoMetricService videoMetricService;
    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;


    @Test
    void create_success() {
        CommentRequest request = new CommentRequest();
        request.setVideoId(UUID.randomUUID());
        request.setText("Отличное видео!");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("TestUser");

        Video video = new Video();
        video.setId(request.getVideoId());

        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setText("Отличное видео!");

        CommentResponse expectedResponse = new CommentResponse();
        expectedResponse.setId(comment.getId());
        expectedResponse.setText(comment.getText());
        expectedResponse.setUsername(user.getUsername());

        when(videoRepository.findById(request.getVideoId())).thenReturn(Optional.of(video));
        when(commentMapper.fromDTO(request)).thenReturn(comment);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toDTO(comment)).thenReturn(expectedResponse);

        CommentResponse response = commentService.create(request, user);

        assertNotNull(response);
        assertEquals(expectedResponse.getText(), response.getText());
        assertEquals(expectedResponse.getUsername(), response.getUsername());

        verify(videoRepository).findById(request.getVideoId());
        verify(commentMapper).fromDTO(request);
        verify(commentRepository).save(any(Comment.class));
        verify(commentMapper).toDTO(comment);
        verify(videoMetricService).incrementCommentCount(video.getId());
    }

    @Test
    void create_videoNotFound_shouldThrow() {
        CommentRequest request = new CommentRequest();
        UUID videoId = UUID.randomUUID();
        request.setVideoId(videoId);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("TestUser");

        when(videoRepository.findById(request.getVideoId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.create(request, user)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(commentMapper, never()).fromDTO(any());
        verify(commentRepository, never()).save(any());
        verify(commentMapper, never()).toDTO(any());
        verify(videoMetricService, never()).incrementCommentCount(any());
    }

    @Test
    void getByVideoId_success() {
        UUID videoId = UUID.randomUUID();
        Video video = new Video();
        video.setId(videoId);

        Comment comment1 = new Comment();
        comment1.setText("Комментарий 1");

        Comment comment2 = new Comment();
        comment2.setText("Комментарий 2");

        CommentResponse response1 = new CommentResponse();
        response1.setText(comment1.getText());

        CommentResponse response2 = new CommentResponse();
        response2.setText(comment2.getText());

        List<Comment> comments = List.of(comment1, comment2);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(commentRepository.findByVideoOrderByCreatedAtDesc(video)).thenReturn(comments);
        when(commentMapper.toDTO(comment1)).thenReturn(response1);
        when(commentMapper.toDTO(comment2)).thenReturn(response2);

        List<CommentResponse> result = commentService.getByVideoId(videoId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Комментарий 1", result.get(0).getText());
        assertEquals("Комментарий 2", result.get(1).getText());

        verify(videoRepository.findById(videoId));
        verify(commentRepository.findByVideoOrderByCreatedAtDesc(video));
        verify(commentMapper.toDTO(comment1));
        verify(commentMapper.toDTO(comment2));
    }

    @Test
    void getByVideoId_videoNotFound_shouldThrow() {
        UUID videoId = UUID.randomUUID();

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.getByVideoId(videoId)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(videoId);
        verify(commentRepository, never()).findByVideoOrderByCreatedAtDesc(any());
        verify(commentMapper, never()).toDTO(any());
        verify(commentMapper, never()).toDTO(any());
    }

    @Test
    void getByVideoIdPageable_success() {
        UUID videoId = UUID.randomUUID();
        Video video = new Video();
        video.setId(videoId);

        Comment comment1 = new Comment();
        comment1.setText("Комментарий 1");

        Comment comment2 = new Comment();
        comment2.setText("Комментарий 2");

        CommentResponse response1 = new CommentResponse();
        response1.setText(comment1.getText());

        CommentResponse response2 = new CommentResponse();
        response2.setText(comment2.getText());

        Pageable pageable = PageRequest.of(0, 10);

        Page<Comment> commentPage = new PageImpl<>(List.of(comment1, comment2));

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(commentRepository.findByVideoOrderByCreatedAtDesc(video, pageable)).thenReturn(commentPage);
        when(commentMapper.toDTO(comment1)).thenReturn(response1);
        when(commentMapper.toDTO(comment2)).thenReturn(response2);

        Page<CommentResponse> result = commentService.getByVideoId(videoId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("Комментарий 1", result.getContent().get(0).getText());
        assertEquals("Комментарий 2", result.getContent().get(1).getText());

        verify(videoRepository.findById(videoId));
        verify(commentRepository.findByVideoOrderByCreatedAtDesc(video));
        verify(commentMapper.toDTO(comment1));
        verify(commentMapper.toDTO(comment2));
    }

    @Test
    void getByVideoIdPageable_videoNotFound_shouldThrow() {
        UUID videoId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(0, 10);

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.getByVideoId(videoId, pageable)
        );

        assertTrue(ex.getMessage().contains("Видео не найдено"));

        verify(videoRepository).findById(videoId);
        verify(commentRepository, never()).findByVideoOrderByCreatedAtDesc(any(), any());
        verify(commentMapper, never()).toDTO(any());
    }

    @Test
    void getById_success() {
        UUID commentId = UUID.randomUUID();

        Comment comment = new Comment();
        comment.setText("Комментарий");
        comment.setId(commentId);

        CommentResponse response = new CommentResponse();
        response.setText(comment.getText());

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentMapper.toDTO(comment)).thenReturn(response);

        CommentResponse result = commentService.getById(commentId);

        assertNotNull(result);
        assertEquals("Комментарий", result.getText());

        verify(commentRepository.findById(commentId));
        verify(commentMapper.toDTO(comment));
    }

    @Test
    void getById_commentNotFound_shouldThrow() {
        UUID commentId = UUID.randomUUID();

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.getById(commentId)
        );

        assertTrue(ex.getMessage().contains("Комментарий не найден"));

        verify(commentRepository).findById(commentId);
        verify(commentMapper, never()).toDTO(any());
    }
}