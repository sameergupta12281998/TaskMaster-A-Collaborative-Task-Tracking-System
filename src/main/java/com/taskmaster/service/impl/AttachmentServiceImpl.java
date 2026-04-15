package com.taskmaster.service.impl;

import com.taskmaster.dto.response.AttachmentResponse;
import com.taskmaster.entity.Attachment;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.User;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.mapper.AttachmentMapper;
import com.taskmaster.repository.AttachmentRepository;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.AttachmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.allowed-extensions}")
    private String allowedExtensions;

    private Path uploadPath;
    private Set<String> allowedExtensionSet;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedExtensionSet = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException ex) {
            log.error("Failed to create upload directory: {}", uploadPath, ex);
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    @Override
    @Transactional
    public AttachmentResponse uploadAttachment(UUID taskId, UUID userId, MultipartFile file) {
        log.info("Uploading attachment for task: {} by user: {}", taskId, userId);

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unnamed");

        // Validate file extension
        String extension = getFileExtension(originalFileName);
        if (!allowedExtensionSet.contains(extension.toLowerCase())) {
            throw new BadRequestException("File type '" + extension + "' is not allowed. Allowed types: " + allowedExtensions);
        }

        // Prevent path traversal
        if (originalFileName.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }

        // Generate unique file name
        String storedFileName = UUID.randomUUID() + "." + extension;

        try {
            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File stored at: {}", targetPath);
        } catch (IOException ex) {
            log.error("Failed to store file: {}", originalFileName, ex);
            throw new RuntimeException("Failed to store file", ex);
        }

        Attachment attachment = Attachment.builder()
                .fileName(storedFileName)
                .originalFileName(originalFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(storedFileName)
                .task(task)
                .uploadedBy(uploader)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("Attachment uploaded successfully with id: {}", attachment.getId());
        return AttachmentMapper.toResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByTask(UUID taskId) {
        log.debug("Fetching attachments for task: {}", taskId);

        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        return attachmentRepository.findByTaskIdWithUploader(taskId).stream()
                .map(AttachmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(UUID attachmentId) {
        log.debug("Downloading attachment: {}", attachmentId);

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        try {
            Path filePath = uploadPath.resolve(attachment.getFilePath()).normalize();

            // Ensure the resolved path is still within the upload directory
            if (!filePath.startsWith(uploadPath)) {
                throw new BadRequestException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Attachment", "id", attachmentId);
            }

            return resource;
        } catch (MalformedURLException ex) {
            log.error("Failed to read file for attachment: {}", attachmentId, ex);
            throw new ResourceNotFoundException("Attachment", "id", attachmentId);
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
