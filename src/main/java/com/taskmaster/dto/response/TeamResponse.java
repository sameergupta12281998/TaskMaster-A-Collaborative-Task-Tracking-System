package com.taskmaster.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamResponse {

    private UUID id;
    private String name;
    private String description;
    private MemberSummary owner;
    private int memberCount;
    private List<MemberSummary> members;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberSummary {
        private UUID id;
        private String username;
        private String fullName;
        private String role;
    }
}
