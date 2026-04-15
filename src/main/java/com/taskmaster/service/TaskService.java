package com.taskmaster.service;

import com.taskmaster.dto.request.TaskCreateRequest;
import com.taskmaster.dto.request.TaskUpdateRequest;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.dto.response.TaskResponse;
import com.taskmaster.entity.enums.TaskPriority;
import com.taskmaster.entity.enums.TaskStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {

    TaskResponse createTask(UUID userId, TaskCreateRequest request);

    TaskResponse getTaskById(UUID taskId);

    TaskResponse updateTask(UUID userId, UUID taskId, TaskUpdateRequest request);

    void deleteTask(UUID userId, UUID taskId);

    PagedResponse<TaskResponse> getTasksByUser(UUID userId, Pageable pageable);

    PagedResponse<TaskResponse> getTasksByTeam(UUID teamId, Pageable pageable);

    PagedResponse<TaskResponse> filterTasks(TaskStatus status, TaskPriority priority,
                                             String search, String sortBy, String sortDir,
                                             Pageable pageable);
}
