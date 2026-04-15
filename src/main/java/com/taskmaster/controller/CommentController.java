package com.taskmaster.controller;

import com.taskmaster.dto.request.CommentCreateRequest;
import com.taskmaster.dto.response.ApiResponse;
import com.taskmaster.dto.response.CommentResponse;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.CommentService;
import com.taskmaster.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comments", description = "Task comment APIs")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Add a comment to a task")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID taskId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.addComment(taskId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", response));
    }

    @GetMapping("/tasks/{taskId}/comments")
    @Operation(summary = "Get all comments for a task")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getComments(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PagedResponse<CommentResponse> response = commentService.getCommentsByTask(
                taskId, PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE),
                        Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
