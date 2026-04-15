package com.taskmaster.repository;

import com.taskmaster.entity.TeamMembership;
import com.taskmaster.entity.enums.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    Optional<TeamMembership> findByTeamIdAndUserId(UUID teamId, UUID userId);

    List<TeamMembership> findByTeamId(UUID teamId);

    List<TeamMembership> findByUserId(UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserIdAndRole(UUID teamId, UUID userId, TeamRole role);

    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}
