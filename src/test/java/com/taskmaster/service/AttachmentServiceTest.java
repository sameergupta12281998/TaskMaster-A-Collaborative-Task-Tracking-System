package com.taskmaster.service;

import com.taskmaster.dto.response.AttachmentResponse;
import com.taskmaster.entity.Attachment;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.User;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.repository.AttachmentRepository;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.impl.AttachmentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private UUID taskId;
    private UUID userId;
    private Path tempUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tempUploadDir = Files.createTempDirectory("attachment-test-");

        ReflectionTestUtils.setField(attachmentService, "uploadDir", tempUploadDir.toString());
        ReflectionTestUtils.setField(attachmentService, "allowedExtensions", "pdf,jpg,png");
        ReflectionTestUtils.setField(attachmentService, "uploadPath", tempUploadDir);
        ReflectionTestUtils.setField(attachmentService, "allowedExtensionSet", Set.of("pdf", "jpg", "png"));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempUploadDir != null && Files.exists(tempUploadDir)) {
            try (var stream = Files.walk(tempUploadDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // cleanup best-effort
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("Should throw when file is empty")
    void uploadAttachment_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(taskId, userId, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    @DisplayName("Should throw when extension not allowed")
    void uploadAttachment_InvalidExtension() {
        Task task = Task.builder().title("Task").build();
        task.setId(taskId);
        User user = User.builder().username("u").email("e@test.com").password("p").build();
        user.setId(userId);

        MockMultipartFile file = new MockMultipartFile("file", "script.exe", "application/octet-stream", "bin".getBytes());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> attachmentService.uploadAttachment(taskId, userId, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    @DisplayName("Should upload attachment successfully")
    void uploadAttachment_Success() {
        Task task = Task.builder().title("Task").build();
        task.setId(taskId);
        User user = User.builder().username("u").email("e@test.com").password("p").build();
        user.setId(userId);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(Instant.now());
            return a;
        });

        AttachmentResponse response = attachmentService.uploadAttachment(taskId, userId, file);

        assertThat(response.getOriginalFileName()).isEqualTo("doc.pdf");
        assertThat(response.getTaskId()).isEqualTo(taskId);
    }

    @Test
    @DisplayName("Should throw when task not found while listing attachments")
    void getAttachmentsByTask_TaskNotFound() {
        when(taskRepository.existsById(taskId)).thenReturn(false);

        assertThatThrownBy(() -> attachmentService.getAttachmentsByTask(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("Should list attachments successfully")
    void getAttachmentsByTask_Success() {
        User uploader = User.builder().username("uploader").email("u@test.com").password("p").build();
        uploader.setId(userId);

        Task task = Task.builder().title("Task").build();
        task.setId(taskId);

        Attachment attachment = Attachment.builder()
                .fileName("stored.pdf")
                .originalFileName("doc.pdf")
                .contentType("application/pdf")
                .fileSize(10L)
                .filePath("stored.pdf")
                .task(task)
                .uploadedBy(uploader)
                .build();
        attachment.setId(UUID.randomUUID());
        attachment.setCreatedAt(Instant.now());

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(attachmentRepository.findByTaskIdWithUploader(taskId)).thenReturn(List.of(attachment));

        List<AttachmentResponse> response = attachmentService.getAttachmentsByTask(taskId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getOriginalFileName()).isEqualTo("doc.pdf");
    }

    @Test
    @DisplayName("Should throw when downloading nonexistent attachment")
    void downloadAttachment_NotFound() {
        UUID attachmentId = UUID.randomUUID();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.downloadAttachment(attachmentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Attachment");
    }

    @Test
    @DisplayName("Should throw for path traversal in stored file path")
    void downloadAttachment_PathTraversal() {
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = Attachment.builder()
                .filePath("../outside.pdf")
                .originalFileName("outside.pdf")
                .contentType("application/pdf")
                .fileName("outside.pdf")
                .fileSize(1L)
                .build();
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> attachmentService.downloadAttachment(attachmentId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid file path");
    }
}
