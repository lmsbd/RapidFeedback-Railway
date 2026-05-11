package com.unimelb.swen90017.rfo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimelb.swen90017.rfo.pojo.vo.CommentVO;
import com.unimelb.swen90017.rfo.pojo.vo.TemplateElementVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.CommentListGetByCriteriaIdRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.CommentListGetRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.CommentSaveRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetailsService;
import com.unimelb.swen90017.rfo.security.JwtAuthenticationFilter;
import com.unimelb.swen90017.rfo.service.CommentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CommentController.
 *
 * Covers all 5 endpoints (none require @AuthenticationPrincipal):
 *   - POST /api/comment/getCommentLibraryList
 *   - POST /api/comment/getCommentList
 *   - POST /api/comment/getCommentListByCriteriaId
 *   - POST /api/comment/saveComment
 *   - POST /api/comment/deleteComment
 *
 * All endpoints use catch → Result.error(e.getMessage()) pattern,
 * so errors return HTTP 200 with code=500 in the JSON body.
 *
 * @see com.unimelb.swen90017.rfo.controller.CommentController
 */
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommentController Unit Tests")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Primary service ====================
    @MockBean
    private CommentService commentService;

    // ==================== Security infrastructure ====================
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== getCommentLibraryList Tests ====================

    @Nested
    @DisplayName("POST /api/comment/getCommentLibraryList")
    class GetCommentLibraryListTests {

        @Test
        @DisplayName("CC-001: Should return template element list successfully")
        void getCommentLibraryList_success() throws Exception {
            TemplateElementVO elem1 = new TemplateElementVO();
            elem1.setId(1L);
            elem1.setName("Code Quality");
            elem1.setWeighting(30);
            elem1.setMaximumMark(10);

            TemplateElementVO elem2 = new TemplateElementVO();
            elem2.setId(2L);
            elem2.setName("Documentation");
            elem2.setWeighting(20);
            elem2.setMaximumMark(10);

            when(commentService.getTemplateElementList())
                    .thenReturn(Arrays.asList(elem1, elem2));

            mockMvc.perform(post("/api/comment/getCommentLibraryList"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("Code Quality"))
                    .andExpect(jsonPath("$.data[1].name").value("Documentation"));

            verify(commentService, times(1)).getTemplateElementList();
        }

        @Test
        @DisplayName("CC-002: Should return empty list when no elements")
        void getCommentLibraryList_empty() throws Exception {
            when(commentService.getTemplateElementList())
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/comment/getCommentLibraryList"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(commentService, times(1)).getTemplateElementList();
        }

        @Test
        @DisplayName("CC-003: Should return error when service throws exception")
        void getCommentLibraryList_serviceException() throws Exception {
            when(commentService.getTemplateElementList())
                    .thenThrow(new RuntimeException("DAO error"));

            mockMvc.perform(post("/api/comment/getCommentLibraryList"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("DAO error"));

            verify(commentService, times(1)).getTemplateElementList();
        }
    }

    // ==================== getCommentList Tests ====================

    @Nested
    @DisplayName("POST /api/comment/getCommentList")
    class GetCommentListTests {

        @Test
        @DisplayName("CC-004: Should return comments for given templateElementId")
        void getCommentList_success() throws Exception {
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(1L);

            CommentVO comment1 = new CommentVO();
            comment1.setId(10L);
            comment1.setContent("Good code structure");
            comment1.setCommentType(2); // positive

            CommentVO comment2 = new CommentVO();
            comment2.setId(11L);
            comment2.setContent("Missing edge case handling");
            comment2.setCommentType(0); // negative

            when(commentService.getCommentList(any(CommentListGetRequestVO.class)))
                    .thenReturn(Arrays.asList(comment1, comment2));

            mockMvc.perform(post("/api/comment/getCommentList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].content").value("Good code structure"))
                    .andExpect(jsonPath("$.data[0].commentType").value(2))
                    .andExpect(jsonPath("$.data[1].content").value("Missing edge case handling"))
                    .andExpect(jsonPath("$.data[1].commentType").value(0));

            verify(commentService, times(1)).getCommentList(any(CommentListGetRequestVO.class));
        }

        @Test
        @DisplayName("CC-005: Should return empty list when no comments")
        void getCommentList_empty() throws Exception {
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(1L);

            when(commentService.getCommentList(any(CommentListGetRequestVO.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/comment/getCommentList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(commentService, times(1)).getCommentList(any(CommentListGetRequestVO.class));
        }

        @Test
        @DisplayName("CC-006: Should return error when service throws exception")
        void getCommentList_serviceException() throws Exception {
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(1L);

            when(commentService.getCommentList(any(CommentListGetRequestVO.class)))
                    .thenThrow(new RuntimeException("Element not found"));

            mockMvc.perform(post("/api/comment/getCommentList")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Element not found"));

            verify(commentService, times(1)).getCommentList(any(CommentListGetRequestVO.class));
        }
    }

    // ==================== getCommentListByCriteriaId Tests ====================

    @Nested
    @DisplayName("POST /api/comment/getCommentListByCriteriaId")
    class GetCommentListByCriteriaIdTests {

        @Test
        @DisplayName("CC-007: Should return comments for given criteriaId")
        void getCommentListByCriteriaId_success() throws Exception {
            CommentListGetByCriteriaIdRequestVO request = new CommentListGetByCriteriaIdRequestVO();
            request.setCriteriaId(5L);

            CommentVO comment = new CommentVO();
            comment.setId(20L);
            comment.setContent("Average performance");
            comment.setCommentType(1); // neutral

            when(commentService.getCommentListByCriteriaId(any(CommentListGetByCriteriaIdRequestVO.class)))
                    .thenReturn(Collections.singletonList(comment));

            mockMvc.perform(post("/api/comment/getCommentListByCriteriaId")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].content").value("Average performance"))
                    .andExpect(jsonPath("$.data[0].commentType").value(1));

            verify(commentService, times(1))
                    .getCommentListByCriteriaId(any(CommentListGetByCriteriaIdRequestVO.class));
        }

        @Test
        @DisplayName("CC-008: Should return error when service throws exception")
        void getCommentListByCriteriaId_serviceException() throws Exception {
            CommentListGetByCriteriaIdRequestVO request = new CommentListGetByCriteriaIdRequestVO();
            request.setCriteriaId(5L);

            when(commentService.getCommentListByCriteriaId(any(CommentListGetByCriteriaIdRequestVO.class)))
                    .thenThrow(new RuntimeException("Criteria not found"));

            mockMvc.perform(post("/api/comment/getCommentListByCriteriaId")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Criteria not found"));

            verify(commentService, times(1))
                    .getCommentListByCriteriaId(any(CommentListGetByCriteriaIdRequestVO.class));
        }
    }

    // ==================== saveComment Tests ====================

    @Nested
    @DisplayName("POST /api/comment/saveComment")
    class SaveCommentTests {

        @Test
        @DisplayName("CC-009: Should save new comment successfully (id=null → insert)")
        void saveComment_create() throws Exception {
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(null);
            request.setTemplateElementId(1L);
            request.setContent("Excellent work on testing");
            request.setCommentType(2);

            doNothing().when(commentService).saveComment(any(CommentSaveRequestVO.class));

            mockMvc.perform(post("/api/comment/saveComment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(commentService, times(1)).saveComment(any(CommentSaveRequestVO.class));
        }

        @Test
        @DisplayName("CC-010: Should update existing comment successfully (id!=null → update)")
        void saveComment_update() throws Exception {
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(10L);
            request.setTemplateElementId(1L);
            request.setContent("Updated feedback");
            request.setCommentType(1);

            doNothing().when(commentService).saveComment(any(CommentSaveRequestVO.class));

            mockMvc.perform(post("/api/comment/saveComment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(commentService, times(1)).saveComment(any(CommentSaveRequestVO.class));
        }

        @Test
        @DisplayName("CC-011: Should return error when save fails")
        void saveComment_failure() throws Exception {
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(null);
            request.setTemplateElementId(1L);
            request.setContent("Test");
            request.setCommentType(0);

            doThrow(new RuntimeException("Insert failed"))
                    .when(commentService).saveComment(any(CommentSaveRequestVO.class));

            mockMvc.perform(post("/api/comment/saveComment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Insert failed"));

            verify(commentService, times(1)).saveComment(any(CommentSaveRequestVO.class));
        }
    }

    // ==================== deleteComment Tests ====================

    @Nested
    @DisplayName("POST /api/comment/deleteComment")
    class DeleteCommentTests {

        @Test
        @DisplayName("CC-012: Should delete comment successfully (soft delete)")
        void deleteComment_success() throws Exception {
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(10L);

            doNothing().when(commentService).deleteComment(eq(10L));

            mockMvc.perform(post("/api/comment/deleteComment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Operation successful"));

            verify(commentService, times(1)).deleteComment(eq(10L));
        }

        @Test
        @DisplayName("CC-013: Should return error when comment not found")
        void deleteComment_notFound() throws Exception {
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(999L);

            doThrow(new RuntimeException("Comment not found"))
                    .when(commentService).deleteComment(eq(999L));

            mockMvc.perform(post("/api/comment/deleteComment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.message").value("Comment not found"));

            verify(commentService, times(1)).deleteComment(eq(999L));
        }

        @Test
        @DisplayName("CC-014: Should pass correct id to service")
        void deleteComment_passesCorrectId() throws Exception {
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(42L);
            request.setContent("ignored content");
            request.setCommentType(2);

            doNothing().when(commentService).deleteComment(eq(42L));

            mockMvc.perform(post("/api/comment/deleteComment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // Verify only id is extracted, other fields ignored
            verify(commentService, times(1)).deleteComment(eq(42L));
            verify(commentService, never()).saveComment(any());
        }
    }
}
