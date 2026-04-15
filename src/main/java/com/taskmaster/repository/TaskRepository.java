package com.taskmaster.repository;

import com.taskmaster.entity.Task;
import com.taskmaster.entity.enums.TaskPriority;
import com.taskmaster.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Page<Task> findByAssignedUserId(UUID userId, Pageable pageable);

    Page<Task> findByCreatedById(UUID userId, Pageable pageable);

    Page<Task> findByTeamId(UUID teamId, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByPriority(TaskPriority priority, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.assignedUser.id = :userId OR t.createdBy.id = :userId")
    Page<Task> findByUserInvolvement(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignedUser LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.team WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT t FROM Task t WHERE " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
