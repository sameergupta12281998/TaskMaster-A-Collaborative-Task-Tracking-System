package com.taskmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.dto.request.TaskCreateRequest;
import com.taskmaster.dto.request.TaskUpdateRequest;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.dto.response.TaskResponse;
import com.taskmaster.entity.enums.TaskPriority;
import com.taskmaster.entity.enums.TaskStatus;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.TaskService;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private CustomUserPrincipal createPrincipal() {
        return new CustomUserPrincipal(
                UUID.randomUUID(), "testuser", "test@example.com",
                "password", true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("POST /api/tasks - Create task successfully")
    void createTask_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("New Task")
                .description("Task Description")
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        TaskResponse response = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("New Task")
                .description("Task Description")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(taskService.createTask(eq(principal.getId()), any(TaskCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("New Task"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Get task successfully")
    void getTask_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .title("Test Task")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.MEDIUM)
                .createdAt(Instant.now())
                .build();

        when(taskService.getTaskById(taskId)).thenReturn(response);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(taskId.toString()));
    }

    @Test
    @DisplayName("GET /api/tasks - Filter tasks")
    void getTasks_Filtered() throws Exception {
        CustomUserPrincipal principal = createPrincipal();

        TaskResponse task = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("Filtered Task")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.HIGH)
                .build();

        PagedResponse<TaskResponse> response = PagedResponse.<TaskResponse>builder()
                .content(List.of(task))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(taskService.filterTasks(eq(TaskStatus.OPEN), eq(TaskPriority.HIGH), eq("Filtered"),
                eq("createdAt"), eq("desc"), any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/tasks")
                        .with(user(principal))
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("search", "Filtered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Filtered Task"));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - Update task successfully")
    void updateTask_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated Task")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .title("Updated Task")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .build();

        when(taskService.updateTask(eq(principal.getId()), eq(taskId), any(TaskUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - Delete task successfully")
    void deleteTask_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID taskId = UUID.randomUUID();

        doNothing().when(taskService).deleteTask(principal.getId(), taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task deleted successfully"));
    }

    @Test
    @DisplayName("GET /api/tasks/my-tasks - Success")
    void getMyTasks_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();

        TaskResponse task = TaskResponse.builder()
                .id(UUID.randomUUID())
                .title("My Task")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.MEDIUM)
                .build();

        PagedResponse<TaskResponse> response = PagedResponse.<TaskResponse>builder()
                .content(List.of(task))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(taskService.getTasksByUser(eq(principal.getId()), any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/api/tasks/my-tasks")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("My Task"));
    }

    @Test
    @DisplayName("POST /api/tasks - Unauthorized access")
    void createTask_Unauthorized() throws Exception {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("New Task")
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
