package com.unimelb.swen90017.rfo.controller;

import com.unimelb.swen90017.rfo.pojo.vo.SubjectDetailVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetailsService;
import com.unimelb.swen90017.rfo.security.JwtAuthenticationFilter;
import com.unimelb.swen90017.rfo.service.UserSubjectService;
import com.unimelb.swen90017.rfo.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserSubjectController.
 *
 * Covers all 2 endpoints (none require @AuthenticationPrincipal):
 *   - GET /api/users/{userId}/subjectIds
 *   - GET /api/users/{userId}/subjects
 *
 * Both endpoints use catch → Result.error(e.getMessage()) pattern,
 * so errors return HTTP 200 with code=500 in the JSON body.
 *
 * @see com.unimelb.swen90017.rfo.controller.UserSubjectController
 */
@WebMvcTest(UserSubjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserSubjectController Unit Tests")
class UserSubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== Primary service ====================
    @MockBean
    private UserSubjectService userSubjectService;

    // ==================== Security infrastructure ====================
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== getSubjectIds Tests ====================

    @Nested
    @DisplayName("GET /api/users/{userId}/subjectIds")
    class GetSubjectIdsTests {

        @Test
        @DisplayName("USC-001: Should return subject ID list successfully")
        void getSubjectIds_success() throws Exception {
            when(userSubjectService.getSubjectIdsByUserId(1L))
                    .thenReturn(Arrays.asList(10L, 20L, 30L));

            mockMvc.perform(get("/api/users/1/subjectIds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0]").value(10))
                    .andExpect(jsonPath("$.data[1]").value(20))
                    .andExpect(jsonPath("$.data[2]").value(30));

            verify(userSubjectService, times(1)).getSubjectIdsByUserId(1L);
        }

        @Test
        @DisplayName("USC-002: Should return empty list when user has no subjects")
        void getSubjectIds_empty() throws Exception {
            when(userSubjectService.getSubjectIdsByUserId(99L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/users/99/subjectIds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(userSubjectService, times(1)).getSubjectIdsByUserId(99L);
        }

        @Test
        @DisplayName("USC-003: Should return error when service throws exception")
        void getSubjectIds_serviceException() throws Exception {
            when(userSubjectService.getSubjectIdsByUserId(1L))
                    .thenThrow(new RuntimeException("Database connection error"));

            mockMvc.perform(get("/api/users/1/subjectIds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Database connection error"));

            verify(userSubjectService, times(1)).getSubjectIdsByUserId(1L);
        }

        @Test
        @DisplayName("USC-004: Should pass correct userId to service")
        void getSubjectIds_passesCorrectUserId() throws Exception {
            when(userSubjectService.getSubjectIdsByUserId(42L))
                    .thenReturn(Collections.singletonList(100L));

            mockMvc.perform(get("/api/users/42/subjectIds"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0]").value(100));

            verify(userSubjectService, times(1)).getSubjectIdsByUserId(42L);
            verify(userSubjectService, never()).getSubjectDetailsByUserId(anyLong());
        }
    }

    // ==================== getSubjectDetails Tests ====================

    @Nested
    @DisplayName("GET /api/users/{userId}/subjects")
    class GetSubjectDetailsTests {

        @Test
        @DisplayName("USC-005: Should return subject details list successfully")
        void getSubjectDetails_success() throws Exception {
            SubjectDetailVO detail1 = new SubjectDetailVO(10L, "SWEN90017", "Software Requirements");
            SubjectDetailVO detail2 = new SubjectDetailVO(20L, "COMP90015", "Distributed Systems");

            when(userSubjectService.getSubjectDetailsByUserId(1L))
                    .thenReturn(Arrays.asList(detail1, detail2));

            mockMvc.perform(get("/api/users/1/subjects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(10))
                    .andExpect(jsonPath("$.data[0].name").value("SWEN90017"))
                    .andExpect(jsonPath("$.data[0].description").value("Software Requirements"))
                    .andExpect(jsonPath("$.data[1].id").value(20))
                    .andExpect(jsonPath("$.data[1].name").value("COMP90015"));

            verify(userSubjectService, times(1)).getSubjectDetailsByUserId(1L);
        }

        @Test
        @DisplayName("USC-006: Should return empty list when user has no subjects")
        void getSubjectDetails_empty() throws Exception {
            when(userSubjectService.getSubjectDetailsByUserId(99L))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/users/99/subjects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(userSubjectService, times(1)).getSubjectDetailsByUserId(99L);
        }

        @Test
        @DisplayName("USC-007: Should return error when service throws exception")
        void getSubjectDetails_serviceException() throws Exception {
            when(userSubjectService.getSubjectDetailsByUserId(1L))
                    .thenThrow(new RuntimeException("Query failed"));

            mockMvc.perform(get("/api/users/1/subjects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Query failed"));

            verify(userSubjectService, times(1)).getSubjectDetailsByUserId(1L);
        }

        @Test
        @DisplayName("USC-008: Should pass correct userId to service")
        void getSubjectDetails_passesCorrectUserId() throws Exception {
            SubjectDetailVO detail = new SubjectDetailVO(50L, "INFO90002", "Database Systems");

            when(userSubjectService.getSubjectDetailsByUserId(7L))
                    .thenReturn(Collections.singletonList(detail));

            mockMvc.perform(get("/api/users/7/subjects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].name").value("INFO90002"));

            verify(userSubjectService, times(1)).getSubjectDetailsByUserId(7L);
            verify(userSubjectService, never()).getSubjectIdsByUserId(anyLong());
        }
    }
}
