package com.taskmaster.controller;

import com.taskmaster.dto.response.ApiResponse;
import com.taskmaster.dto.response.AttachmentResponse;
import com.taskmaster.entity.Attachment;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.repository.AttachmentRepository;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attachments", description = "File attachment APIs")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file attachment to a task")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID taskId,
            @RequestParam("file") MultipartFile file) {
        AttachmentResponse response = attachmentService.uploadAttachment(taskId, principal.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", response));
    }

    @GetMapping("/tasks/{taskId}/attachments")
    @Operation(summary = "Get all attachments for a task")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(@PathVariable UUID taskId) {
        List<AttachmentResponse> response = attachmentService.getAttachmentsByTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/attachments/{id}/download")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id) {
        Resource resource = attachmentService.downloadAttachment(id);

        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id));

        String contentType = attachment.getContentType() != null
                ? attachment.getContentType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .body(resource);
    }
}
