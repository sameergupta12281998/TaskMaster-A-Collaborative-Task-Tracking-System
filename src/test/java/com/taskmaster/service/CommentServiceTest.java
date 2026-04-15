package com.taskmaster.service;

import com.taskmaster.dto.request.CommentCreateRequest;
import com.taskmaster.dto.response.CommentResponse;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.entity.Comment;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.User;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.repository.CommentRepository;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UUID taskId;
    private UUID userId;
    private Task task;
    private User user;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        task = Task.builder().title("Test Task").build();
        task.setId(taskId);

        user = User.builder().username("testuser").email("test@example.com").password("encoded").build();
        user.setId(userId);
    }

    @Test
    @DisplayName("Should add comment successfully")
    void addComment_Success() {
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("Looks good")
                .build();

        Comment saved = Comment.builder()
                .content("Looks good")
                .task(task)
                .author(user)
                .build();
        saved.setId(UUID.randomUUID());
        saved.setCreatedAt(Instant.now());
        saved.setUpdatedAt(Instant.now());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse response = commentService.addComment(taskId, userId, request);

        assertThat(response.getTaskId()).isEqualTo(taskId);
        assertThat(response.getContent()).isEqualTo("Looks good");
    }

    @Test
    @DisplayName("Should throw when task not found while adding comment")
    void addComment_TaskNotFound() {
        CommentCreateRequest request = CommentCreateRequest.builder().content("Looks good").build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(taskId, userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("Should fetch paged comments successfully")
    void getCommentsByTask_Success() {
        Comment comment = Comment.builder().content("Comment 1").task(task).author(user).build();
        comment.setId(UUID.randomUUID());
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());

        Page<Comment> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1);

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(commentRepository.findByTaskIdWithAuthor(eq(taskId), any(Pageable.class))).thenReturn(page);

        PagedResponse<CommentResponse> response = commentService.getCommentsByTask(taskId, PageRequest.of(0, 20));

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getContent()).isEqualTo("Comment 1");
    }

    @Test
    @DisplayName("Should throw when task not found while fetching comments")
    void getCommentsByTask_TaskNotFound() {
        when(taskRepository.existsById(taskId)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByTask(taskId, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }
}
