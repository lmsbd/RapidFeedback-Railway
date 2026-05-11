package com.unimelb.swen90017.rfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimelb.swen90017.rfo.pojo.vo.UserProfileVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.UpdatePasswordRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetailsService;
import com.unimelb.swen90017.rfo.security.JwtAuthenticationFilter;
import com.unimelb.swen90017.rfo.service.UserService;
import com.unimelb.swen90017.rfo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 *
 * Covers 3 endpoints:
 *   - POST /api/user/getAllMarkers
 *   - POST /api/user/updateProfile
 *   - POST /api/user/updatePassword
 *
 * Uses @WebMvcTest to load only the Controller layer.
 * Security filters are disabled via @AutoConfigureMockMvc(addFilters = false).
 *
 * Security infrastructure beans (JwtAuthenticationFilter, JwtUtil, CustomUserDetailsService)
 * are mocked via @MockBean so that SecurityConfig can wire up without NoSuchBeanDefinitionException.
 *
 * Result wrapper format:
 *   - Success: { "code": 200, "message": "Operation successful", "data": ..., "timestamp": ... }
 *   - Error:   { "code": 500, "message": "...", "data": null, "timestamp": ... }
 *
 * @see com.unimelb.swen90017.rfo.controller.UserController
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Primary service (业务 Mock) ====================
    @MockBean
    private UserService userService;

    // ==================== Security infrastructure (安保假零件) ====================
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== getAllMarkers Tests ====================

    @Nested
    @DisplayName("POST /api/user/getAllMarkers")
    class GetAllMarkersTests {

        @Test
        @DisplayName("UC-001: Should return list of markers successfully")
        void getAllMarkers_success() throws Exception {
            UserResponseVO marker1 = new UserResponseVO();
            marker1.setUserId(1L);
            marker1.setUserName("Alice");
            marker1.setRole(2);

            UserResponseVO marker2 = new UserResponseVO();
            marker2.setUserId(2L);
            marker2.setUserName("Bob");
            marker2.setRole(2);

            List<UserResponseVO> markers = Arrays.asList(marker1, marker2);
            when(userService.getAllMarkers()).thenReturn(markers);

            mockMvc.perform(post("/api/user/getAllMarkers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].userId").value(1))
                    .andExpect(jsonPath("$.data[0].userName").value("Alice"))
                    .andExpect(jsonPath("$.data[1].userId").value(2))
                    .andExpect(jsonPath("$.data[1].userName").value("Bob"));

            verify(userService, times(1)).getAllMarkers();
        }

        @Test
        @DisplayName("UC-002: Should return empty array when no markers exist")
        void getAllMarkers_empty() throws Exception {
            when(userService.getAllMarkers()).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/user/getAllMarkers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(userService, times(1)).getAllMarkers();
        }

        @Test
        @DisplayName("UC-003: Should return 500 when service throws RuntimeException")
        void getAllMarkers_serviceException() throws Exception {
            when(userService.getAllMarkers()).thenThrow(new RuntimeException("Database error"));

            // Controller catches Exception -> throw new RuntimeException(e)
            // GlobalExceptionHandler catches it -> returns 500 HTTP status
            mockMvc.perform(post("/api/user/getAllMarkers"))
                    .andExpect(status().isInternalServerError());

            verify(userService, times(1)).getAllMarkers();
        }
    }

    // ==================== updateProfile Tests ====================

    @Nested
    @DisplayName("POST /api/user/updateProfile")
    class UpdateProfileTests {

        private UserProfileVO profileRequest;

        @BeforeEach
        void setUp() {
            profileRequest = UserProfileVO.builder()
                    .userId(1L)
                    .username("NewAlice")
                    .build();
        }

        @Test
        @DisplayName("UC-004: Should update profile successfully")
        void updateProfile_success() throws Exception {
            UserProfileVO updatedProfile = UserProfileVO.builder()
                    .userId(1L)
                    .username("NewAlice")
                    .build();

            when(userService.updateProfile(eq(1L), eq("NewAlice"))).thenReturn(updatedProfile);

            mockMvc.perform(post("/api/user/updateProfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(profileRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.username").value("NewAlice"));

            verify(userService, times(1)).updateProfile(eq(1L), eq("NewAlice"));
        }

        @Test
        @DisplayName("UC-005: Should return error when user not found")
        void updateProfile_userNotFound() throws Exception {
            when(userService.updateProfile(eq(999L), any()))
                    .thenThrow(new RuntimeException("User not found"));

            UserProfileVO notFoundRequest = UserProfileVO.builder()
                    .userId(999L)
                    .username("Ghost")
                    .build();

            // Controller catches Exception -> returns Result.error(e.getMessage())
            // Result.error("...") -> code=500, message="..."
            mockMvc.perform(post("/api/user/updateProfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(notFoundRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updateProfile(eq(999L), eq("Ghost"));
        }

        @Test
        @DisplayName("UC-006: Should handle null username gracefully")
        void updateProfile_nullUsername() throws Exception {
            UserProfileVO nullUsernameRequest = UserProfileVO.builder()
                    .userId(1L)
                    .username(null)
                    .build();

            UserProfileVO profile = UserProfileVO.builder()
                    .userId(1L)
                    .username(null)
                    .build();

            when(userService.updateProfile(eq(1L), eq(null))).thenReturn(profile);

            mockMvc.perform(post("/api/user/updateProfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nullUsernameRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.userId").value(1));

            verify(userService, times(1)).updateProfile(eq(1L), eq(null));
        }

        @Test
        @DisplayName("UC-007: Should return error when duplicate username")
        void updateProfile_duplicateUsername() throws Exception {
            when(userService.updateProfile(eq(1L), eq("ExistingName")))
                    .thenThrow(new RuntimeException("Username already exists"));

            UserProfileVO dupRequest = UserProfileVO.builder()
                    .userId(1L)
                    .username("ExistingName")
                    .build();

            mockMvc.perform(post("/api/user/updateProfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dupRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Username already exists"));

            verify(userService, times(1)).updateProfile(eq(1L), eq("ExistingName"));
        }
    }

    // ==================== updatePassword Tests ====================

    @Nested
    @DisplayName("POST /api/user/updatePassword")
    class UpdatePasswordTests {

        private UpdatePasswordRequestVO passwordRequest;

        @BeforeEach
        void setUp() {
            passwordRequest = new UpdatePasswordRequestVO();
            passwordRequest.setUserId(1L);
            passwordRequest.setOldPassword("oldPass123");
            passwordRequest.setNewPassword("newPass456");
        }

        @Test
        @DisplayName("UC-008: Should update password successfully")
        void updatePassword_success() throws Exception {
            doNothing().when(userService).updatePassword(any(UpdatePasswordRequestVO.class));

            mockMvc.perform(post("/api/user/updatePassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(userService, times(1)).updatePassword(any(UpdatePasswordRequestVO.class));
        }

        @Test
        @DisplayName("UC-009: Should return error when old password is incorrect")
        void updatePassword_wrongOldPassword() throws Exception {
            doThrow(new RuntimeException("Incorrect old password"))
                    .when(userService).updatePassword(any(UpdatePasswordRequestVO.class));

            mockMvc.perform(post("/api/user/updatePassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Incorrect old password"));

            verify(userService, times(1)).updatePassword(any(UpdatePasswordRequestVO.class));
        }

        @Test
        @DisplayName("UC-010: Should return error when user not found")
        void updatePassword_userNotFound() throws Exception {
            doThrow(new RuntimeException("User not found"))
                    .when(userService).updatePassword(any(UpdatePasswordRequestVO.class));

            mockMvc.perform(post("/api/user/updatePassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("User not found"));

            verify(userService, times(1)).updatePassword(any(UpdatePasswordRequestVO.class));
        }

        @Test
        @DisplayName("UC-011: Should handle null fields in password request")
        void updatePassword_nullFields() throws Exception {
            UpdatePasswordRequestVO nullRequest = new UpdatePasswordRequestVO();
            nullRequest.setUserId(null);
            nullRequest.setOldPassword(null);
            nullRequest.setNewPassword(null);

            doThrow(new RuntimeException("Missing required fields"))
                    .when(userService).updatePassword(any(UpdatePasswordRequestVO.class));

            mockMvc.perform(post("/api/user/updatePassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nullRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Missing required fields"));

            verify(userService, times(1)).updatePassword(any(UpdatePasswordRequestVO.class));
        }

        @Test
        @DisplayName("UC-012: Should accept when old and new passwords are the same")
        void updatePassword_sameOldAndNew() throws Exception {
            UpdatePasswordRequestVO samePassRequest = new UpdatePasswordRequestVO();
            samePassRequest.setUserId(1L);
            samePassRequest.setOldPassword("samePass");
            samePassRequest.setNewPassword("samePass");

            doNothing().when(userService).updatePassword(any(UpdatePasswordRequestVO.class));

            mockMvc.perform(post("/api/user/updatePassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(samePassRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userService, times(1)).updatePassword(any(UpdatePasswordRequestVO.class));
        }
    }
}
