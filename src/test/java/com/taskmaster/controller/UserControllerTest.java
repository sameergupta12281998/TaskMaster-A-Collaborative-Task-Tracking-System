package com.taskmaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmaster.dto.request.UserUpdateRequest;
import com.taskmaster.dto.response.UserResponse;
import com.taskmaster.security.CustomUserPrincipal;
import com.taskmaster.service.UserService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private CustomUserPrincipal createPrincipal() {
        return new CustomUserPrincipal(
                UUID.randomUUID(), "testuser", "test@example.com",
                "password", true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("GET /api/users/me - Success")
    void getCurrentUser_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();

        UserResponse response = UserResponse.builder()
                .id(principal.getId())
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .createdAt(Instant.now())
                .build();

        when(userService.getCurrentUser(principal.getId())).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("PUT /api/users/update - Success")
    void updateUser_Success() throws Exception {
        CustomUserPrincipal principal = createPrincipal();

        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .bio("Updated bio")
                .build();

        UserResponse response = UserResponse.builder()
                .id(principal.getId())
                .username("testuser")
                .email("test@example.com")
                .fullName("Updated Name")
                .bio("Updated bio")
                .createdAt(Instant.now())
                .build();

        when(userService.updateUser(eq(principal.getId()), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/update")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("PUT /api/users/update - Validation error")
    void updateUser_ValidationError() throws Exception {
        CustomUserPrincipal principal = createPrincipal();

        String longName = "x".repeat(101);
        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName(longName)
                .build();

        mockMvc.perform(put("/api/users/update")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.fullName").exists());
    }

    @Test
    @DisplayName("GET /api/users/me - Unauthorized")
    void getCurrentUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
