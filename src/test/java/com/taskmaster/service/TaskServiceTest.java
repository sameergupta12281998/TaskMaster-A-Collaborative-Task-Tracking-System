package com.taskmaster.service;

import com.taskmaster.dto.request.TaskCreateRequest;
import com.taskmaster.dto.request.TaskUpdateRequest;
import com.taskmaster.dto.response.TaskResponse;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.User;
import com.taskmaster.entity.enums.TaskPriority;
import com.taskmaster.entity.enums.TaskStatus;
import com.taskmaster.exception.AccessDeniedException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.TeamMembershipRepository;
import com.taskmaster.repository.TeamRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMembershipRepository teamMembershipRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private UUID userId;
    private UUID taskId;
    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .fullName("Test User")
                .build();
        user.setId(userId);
        user.setCreatedAt(Instant.now());

        task = Task.builder()
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(7))
                .createdBy(user)
                .comments(new ArrayList<>())
                .attachments(new ArrayList<>())
                .build();
        task.setId(taskId);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create task successfully")
    void createTask_Success() {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("New Task")
                .description("New Description")
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        TaskResponse response = taskService.createTask(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("New Task");
        assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when task not found")
    void getTaskById_NotFound() {
        when(taskRepository.findByIdWithDetails(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("Should get task by ID successfully")
    void getTaskById_Success() {
        when(taskRepository.findByIdWithDetails(taskId)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(taskId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(taskId);
        assertThat(response.getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("Should update task successfully as creator")
    void updateTask_Success_AsCreator() {
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated Title")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(taskRepository.findByIdWithDetails(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.updateTask(userId, taskId, request);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should deny update to non-creator/non-assignee")
    void updateTask_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(taskRepository.findByIdWithDetails(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTask(otherUserId, taskId, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should delete task successfully as creator")
    void deleteTask_Success() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskService.deleteTask(userId, taskId);

        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("Should deny delete to non-creator")
    void deleteTask_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.deleteTask(otherUserId, taskId))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Should fetch tasks by user with pagination")
    void getTasksByUser_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);

        when(taskRepository.findByUserInvolvement(userId, pageable)).thenReturn(page);

        var response = taskService.getTasksByUser(userId, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should fetch tasks by team with pagination")
    void getTasksByTeam_Success() {
        UUID teamId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);

        when(taskRepository.findByTeamId(teamId, pageable)).thenReturn(page);

        var response = taskService.getTasksByTeam(teamId, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should filter tasks successfully")
    void filterTasks_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);

        when(taskRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);

        var response = taskService.filterTasks(TaskStatus.OPEN, TaskPriority.MEDIUM,
                "Test", "createdAt", "desc", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Test Task");
    }
}
