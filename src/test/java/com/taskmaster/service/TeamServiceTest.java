package com.taskmaster.service;

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
import com.taskmaster.repository.TeamMembershipRepository;
import com.taskmaster.repository.TeamRepository;
import com.taskmaster.repository.UserRepository;
import com.taskmaster.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMembershipRepository teamMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamServiceImpl teamService;

    private UUID userId;
    private UUID teamId;
    private User user;
    private Team team;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .fullName("Test User")
                .build();
        user.setId(userId);
        user.setCreatedAt(Instant.now());

        team = Team.builder()
                .name("Test Team")
                .description("Test Description")
                .owner(user)
                .build();
        team.setId(teamId);
        team.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should create team successfully")
    void createTeam_Success() {
        TeamCreateRequest request = TeamCreateRequest.builder()
                .name("New Team")
                .description("Team Description")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(teamMembershipRepository.save(any(TeamMembership.class))).thenAnswer(i -> i.getArgument(0));

        TeamMembership membership = TeamMembership.builder()
                .team(team).user(user).role(TeamRole.OWNER).build();
        membership.setId(UUID.randomUUID());
        membership.setCreatedAt(Instant.now());
        when(teamMembershipRepository.findByTeamId(teamId)).thenReturn(List.of(membership));

        TeamResponse response = teamService.createTeam(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Team");
        assertThat(response.getMemberCount()).isEqualTo(1);
        verify(teamRepository).save(any(Team.class));
        verify(teamMembershipRepository).save(any(TeamMembership.class));
    }

    @Test
    @DisplayName("Should join team successfully")
    void joinTeam_Success() {
        UUID newUserId = UUID.randomUUID();
        User newUser = User.builder().username("newuser").email("new@test.com").password("p").build();
        newUser.setId(newUserId);
        newUser.setCreatedAt(Instant.now());

        when(teamRepository.findByIdWithOwner(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
        when(teamMembershipRepository.existsByTeamIdAndUserId(teamId, newUserId)).thenReturn(false);
        when(teamMembershipRepository.save(any(TeamMembership.class))).thenAnswer(i -> i.getArgument(0));

        TeamMembership ownerMembership = TeamMembership.builder()
                .team(team).user(user).role(TeamRole.OWNER).build();
        ownerMembership.setId(UUID.randomUUID());
        ownerMembership.setCreatedAt(Instant.now());
        TeamMembership newMembership = TeamMembership.builder()
                .team(team).user(newUser).role(TeamRole.MEMBER).build();
        newMembership.setId(UUID.randomUUID());
        newMembership.setCreatedAt(Instant.now());
        when(teamMembershipRepository.findByTeamId(teamId)).thenReturn(List.of(ownerMembership, newMembership));

        TeamResponse response = teamService.joinTeam(teamId, newUserId);

        assertThat(response.getMemberCount()).isEqualTo(2);
        verify(teamMembershipRepository).save(any(TeamMembership.class));
    }

    @Test
    @DisplayName("Should throw exception when joining team already a member")
    void joinTeam_DuplicateMember() {
        when(teamRepository.findByIdWithOwner(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(true);

        assertThatThrownBy(() -> teamService.joinTeam(teamId, userId))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should throw exception when team not found")
    void getTeam_NotFound() {
        when(teamRepository.findByIdWithOwner(teamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(teamId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should deny access for non-member")
    void getTeam_AccessDenied() {
        when(teamRepository.findByIdWithOwner(teamId)).thenReturn(Optional.of(team));
        when(teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)).thenReturn(false);

        assertThatThrownBy(() -> teamService.getTeamById(teamId, userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should add member successfully when requester is owner")
    void addMember_Success() {
        UUID memberId = UUID.randomUUID();
        User member = User.builder().username("member").email("member@test.com").password("p").build();
        member.setId(memberId);

        when(teamRepository.findByIdWithOwner(teamId)).thenReturn(Optional.of(team));
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.OWNER)).thenReturn(true);
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN)).thenReturn(false);
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(teamMembershipRepository.existsByTeamIdAndUserId(teamId, memberId)).thenReturn(false);
        when(teamMembershipRepository.save(any(TeamMembership.class))).thenAnswer(i -> i.getArgument(0));

        TeamMembership ownerMembership = TeamMembership.builder().team(team).user(user).role(TeamRole.OWNER).build();
        ownerMembership.setId(UUID.randomUUID());
        TeamMembership memberMembership = TeamMembership.builder().team(team).user(member).role(TeamRole.MEMBER).build();
        memberMembership.setId(UUID.randomUUID());
        when(teamMembershipRepository.findByTeamId(teamId)).thenReturn(List.of(ownerMembership, memberMembership));

        TeamResponse response = teamService.addMember(teamId, userId, memberId);

        assertThat(response.getMemberCount()).isEqualTo(2);
        verify(teamMembershipRepository).save(any(TeamMembership.class));
    }

    @Test
    @DisplayName("Should deny add member when requester is not admin or owner")
    void addMember_AccessDenied() {
        UUID memberId = UUID.randomUUID();

        when(teamRepository.findByIdWithOwner(teamId)).thenReturn(Optional.of(team));
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.OWNER)).thenReturn(false);
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> teamService.addMember(teamId, userId, memberId))
                .isInstanceOf(AccessDeniedException.class);

        verify(teamMembershipRepository, never()).save(any(TeamMembership.class));
    }

    @Test
    @DisplayName("Should remove member successfully")
    void removeMember_Success() {
        UUID memberId = UUID.randomUUID();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.OWNER)).thenReturn(true);
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN)).thenReturn(false);
        when(teamMembershipRepository.existsByTeamIdAndUserId(teamId, memberId)).thenReturn(true);

        teamService.removeMember(teamId, userId, memberId);

        verify(teamMembershipRepository).deleteByTeamIdAndUserId(teamId, memberId);
    }

    @Test
    @DisplayName("Should not remove owner from team")
    void removeMember_OwnerCannotBeRemoved() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.OWNER)).thenReturn(true);
        when(teamMembershipRepository.existsByTeamIdAndUserIdAndRole(teamId, userId, TeamRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> teamService.removeMember(teamId, userId, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be removed");
    }
}
