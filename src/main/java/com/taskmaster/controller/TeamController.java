package com.taskmaster.controller;

import com.taskmaster.dto.request.TeamCreateRequest;
import com.taskmaster.dto.request.TeamMemberRequest;
import com.taskmaster.dto.response.ApiResponse;
import com.taskmaster.dto.response.TeamResponse;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Teams", description = "Team/Project management APIs")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody TeamCreateRequest request) {
        TeamResponse response = teamService.createTeam(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Team created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team details")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        TeamResponse response = teamService.getTeamById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join a team")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id) {
        TeamResponse response = teamService.joinTeam(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Joined team successfully", response));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member to the team")
    public ResponseEntity<ApiResponse<TeamResponse>> addMember(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody TeamMemberRequest request) {
        TeamResponse response = teamService.addMember(id, principal.getId(), request.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Member added successfully", response));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from the team")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID memberId) {
        teamService.removeMember(id, principal.getId(), memberId);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully"));
    }
}
