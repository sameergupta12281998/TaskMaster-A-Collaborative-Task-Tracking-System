package com.taskmaster.mapper;

import com.taskmaster.dto.response.AttachmentResponse;
import com.taskmaster.entity.Attachment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachmentMapper {

    public static AttachmentResponse toResponse(Attachment attachment) {
        if (attachment == null) return null;
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .taskId(attachment.getTask().getId())
                .uploadedBy(AttachmentResponse.UploaderSummary.builder()
                        .id(attachment.getUploadedBy().getId())
                        .username(attachment.getUploadedBy().getUsername())
                        .build())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
