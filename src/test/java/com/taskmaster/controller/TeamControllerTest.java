package com.taskmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.dto.request.TeamCreateRequest;
import com.taskmaster.dto.request.TeamMemberRequest;
import com.taskmaster.dto.response.TeamResponse;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamService teamService;

    private CustomUserPrincipal createPrincipal() {
        return new CustomUserPrincipal(
                UUID.randomUUID(), "testuser", "test@example.com",
                "password", true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private TeamResponse buildTeamResponse(UUID teamId) {
        return TeamResponse.builder()
                .id(teamId)
                .name("Platform Team")
                .description("Core platform")
                .memberCount(1)
                .members(List.of(
                        TeamResponse.MemberSummary.builder()
                                .id(UUID.randomUUID())
                                .username("testuser")
                                .fullName("Test User")
                                .role("OWNER")
                                .build()
                ))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/teams - Success")
    void createTeam_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID teamId = UUID.randomUUID();

        TeamCreateRequest request = TeamCreateRequest.builder()
                .name("Platform Team")
                .description("Core platform")
                .build();

        when(teamService.createTeam(eq(principal.getId()), any(TeamCreateRequest.class)))
                .thenReturn(buildTeamResponse(teamId));

        mockMvc.perform(post("/api/teams")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Platform Team"));
    }

    @Test
    @DisplayName("GET /api/teams/{id} - Success")
    void getTeam_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID teamId = UUID.randomUUID();

        when(teamService.getTeamById(teamId, principal.getId())).thenReturn(buildTeamResponse(teamId));

        mockMvc.perform(get("/api/teams/{id}", teamId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(teamId.toString()));
    }

    @Test
    @DisplayName("POST /api/teams/{id}/join - Success")
    void joinTeam_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID teamId = UUID.randomUUID();

        TeamResponse response = buildTeamResponse(teamId);
        response.setMemberCount(2);

        when(teamService.joinTeam(teamId, principal.getId())).thenReturn(response);

        mockMvc.perform(post("/api/teams/{id}/join", teamId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberCount").value(2));
    }

    @Test
    @DisplayName("POST /api/teams/{id}/members - Validation error")
    void addMember_ValidationError() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID teamId = UUID.randomUUID();

        TeamMemberRequest request = TeamMemberRequest.builder().build();

        mockMvc.perform(post("/api/teams/{id}/members", teamId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userId").exists());
    }

    @Test
    @DisplayName("DELETE /api/teams/{id}/members/{memberId} - Success")
    void removeMember_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();
        UUID teamId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        doNothing().when(teamService).removeMember(teamId, principal.getId(), memberId);

        mockMvc.perform(delete("/api/teams/{id}/members/{memberId}", teamId, memberId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Member removed successfully"));
    }
}
