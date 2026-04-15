package com.taskmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.dto.request.CommentCreateRequest;
import com.taskmaster.dto.response.CommentResponse;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    private CustomUserPrincipal createPrincipal() {
        return new CustomUserPrincipal(
                UUID.randomUUID(), "testuser", "test@example.com",
                "password", true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/comments - Success")
    void addComment_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("This is a test comment")
                .build();

        CommentResponse response = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("This is a test comment")
                .taskId(taskId)
                .author(CommentResponse.AuthorSummary.builder()
                        .id(principal.getId())
                        .username("testuser")
                        .fullName("Test User")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(commentService.addComment(eq(taskId), eq(principal.getId()), any(CommentCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("This is a test comment"));
    }

    @Test
    @DisplayName("POST /api/tasks/{taskId}/comments - Validation error")
    void addComment_ValidationError() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        CommentCreateRequest request = CommentCreateRequest.builder()
                .content(" ")
                .build();

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.content").exists());
    }

    @Test
    @DisplayName("GET /api/tasks/{taskId}/comments - Success")
    void getComments_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        CommentResponse comment = CommentResponse.builder()
                .id(UUID.randomUUID())
                .content("Looks good")
                .taskId(taskId)
                .createdAt(Instant.now())
                .build();

        PagedResponse<CommentResponse> response = PagedResponse.<CommentResponse>builder()
                .content(List.of(comment))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(commentService.getCommentsByTask(eq(taskId), any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].content").value("Looks good"));
    }
}
