package com.taskmaster.repository;

import com.taskmaster.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.owner WHERE t.id = :id")
    Optional<Team> findByIdWithOwner(@Param("id") UUID id);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.memberships m WHERE m.user.id = :userId")
    Page<Team> findTeamsByUserId(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
