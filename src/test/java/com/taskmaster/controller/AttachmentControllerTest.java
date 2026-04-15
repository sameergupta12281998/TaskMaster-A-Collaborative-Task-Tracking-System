package com.taskmaster.controller;

import com.taskmaster.dto.response.AttachmentResponse;
import com.taskmaster.entity.Attachment;
import com.taskmaster.repository.AttachmentRepository;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.AttachmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    @MockBean
    private AttachmentRepository attachmentRepository;

    private CustomUserPrincipal createPrincipal() {
        return new CustomUserPrincipal(
                UUID.randomUUID(), "testuser", "test@example.com",
                "password", true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/attachments - Success")
    void uploadAttachment_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "notes.pdf", "application/pdf", "content".getBytes());

        AttachmentResponse response = AttachmentResponse.builder()
                .id(UUID.randomUUID())
                .fileName("stored.pdf")
                .originalFileName("notes.pdf")
                .contentType("application/pdf")
                .fileSize(7L)
                .taskId(taskId)
                .createdAt(Instant.now())
                .build();

        when(attachmentService.uploadAttachment(eq(taskId), eq(principal.getId()), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                        .file(file)
                        .with(user(principal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFileName").value("notes.pdf"));
    }

    @Test
    @DisplayName("GET /api/tasks/{taskId}/attachments - Success")
    void getAttachments_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        AttachmentResponse response = AttachmentResponse.builder()
                .id(UUID.randomUUID())
                .originalFileName("notes.pdf")
                .contentType("application/pdf")
                .fileSize(7L)
                .taskId(taskId)
                .createdAt(Instant.now())
                .build();

        when(attachmentService.getAttachmentsByTask(taskId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].originalFileName").value("notes.pdf"));
    }

    @Test
    @DisplayName("GET /api/attachments/{id}/download - Success")
    void downloadAttachment_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID attachmentId = UUID.randomUUID();

        Attachment attachment = Attachment.builder()
                .originalFileName("notes.pdf")
                .contentType("application/pdf")
                .fileName("stored.pdf")
                .filePath("stored.pdf")
                .fileSize(7L)
                .build();
        attachment.setId(attachmentId);

        when(attachmentService.downloadAttachment(attachmentId))
                .thenReturn(new ByteArrayResource("content".getBytes()));
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        mockMvc.perform(get("/api/attachments/{id}/download", attachmentId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"notes.pdf\""))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/attachments - Unauthorized")
    void uploadAttachment_Unauthorized() throws Exception {
        UUID taskId = UUID.randomUUID();

        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "notes.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}
