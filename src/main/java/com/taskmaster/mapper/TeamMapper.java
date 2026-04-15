package com.taskmaster.mapper;

import com.taskmaster.dto.response.TeamResponse;
import com.taskmaster.entity.Team;
import com.taskmaster.entity.TeamMembership;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamMapper {

    public static TeamResponse toResponse(Team team, List<TeamMembership> memberships) {
        if (team == null) return null;

        List<TeamResponse.MemberSummary> memberSummaries = memberships.stream()
                .map(m -> TeamResponse.MemberSummary.builder()
                        .id(m.getUser().getId())
                        .username(m.getUser().getUsername())
                        .fullName(m.getUser().getFullName())
                        .role(m.getRole().name())
                        .build())
                .toList();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .owner(TeamResponse.MemberSummary.builder()
                        .id(team.getOwner().getId())
                        .username(team.getOwner().getUsername())
                        .fullName(team.getOwner().getFullName())
                        .role("OWNER")
                        .build())
                .memberCount(memberships.size())
                .members(memberSummaries)
                .createdAt(team.getCreatedAt())
                .build();
    }
}
