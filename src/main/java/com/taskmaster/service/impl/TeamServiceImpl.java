package com.taskmaster.service.impl;

import com.taskmaster.dto.request.TeamCreateRequest;
import com.taskmaster.dto.response.TeamResponse;
import com.taskmaster.entity.Team;
import com.taskmaster.entity.TeamMembership;
import com.taskmaster.entity.User;
import com.taskmaster.entity.enums.TeamRole;
import com.taskmaster.exception.AccessDeniedException;
import com.taskmaster.exception.BadRequestException;
import com.taskmaster.exception.DuplicateResourceException;
import com.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.mapper.TeamMapper;
import com.taskmaster.repository.TeamMembershipRepository;
import com.taskmaster.repository.TeamRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TeamResponse createTeam(UUID userId, TeamCreateRequest request) {
        log.info("Creating team '{}' by user: {}", request.getName(), userId);

        User owner = findUserById(userId);

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        team = teamRepository.save(team);

        // Add owner as a member with OWNER role
        TeamMembership membership = TeamMembership.builder()
                .team(team)
                .user(owner)
                .role(TeamRole.OWNER)
                .build();
        teamMembershipRepository.save(membership);

        log.info("Team created successfully with id: {}", team.getId());

        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(team.getId());
        return TeamMapper.toResponse(team, memberships);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID teamId, UUID userId) {
        log.debug("Fetching team: {} for user: {}", teamId, userId);

        Team team = teamRepository.findByIdWithOwner(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        // Verify the user is a member
        if (!teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new AccessDeniedException("You are not a member of this team");
        }

        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(teamId);
        return TeamMapper.toResponse(team, memberships);
    }

    @Override
    @Transactional
    public TeamResponse joinTeam(UUID teamId, UUID userId) {
        log.info("User {} requesting to join team: {}", userId, teamId);

        Team team = teamRepository.findByIdWithOwner(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User user = findUserById(userId);

        if (teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new DuplicateResourceException("You are already a member of this team");
        }

        TeamMembership membership = TeamMembership.builder()
                .team(team)
                .user(user)
                .role(TeamRole.MEMBER)
                .build();
        teamMembershipRepository.save(membership);

        log.info("User {} joined team {} successfully", userId, teamId);

        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(teamId);
        return TeamMapper.toResponse(team, memberships);
    }

    @Override
    @Transactional
    public TeamResponse addMember(UUID teamId, UUID ownerId, UUID memberId) {
        log.info("Adding member {} to team {} by owner {}", memberId, teamId, ownerId);

        Team team = teamRepository.findByIdWithOwner(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        // Verify the requester is owner or admin
        verifyTeamAdminAccess(teamId, ownerId);

        User newMember = findUserById(memberId);

        if (teamMembershipRepository.existsByTeamIdAndUserId(teamId, memberId)) {
            throw new DuplicateResourceException("User is already a member of this team");
        }

        TeamMembership membership = TeamMembership.builder()
                .team(team)
                .user(newMember)
                .role(TeamRole.MEMBER)
                .build();
        teamMembershipRepository.save(membership);

        log.info("Member {} added to team {} successfully", memberId, teamId);

        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(teamId);
        return TeamMapper.toResponse(team, memberships);
    }

    @Override
    @Transactional
    public void removeMember(UUID teamId, UUID ownerId, UUID memberId) {
        log.info("Removing member {} from team {} by owner {}", memberId, teamId, ownerId);

        teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        verifyTeamAdminAccess(teamId, ownerId);

        if (ownerId.equals(memberId)) {
            throw new BadRequestException("Team owner cannot be removed. Transfer ownership first.");
        }

        if (!teamMembershipRepository.existsByTeamIdAndUserId(teamId, memberId)) {
            throw new ResourceNotFoundException("TeamMembership", "userId", memberId);
        }

        teamMembershipRepository.deleteByTeamIdAndUserId(teamId, memberId);
        log.info("Member {} removed from team {} successfully", memberId, teamId);
    }

    private void verifyTeamAdminAccess(UUID teamId, UUID userId) {
        boolean isOwner = teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.OWNER);
        boolean isAdmin = teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Only team owners and admins can perform this action");
        }
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
