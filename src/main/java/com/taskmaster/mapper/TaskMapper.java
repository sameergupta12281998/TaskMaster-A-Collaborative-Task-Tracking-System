package com.taskmaster.mapper;

import com.taskmaster.dto.response.TaskResponse;
import com.taskmaster.entity.Task;
import com.taskmaster.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskMapper {

    public static TaskResponse toResponse(Task task) {
        if (task == null) return null;
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUser(toUserSummary(task.getAssignedUser()))
                .createdBy(toUserSummary(task.getCreatedBy()))
                .team(task.getTeam() != null ? TaskResponse.TeamSummary.builder()
                        .id(task.getTeam().getId())
                        .name(task.getTeam().getName())
                        .build() : null)
                .commentCount(task.getComments() != null ? task.getComments().size() : 0)
                .attachmentCount(task.getAttachments() != null ? task.getAttachments().size() : 0)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public static TaskResponse toResponseWithCounts(Task task, int commentCount, int attachmentCount) {
        if (task == null) return null;
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .assignedUser(toUserSummary(task.getAssignedUser()))
                .createdBy(toUserSummary(task.getCreatedBy()))
                .team(task.getTeam() != null ? TaskResponse.TeamSummary.builder()
                        .id(task.getTeam().getId())
                        .name(task.getTeam().getName())
                        .build() : null)
                .commentCount(commentCount)
                .attachmentCount(attachmentCount)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private static TaskResponse.UserSummary toUserSummary(User user) {
        if (user == null) return null;
        return TaskResponse.UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .build();
    }
}
