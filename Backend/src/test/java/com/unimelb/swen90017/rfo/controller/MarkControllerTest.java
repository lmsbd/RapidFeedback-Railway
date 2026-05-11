package com.unimelb.swen90017.rfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimelb.swen90017.rfo.pojo.dto.GroupStudentMarkDTO;
import com.unimelb.swen90017.rfo.pojo.dto.MarkDetailDTO;
import com.unimelb.swen90017.rfo.pojo.vo.GroupMarkResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SaveGroupMarkRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SaveMarkRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetailsService;
import com.unimelb.swen90017.rfo.security.JwtAuthenticationFilter;
import com.unimelb.swen90017.rfo.service.MarkService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MarkController.
 *
 * Covers all 3 endpoints (none require @AuthenticationPrincipal):
 *   - POST /api/mark/saveMark
 *   - POST /api/mark/saveGroupMark
 *   - GET  /api/mark/getGroupMark
 *
 * Note: These endpoints do NOT have try-catch blocks in the controller,
 * so exceptions propagate to GlobalExceptionHandler → HTTP 500.
 *
 * @see com.unimelb.swen90017.rfo.controller.MarkController
 */
@WebMvcTest(MarkController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MarkController Unit Tests")
class MarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Primary service ====================
    @MockBean
    private MarkService markService;

    // ==================== Security infrastructure ====================
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== saveMark Tests ====================

    @Nested
    @DisplayName("POST /api/mark/saveMark")
    class SaveMarkTests {

        @Test
        @DisplayName("MC-001: Should save individual mark successfully")
        void saveMark_success() throws Exception {
            SaveMarkRequestVO request = new SaveMarkRequestVO();
            request.setProjectId(1L);
            request.setStudentId(100L);

            MarkDetailDTO detail = new MarkDetailDTO();
            request.setDetails(Collections.singletonList(detail));

            doNothing().when(markService).saveMark(any(SaveMarkRequestVO.class));

            mockMvc.perform(post("/api/mark/saveMark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(markService, times(1)).saveMark(any(SaveMarkRequestVO.class));
        }

        @Test
        @DisplayName("MC-002: Should return 500 when service throws exception")
        void saveMark_serviceException() throws Exception {
            SaveMarkRequestVO request = new SaveMarkRequestVO();
            request.setProjectId(1L);
            request.setStudentId(100L);

            doThrow(new RuntimeException("Save failed"))
                    .when(markService).saveMark(any(SaveMarkRequestVO.class));

            mockMvc.perform(post("/api/mark/saveMark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());

            verify(markService, times(1)).saveMark(any(SaveMarkRequestVO.class));
        }

        @Test
        @DisplayName("MC-003: Should pass request body correctly to service")
        void saveMark_passesCorrectRequest() throws Exception {
            SaveMarkRequestVO request = new SaveMarkRequestVO();
            request.setProjectId(5L);
            request.setStudentId(200L);
            request.setDetails(Collections.emptyList());

            doNothing().when(markService).saveMark(any(SaveMarkRequestVO.class));

            mockMvc.perform(post("/api/mark/saveMark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(markService, times(1)).saveMark(any(SaveMarkRequestVO.class));
            verify(markService, never()).saveGroupMark(any());
            verify(markService, never()).getGroupMark(anyLong(), anyLong());
        }
    }

    // ==================== saveGroupMark Tests ====================

    @Nested
    @DisplayName("POST /api/mark/saveGroupMark")
    class SaveGroupMarkTests {

        @Test
        @DisplayName("MC-004: Should save group marks successfully")
        void saveGroupMark_success() throws Exception {
            SaveGroupMarkRequestVO request = new SaveGroupMarkRequestVO();
            request.setProjectId(1L);
            request.setGroupId(10L);
            request.setComment("Good team collaboration");

            GroupStudentMarkDTO student1 = new GroupStudentMarkDTO();
            GroupStudentMarkDTO student2 = new GroupStudentMarkDTO();
            request.setStudents(Arrays.asList(student1, student2));

            doNothing().when(markService).saveGroupMark(any(SaveGroupMarkRequestVO.class));

            mockMvc.perform(post("/api/mark/saveGroupMark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(markService, times(1)).saveGroupMark(any(SaveGroupMarkRequestVO.class));
        }

        @Test
        @DisplayName("MC-005: Should save group marks with null students list")
        void saveGroupMark_nullStudents() throws Exception {
            SaveGroupMarkRequestVO request = new SaveGroupMarkRequestVO();
            request.setProjectId(1L);
            request.setGroupId(10L);
            request.setComment("N/A");
            request.setStudents(null);

            doNothing().when(markService).saveGroupMark(any(SaveGroupMarkRequestVO.class));

            mockMvc.perform(post("/api/mark/saveGroupMark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(markService, times(1)).saveGroupMark(any(SaveGroupMarkRequestVO.class));
        }

        @Test
        @DisplayName("MC-006: Should return 500 when service throws exception")
        void saveGroupMark_serviceException() throws Exception {
            SaveGroupMarkRequestVO request = new SaveGroupMarkRequestVO();
            request.setProjectId(1L);
            request.setGroupId(10L);

            doThrow(new RuntimeException("Transaction failed"))
                    .when(markService).saveGroupMark(any(SaveGroupMarkRequestVO.class));

            mockMvc.perform(post("/api/mark/saveGroupMark")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());

            verify(markService, times(1)).saveGroupMark(any(SaveGroupMarkRequestVO.class));
        }
    }

    // ==================== getGroupMark Tests ====================

    @Nested
    @DisplayName("GET /api/mark/getGroupMark")
    class GetGroupMarkTests {

        @Test
        @DisplayName("MC-007: Should return group mark result successfully")
        void getGroupMark_success() throws Exception {
            GroupStudentMarkDTO student1 = new GroupStudentMarkDTO();
            GroupStudentMarkDTO student2 = new GroupStudentMarkDTO();

            GroupMarkResponseVO response = GroupMarkResponseVO.builder()
                    .projectId(1L)
                    .groupId(10L)
                    .comment("Excellent teamwork")
                    .students(Arrays.asList(student1, student2))
                    .build();

            when(markService.getGroupMark(1L, 10L)).thenReturn(response);

            mockMvc.perform(get("/api/mark/getGroupMark")
                            .param("projectId", "1")
                            .param("groupId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.projectId").value(1))
                    .andExpect(jsonPath("$.data.groupId").value(10))
                    .andExpect(jsonPath("$.data.comment").value("Excellent teamwork"))
                    .andExpect(jsonPath("$.data.students").isArray())
                    .andExpect(jsonPath("$.data.students.length()").value(2));

            verify(markService, times(1)).getGroupMark(1L, 10L);
        }

        @Test
        @DisplayName("MC-008: Should return null data when group not yet marked")
        void getGroupMark_notYetMarked() throws Exception {
            when(markService.getGroupMark(1L, 10L)).thenReturn(null);

            mockMvc.perform(get("/api/mark/getGroupMark")
                            .param("projectId", "1")
                            .param("groupId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(markService, times(1)).getGroupMark(1L, 10L);
        }

        @Test
        @DisplayName("MC-009: Should return 500 when service throws exception")
        void getGroupMark_serviceException() throws Exception {
            when(markService.getGroupMark(1L, 10L))
                    .thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get("/api/mark/getGroupMark")
                            .param("projectId", "1")
                            .param("groupId", "10"))
                    .andExpect(status().isInternalServerError());

            verify(markService, times(1)).getGroupMark(1L, 10L);
        }

        @Test
        @DisplayName("MC-010: Should pass correct params to service")
        void getGroupMark_passesCorrectParams() throws Exception {
            GroupMarkResponseVO response = GroupMarkResponseVO.builder()
                    .projectId(5L)
                    .groupId(20L)
                    .comment("OK")
                    .students(Collections.emptyList())
                    .build();

            when(markService.getGroupMark(5L, 20L)).thenReturn(response);

            mockMvc.perform(get("/api/mark/getGroupMark")
                            .param("projectId", "5")
                            .param("groupId", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectId").value(5))
                    .andExpect(jsonPath("$.data.groupId").value(20));

            verify(markService, times(1)).getGroupMark(5L, 20L);
            verify(markService, never()).saveMark(any());
            verify(markService, never()).saveGroupMark(any());
        }
    }
}
