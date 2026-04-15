package com.taskmaster.service;

import com.taskmaster.dto.request.TeamCreateRequest;
import com.taskmaster.dto.response.TeamResponse;

import java.util.UUID;

public interface TeamService {

    TeamResponse createTeam(UUID userId, TeamCreateRequest request);

    TeamResponse getTeamById(UUID teamId, UUID userId);

    TeamResponse joinTeam(UUID teamId, UUID userId);

    TeamResponse addMember(UUID teamId, UUID ownerId, UUID memberId);

    void removeMember(UUID teamId, UUID ownerId, UUID memberId);
}
