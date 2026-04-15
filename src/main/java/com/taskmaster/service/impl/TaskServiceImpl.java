package com.taskmaster.service.impl;

import com.taskmaster.dto.request.TaskCreateRequest;
import com.taskmaster.dto.request.TaskUpdateRequest;
import com.taskmaster.dto.response.PagedResponse;
import com.taskmaster.dto.response.TaskResponse;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.Team;
import com.taskmaster.entity.User;
import com.taskmaster.entity.enums.TaskPriority;
import com.taskmaster.entity.enums.TaskStatus;
import com.taskmaster.exception.AccessDeniedException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.mapper.TaskMapper;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.TeamMembershipRepository;
import com.taskmaster.repository.TeamRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.TaskService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    @Override
    @Transactional
    public TaskResponse createTask(UUID userId, TaskCreateRequest request) {
        log.info("Creating task '{}' by user: {}", request.getTitle(), userId);

        User creator = findUserById(userId);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.OPEN)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .createdBy(creator)
                .build();

        if (request.getAssignedUserId() != null) {
            User assignee = findUserById(request.getAssignedUserId());
            task.setAssignedUser(assignee);
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));

            // Verify the creator is a member of the team
            if (!teamMembershipRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
                throw new AccessDeniedException("You are not a member of this team");
            }
            task.setTeam(team);
        }

        task = taskRepository.save(task);
        log.info("Task created successfully with id: {}", task.getId());
        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        log.debug("Fetching task with id: {}", taskId);
        Task task = taskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID userId, UUID taskId, TaskUpdateRequest request) {
        log.info("Updating task: {} by user: {}", taskId, userId);

        Task task = taskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Only the creator or assigned user can update the task
        boolean isCreator = task.getCreatedBy().getId().equals(userId);
        boolean isAssignee = task.getAssignedUser() != null && task.getAssignedUser().getId().equals(userId);
        if (!isCreator && !isAssignee) {
            throw new AccessDeniedException("You don't have permission to update this task");
        }

        if (StringUtils.hasText(request.getTitle())) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssignedUserId() != null) {
            User assignee = findUserById(request.getAssignedUserId());
            task.setAssignedUser(assignee);
        }

        task = taskRepository.save(task);
        log.info("Task updated successfully: {}", taskId);
        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        log.info("Deleting task: {} by user: {}", taskId, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!task.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("Only the task creator can delete this task");
        }

        taskRepository.delete(task);
        log.info("Task deleted successfully: {}", taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> getTasksByUser(UUID userId, Pageable pageable) {
        log.debug("Fetching tasks for user: {}", userId);
        Page<Task> taskPage = taskRepository.findByUserInvolvement(userId, pageable);
        return buildPagedResponse(taskPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> getTasksByTeam(UUID teamId, Pageable pageable) {
        log.debug("Fetching tasks for team: {}", teamId);
        Page<Task> taskPage = taskRepository.findByTeamId(teamId, pageable);
        return buildPagedResponse(taskPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> filterTasks(TaskStatus status, TaskPriority priority,
                                                    String search, String sortBy, String sortDir,
                                                    Pageable pageable) {
        log.debug("Filtering tasks - status: {}, priority: {}, search: '{}'", status, priority, search);

        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Task> taskPage = taskRepository.findAll(spec, pageable);
        return buildPagedResponse(taskPage);
    }

    private PagedResponse<TaskResponse> buildPagedResponse(Page<Task> taskPage) {
        List<TaskResponse> content = taskPage.getContent().stream()
                .map(TaskMapper::toResponse)
                .toList();

        return PagedResponse.<TaskResponse>builder()
                .content(content)
                .page(taskPage.getNumber())
                .size(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .last(taskPage.isLast())
                .first(taskPage.isFirst())
                .build();
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
