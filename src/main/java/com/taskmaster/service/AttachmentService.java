package com.taskmaster.service;

import com.taskmaster.dto.response.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AttachmentService {

    AttachmentResponse uploadAttachment(UUID taskId, UUID userId, MultipartFile file);

    List<AttachmentResponse> getAttachmentsByTask(UUID taskId);

    Resource downloadAttachment(UUID attachmentId);
}
