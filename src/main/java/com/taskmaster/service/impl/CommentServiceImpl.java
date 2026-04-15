package com.taskmaster.service.impl;

import com.taskmaster.dto.request.CommentCreateRequest;
import com.taskmaster.dto.response.CommentResponse;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.entity.Comment;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.User;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.mapper.CommentMapper;
import com.taskmaster.repository.CommentRepository;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentResponse addComment(UUID taskId, UUID userId, CommentCreateRequest request) {
        log.info("Adding comment to task: {} by user: {}", taskId, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .task(task)
                .author(author)
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment added successfully with id: {}", comment.getId());
        return CommentMapper.toResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> getCommentsByTask(UUID taskId, Pageable pageable) {
        log.debug("Fetching comments for task: {}", taskId);

        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        Page<Comment> commentPage = commentRepository.findByTaskIdWithAuthor(taskId, pageable);

        List<CommentResponse> content = commentPage.getContent().stream()
                .map(CommentMapper::toResponse)
                .toList();

        return PagedResponse.<CommentResponse>builder()
                .content(content)
                .page(commentPage.getNumber())
                .size(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .last(commentPage.isLast())
                .first(commentPage.isFirst())
                .build();
    }
}
