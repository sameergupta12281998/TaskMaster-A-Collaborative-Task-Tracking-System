package com.taskmaster.repository;

import com.taskmaster.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    @Query("SELECT a FROM Attachment a JOIN FETCH a.uploadedBy WHERE a.task.id = :taskId ORDER BY a.createdAt DESC")
    List<Attachment> findByTaskIdWithUploader(@Param("taskId") UUID taskId);

    Optional<Attachment> findByIdAndTaskId(UUID id, UUID taskId);
}
