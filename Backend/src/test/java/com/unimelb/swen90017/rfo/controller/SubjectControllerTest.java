package com.unimelb.swen90017.rfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimelb.swen90017.rfo.pojo.vo.StudentResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.SubjectDetailVO;
import com.unimelb.swen90017.rfo.pojo.vo.SubjectWholeDetailVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.StudentRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SubjectRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.UserRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetailsService;
import com.unimelb.swen90017.rfo.security.JwtAuthenticationFilter;
import com.unimelb.swen90017.rfo.service.SubjectService;
import com.unimelb.swen90017.rfo.util.JwtUtil;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SubjectController.
 *
 * Covers endpoints that do NOT require @AuthenticationPrincipal:
 *   - POST /api/subjects/getStudentList
 *   - POST /api/subjects/getSubjectIds
 *   - POST /api/subjects/getSubjectList
 *   - GET  /api/subjects/getSubjectsDetail
 *
 * Endpoints SKIPPED (require @AuthenticationPrincipal / @PreAuthorize):
 *   - POST /api/subjects/save  (requires ADMIN + CustomUserDetails)
 *   - POST /api/subjects/updateSubjectsDetail (requires CustomUserDetails)
 *   These are covered by SecurityAuthorizationTest / integration tests.
 *
 * Uses @WebMvcTest + @AutoConfigureMockMvc(addFilters = false)
 * Security beans mocked to allow SecurityConfig to load.
 *
 * @see com.unimelb.swen90017.rfo.controller.SubjectController
 */
@WebMvcTest(SubjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SubjectController Unit Tests")
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Primary service ====================
    @MockBean
    private SubjectService subjectService;

    // ==================== Security infrastructure ====================
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== getStudentList Tests ====================

    @Nested
    @DisplayName("POST /api/subjects/getStudentList")
    class GetStudentListTests {

        @Test
        @DisplayName("SC-010: Should return student list successfully")
        void getStudentList_success() throws Exception {
            StudentResponseVO student1 = new StudentResponseVO();
            student1.setId(1L);
            student1.setStudentId(1001L);
            student1.setFirstName("Alice");
            student1.setSurname("Wang");
            student1.setEmail("alice@example.com");

            StudentResponseVO student2 = new StudentResponseVO();
            student2.setId(2L);
            student2.setStudentId(1002L);
            student2.setFirstName("Bob");
            student2.setSurname("Li");
            student2.setEmail("bob@example.com");

            List<StudentResponseVO> students = Arrays.asList(student1, student2);

            StudentRequestVO request = new StudentRequestVO();
            request.setSubjectId(100L);

            when(subjectService.getStudentList(any(StudentRequestVO.class))).thenReturn(students);

            mockMvc.perform(post("/api/subjects/getStudentList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$.data[1].firstName").value("Bob"));

            verify(subjectService, times(1)).getStudentList(any(StudentRequestVO.class));
        }

        @Test
        @DisplayName("SC-011: Should return empty array when no students")
        void getStudentList_empty() throws Exception {
            StudentRequestVO request = new StudentRequestVO();
            request.setSubjectId(100L);

            when(subjectService.getStudentList(any(StudentRequestVO.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/subjects/getStudentList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(subjectService, times(1)).getStudentList(any(StudentRequestVO.class));
        }

        @Test
        @DisplayName("SC-012: Should return error when service throws exception")
        void getStudentList_serviceException() throws Exception {
            StudentRequestVO request = new StudentRequestVO();
            request.setSubjectId(100L);

            when(subjectService.getStudentList(any(StudentRequestVO.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(post("/api/subjects/getStudentList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Database connection failed"));

            verify(subjectService, times(1)).getStudentList(any(StudentRequestVO.class));
        }
    }

    // ==================== getSubjectIds Tests ====================

    @Nested
    @DisplayName("POST /api/subjects/getSubjectIds")
    class GetSubjectIdsTests {

        @Test
        @DisplayName("SC-EXT-001: Should return subject ID list for valid user")
        void getSubjectIds_success() throws Exception {
            UserRequestVO request = new UserRequestVO();
            request.setUserId(1L);

            when(subjectService.getSubjectIds(any(UserRequestVO.class)))
                    .thenReturn(Arrays.asList(10L, 20L, 30L));

            mockMvc.perform(post("/api/subjects/getSubjectIds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0]").value(10))
                    .andExpect(jsonPath("$.data[1]").value(20))
                    .andExpect(jsonPath("$.data[2]").value(30));

            verify(subjectService, times(1)).getSubjectIds(any(UserRequestVO.class));
        }

        @Test
        @DisplayName("SC-EXT-002: Should return empty array when user has no subjects")
        void getSubjectIds_empty() throws Exception {
            UserRequestVO request = new UserRequestVO();
            request.setUserId(1L);

            when(subjectService.getSubjectIds(any(UserRequestVO.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/subjects/getSubjectIds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(subjectService, times(1)).getSubjectIds(any(UserRequestVO.class));
        }

        @Test
        @DisplayName("SC-EXT-003: Should return error when service throws exception")
        void getSubjectIds_serviceException() throws Exception {
            UserRequestVO request = new UserRequestVO();
            request.setUserId(1L);

            when(subjectService.getSubjectIds(any(UserRequestVO.class)))
                    .thenThrow(new RuntimeException("Query failed"));

            mockMvc.perform(post("/api/subjects/getSubjectIds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Query failed"));

            verify(subjectService, times(1)).getSubjectIds(any(UserRequestVO.class));
        }
    }

    // ==================== getSubjectList Tests ====================

    @Nested
    @DisplayName("POST /api/subjects/getSubjectList")
    class GetSubjectListTests {

        @Test
        @DisplayName("SC-EXT-004: Should return subject detail list successfully")
        void getSubjectList_success() throws Exception {
            UserRequestVO request = new UserRequestVO();
            request.setUserId(1L);

            SubjectDetailVO detail1 = new SubjectDetailVO(1L, "SWEN90017", "Software Quality");
            SubjectDetailVO detail2 = new SubjectDetailVO(2L, "COMP90015", "Distributed Systems");

            when(subjectService.getSubjectList(any(UserRequestVO.class)))
                    .thenReturn(Arrays.asList(detail1, detail2));

            mockMvc.perform(post("/api/subjects/getSubjectList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("SWEN90017"))
                    .andExpect(jsonPath("$.data[1].name").value("COMP90015"));

            verify(subjectService, times(1)).getSubjectList(any(UserRequestVO.class));
        }

        @Test
        @DisplayName("SC-EXT-005: Should return empty array when no subjects")
        void getSubjectList_empty() throws Exception {
            UserRequestVO request = new UserRequestVO();
            request.setUserId(1L);

            when(subjectService.getSubjectList(any(UserRequestVO.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/subjects/getSubjectList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(subjectService, times(1)).getSubjectList(any(UserRequestVO.class));
        }

        @Test
        @DisplayName("SC-EXT-006: Should return error when service throws exception")
        void getSubjectList_serviceException() throws Exception {
            UserRequestVO request = new UserRequestVO();
            request.setUserId(1L);

            when(subjectService.getSubjectList(any(UserRequestVO.class)))
                    .thenThrow(new RuntimeException("Service unavailable"));

            mockMvc.perform(post("/api/subjects/getSubjectList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Service unavailable"));

            verify(subjectService, times(1)).getSubjectList(any(UserRequestVO.class));
        }
    }

    // ==================== getSubjectsDetail Tests ====================

    @Nested
    @DisplayName("GET /api/subjects/getSubjectsDetail")
    class GetSubjectsDetailTests {

        @Test
        @DisplayName("SC-EXT-007: Should return full subject detail with students and markers")
        void getSubjectsDetail_success() throws Exception {
            SubjectWholeDetailVO detail = new SubjectWholeDetailVO();
            detail.setId(1L);
            detail.setName("SWEN90017");
            detail.setDescription("Software Quality");

            StudentResponseVO student = new StudentResponseVO();
            student.setId(1L);
            student.setFirstName("Alice");
            detail.setStudents(Collections.singletonList(student));

            UserResponseVO marker = new UserResponseVO();
            marker.setUserId(10L);
            marker.setUserName("DrSmith");
            detail.setMarkers(Collections.singletonList(marker));

            when(subjectService.getSubjectsDetail(eq(1L))).thenReturn(detail);

            mockMvc.perform(get("/api/subjects/getSubjectsDetail")
                            .param("subjectId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("SWEN90017"))
                    .andExpect(jsonPath("$.data.students").isArray())
                    .andExpect(jsonPath("$.data.students.length()").value(1))
                    .andExpect(jsonPath("$.data.markers").isArray())
                    .andExpect(jsonPath("$.data.markers.length()").value(1))
                    .andExpect(jsonPath("$.data.markers[0].userName").value("DrSmith"));

            verify(subjectService, times(1)).getSubjectsDetail(eq(1L));
        }

        @Test
        @DisplayName("SC-EXT-008: Should return error when service throws exception")
        void getSubjectsDetail_serviceException() throws Exception {
            when(subjectService.getSubjectsDetail(eq(999L)))
                    .thenThrow(new RuntimeException("Subject not found"));

            mockMvc.perform(get("/api/subjects/getSubjectsDetail")
                            .param("subjectId", "999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Subject not found"));

            verify(subjectService, times(1)).getSubjectsDetail(eq(999L));
        }
    }
}
