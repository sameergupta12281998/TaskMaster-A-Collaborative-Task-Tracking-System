package com.taskmaster.service;

import com.taskmaster.dto.request.CommentCreateRequest;
import com.taskmaster.dto.response.CommentResponse;
import com.taskmaster.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {

    CommentResponse addComment(UUID taskId, UUID userId, CommentCreateRequest request);

    PagedResponse<CommentResponse> getCommentsByTask(UUID taskId, Pageable pageable);
}
