package com.unimelb.swen90017.rfo.service;

import com.unimelb.swen90017.rfo.dao.CommentLibraryDao;
import com.unimelb.swen90017.rfo.dao.TemplateElementDao;
import com.unimelb.swen90017.rfo.pojo.po.CommentLibraryPO;
import com.unimelb.swen90017.rfo.pojo.po.TemplateElementPO;
import com.unimelb.swen90017.rfo.pojo.vo.CommentVO;
import com.unimelb.swen90017.rfo.pojo.vo.TemplateElementVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.CommentListGetByCriteriaIdRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.CommentListGetRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.CommentSaveRequestVO;
import com.unimelb.swen90017.rfo.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommentServiceImpl.
 *
 * Covers test cases CS-001 through CS-008 as defined in Test-Plan.md.
 * Uses Mockito to isolate from database layer.
 *
 * Note: getCommentListByCriteriaId() is deferred to Phase 6
 * (business logic still evolving).
 *
 * @see com.unimelb.swen90017.rfo.service.impl.CommentServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceTest {

    @Mock
    private TemplateElementDao templateElementDao;

    @Mock
    private CommentLibraryDao commentLibraryDao;

    @InjectMocks
    private CommentServiceImpl commentService;

    // ==================== Helper Methods ====================

    private TemplateElementPO createTemplateElement(Long id, String name, Integer weighting, Integer maxMark, Double increments) {
        return TemplateElementPO.builder()
                .id(id)
                .name(name)
                .weighting(weighting)
                .maximumMark(maxMark)
                .markIncrements(increments)
                .build();
    }

    private CommentLibraryPO createComment(Long id, Long templateElementId, String content, Integer commentType) {
        return CommentLibraryPO.builder()
                .id(id)
                .templateElementId(templateElementId)
                .content(content)
                .commentType(commentType)
                .deleteStatus(0)
                .build();
    }

    // ==================== GetTemplateElementList Tests ====================

    @Nested
    @DisplayName("GetTemplateElementList Tests")
    class GetTemplateElementListTests {

        /**
         * CS-001: Get template element list returns all template elements.
         */
        @Test
        @DisplayName("CS-001: Get all template elements")
        void getTemplateElementList_returnsAllElements() throws Exception {
            // Arrange
            TemplateElementPO e1 = createTemplateElement(1L, "Clarity", 30, 10, 0.5);
            TemplateElementPO e2 = createTemplateElement(2L, "Accuracy", 40, 10, 1.0);
            TemplateElementPO e3 = createTemplateElement(3L, "Completeness", 30, 5, 0.5);
            when(templateElementDao.selectList(null)).thenReturn(Arrays.asList(e1, e2, e3));

            // Act
            List<TemplateElementVO> result = commentService.getTemplateElementList();

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());

            // Verify PO → VO mapping
            assertEquals(1L, result.get(0).getId());
            assertEquals("Clarity", result.get(0).getName());
            assertEquals(30, result.get(0).getWeighting());
            assertEquals(10, result.get(0).getMaximumMark());
            assertEquals(0.5, result.get(0).getMarkIncrements());

            assertEquals("Accuracy", result.get(1).getName());
            assertEquals(40, result.get(1).getWeighting());

            verify(templateElementDao).selectList(null);
        }

        /**
         * CS-001 supplement: Empty template element list.
         */
        @Test
        @DisplayName("CS-001 Supplement: Empty template list returns empty list")
        void getTemplateElementList_empty_returnsEmptyList() throws Exception {
            // Arrange
            when(templateElementDao.selectList(null)).thenReturn(Collections.emptyList());

            // Act
            List<TemplateElementVO> result = commentService.getTemplateElementList();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        /**
         * CS-001 supplement: DAO exception wraps as Exception.
         */
        @Test
        @DisplayName("CS-001 Supplement: DAO exception wraps as Exception")
        void getTemplateElementList_daoException_throwsWrappedException() {
            // Arrange
            when(templateElementDao.selectList(null))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.getTemplateElementList());
            assertTrue(exception.getMessage().contains("Failed to getTemplateInfo"));
        }
    }

    // ==================== GetCommentList Tests ====================

    @Nested
    @DisplayName("GetCommentList Tests")
    class GetCommentListTests {

        /**
         * CS-002: Get comment list for specific template element.
         */
        @Test
        @DisplayName("CS-002: Get comment list by templateElementId")
        void getCommentList_withValidElementId_returnsComments() throws Exception {
            // Arrange
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(1L);

            CommentLibraryPO c1 = createComment(10L, 1L, "Good work on clarity", 2);
            CommentLibraryPO c2 = createComment(11L, 1L, "Needs improvement", 0);
            when(commentLibraryDao.findByTemplateElementId(1L)).thenReturn(Arrays.asList(c1, c2));

            // Act
            List<CommentVO> result = commentService.getCommentList(request);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());

            // Verify PO → VO mapping
            assertEquals(10L, result.get(0).getId());
            assertEquals("Good work on clarity", result.get(0).getContent());
            assertEquals(2, result.get(0).getCommentType());

            assertEquals(11L, result.get(1).getId());
            assertEquals("Needs improvement", result.get(1).getContent());
            assertEquals(0, result.get(1).getCommentType());

            verify(commentLibraryDao).findByTemplateElementId(1L);
        }

        /**
         * CS-002 supplement: Element with no comments returns empty list.
         */
        @Test
        @DisplayName("CS-002 Supplement: No comments returns empty list")
        void getCommentList_noComments_returnsEmptyList() throws Exception {
            // Arrange
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(999L);
            when(commentLibraryDao.findByTemplateElementId(999L)).thenReturn(Collections.emptyList());

            // Act
            List<CommentVO> result = commentService.getCommentList(request);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        /**
         * CS-002 supplement: Null templateElementId throws Exception.
         */
        @Test
        @DisplayName("CS-002 Supplement: Null templateElementId throws Exception")
        void getCommentList_nullElementId_throwsException() {
            // Arrange
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(null);

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.getCommentList(request));
            assertEquals("TemplateElementId is null", exception.getMessage());

            verify(commentLibraryDao, never()).findByTemplateElementId(anyLong());
        }

        /**
         * CS-002 supplement: Null request throws Exception.
         */
        @Test
        @DisplayName("CS-002 Supplement: Null request throws Exception")
        void getCommentList_nullRequest_throwsException() {
            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.getCommentList(null));
            assertEquals("TemplateElementId is null", exception.getMessage());
        }

        /**
         * CS-004: Comment type verification.
         * Verifies commentType mapping: 0=negative, 1=neutral, 2=positive.
         */
        @Test
        @DisplayName("CS-004: Comment type values correctly mapped (0=neg, 1=neutral, 2=pos)")
        void getCommentList_commentTypes_correctlyMapped() throws Exception {
            // Arrange
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(1L);

            CommentLibraryPO negative = createComment(1L, 1L, "Poor quality", 0);
            CommentLibraryPO neutral = createComment(2L, 1L, "Acceptable", 1);
            CommentLibraryPO positive = createComment(3L, 1L, "Excellent!", 2);
            when(commentLibraryDao.findByTemplateElementId(1L))
                    .thenReturn(Arrays.asList(negative, neutral, positive));

            // Act
            List<CommentVO> result = commentService.getCommentList(request);

            // Assert
            assertEquals(3, result.size());
            assertEquals(0, result.get(0).getCommentType()); // negative
            assertEquals(1, result.get(1).getCommentType()); // neutral
            assertEquals(2, result.get(2).getCommentType()); // positive
        }

        /**
         * CS-002 supplement: DAO exception wraps as Exception.
         */
        @Test
        @DisplayName("CS-002 Supplement: DAO error wraps as Exception")
        void getCommentList_daoException_throwsWrappedException() {
            // Arrange
            CommentListGetRequestVO request = new CommentListGetRequestVO();
            request.setTemplateElementId(1L);
            when(commentLibraryDao.findByTemplateElementId(1L))
                    .thenThrow(new RuntimeException("DB error"));

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.getCommentList(request));
            assertTrue(exception.getMessage().contains("Failed to get comment list"));
        }
    }

    // ==================== SaveComment Tests ====================

    @Nested
    @DisplayName("SaveComment Tests")
    class SaveCommentTests {

        /**
         * CS-005: Successfully save (create) a new comment.
         * When id is null, insert is called.
         */
        @Test
        @DisplayName("CS-005: Create new comment (id=null triggers insert)")
        void saveComment_newComment_insertsSuccessfully() throws Exception {
            // Arrange
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(null); // new comment
            request.setTemplateElementId(1L);
            request.setContent("Great presentation skills");
            request.setCommentType(2);

            when(commentLibraryDao.insert(any(CommentLibraryPO.class))).thenReturn(1);

            // Act
            commentService.saveComment(request);

            // Assert
            ArgumentCaptor<CommentLibraryPO> captor = ArgumentCaptor.forClass(CommentLibraryPO.class);
            verify(commentLibraryDao).insert(captor.capture());

            CommentLibraryPO saved = captor.getValue();
            assertNull(saved.getId()); // new comment has null id
            assertEquals(1L, saved.getTemplateElementId());
            assertEquals("Great presentation skills", saved.getContent());
            assertEquals(2, saved.getCommentType());

            // No update should be called
            verify(commentLibraryDao, never()).updateById(any());
        }

        /**
         * CS-005 supplement: Update existing comment (id is present).
         */
        @Test
        @DisplayName("CS-005 Supplement: Update existing comment (id!=null triggers updateById)")
        void saveComment_existingComment_updatesSuccessfully() throws Exception {
            // Arrange
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(10L); // existing comment
            request.setTemplateElementId(1L);
            request.setContent("Updated feedback text");
            request.setCommentType(1);

            when(commentLibraryDao.updateById(any(CommentLibraryPO.class))).thenReturn(1);

            // Act
            commentService.saveComment(request);

            // Assert
            ArgumentCaptor<CommentLibraryPO> captor = ArgumentCaptor.forClass(CommentLibraryPO.class);
            verify(commentLibraryDao).updateById(captor.capture());

            CommentLibraryPO updated = captor.getValue();
            assertEquals(10L, updated.getId());
            assertEquals("Updated feedback text", updated.getContent());

            // No insert should be called
            verify(commentLibraryDao, never()).insert(any());
        }

        /**
         * CS-005 supplement: Null request throws Exception.
         */
        @Test
        @DisplayName("CS-005 Supplement: Null request throws Exception")
        void saveComment_nullRequest_throwsException() {
            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.saveComment(null));
            assertEquals("Can't find any Save Parameters!", exception.getMessage());
        }

        /**
         * CS-005 supplement: Insert failure throws Exception.
         */
        @Test
        @DisplayName("CS-005 Supplement: Insert failure throws Exception")
        void saveComment_insertFailure_throwsException() {
            // Arrange
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(null);
            request.setTemplateElementId(1L);
            request.setContent("Test");
            request.setCommentType(1);
            when(commentLibraryDao.insert(any(CommentLibraryPO.class))).thenReturn(0);

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.saveComment(request));
            assertTrue(exception.getMessage().contains("Failed to save Comment"));
        }

        /**
         * CS-005 supplement: Update failure throws Exception.
         */
        @Test
        @DisplayName("CS-005 Supplement: Update failure throws Exception")
        void saveComment_updateFailure_throwsException() {
            // Arrange
            CommentSaveRequestVO request = new CommentSaveRequestVO();
            request.setId(10L);
            request.setTemplateElementId(1L);
            request.setContent("Test");
            request.setCommentType(1);
            when(commentLibraryDao.updateById(any(CommentLibraryPO.class))).thenReturn(0);

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.saveComment(request));
            assertTrue(exception.getMessage().contains("Failed to update Comment"));
        }
    }

    // ==================== DeleteComment Tests ====================

    @Nested
    @DisplayName("DeleteComment Tests")
    class DeleteCommentTests {

        /**
         * CS-007: Successfully soft delete a comment.
         * Verifies deleteStatus is set to 1 and updateById is called.
         */
        @Test
        @DisplayName("CS-007: Soft delete sets deleteStatus to 1")
        void deleteComment_validId_setsDeleteStatusTo1() throws Exception {
            // Arrange
            Long commentId = 10L;
            CommentLibraryPO existingComment = createComment(10L, 1L, "Old comment", 2);
            existingComment.setDeleteStatus(0);

            when(commentLibraryDao.selectById(commentId)).thenReturn(existingComment);
            when(commentLibraryDao.updateById(any(CommentLibraryPO.class))).thenReturn(1);

            // Act
            commentService.deleteComment(commentId);

            // Assert
            ArgumentCaptor<CommentLibraryPO> captor = ArgumentCaptor.forClass(CommentLibraryPO.class);
            verify(commentLibraryDao).updateById(captor.capture());

            CommentLibraryPO updated = captor.getValue();
            assertEquals(1, updated.getDeleteStatus()); // soft deleted
            assertEquals(10L, updated.getId());
        }

        /**
         * CS-008: Delete non-existent comment throws Exception.
         */
        @Test
        @DisplayName("CS-008: Non-existent commentId throws Exception")
        void deleteComment_nonExistent_throwsException() {
            // Arrange
            when(commentLibraryDao.selectById(999L)).thenReturn(null);

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.deleteComment(999L));
            assertTrue(exception.getMessage().contains("Comment not found"));
        }

        /**
         * CS-008 supplement: Null commentId throws Exception.
         */
        @Test
        @DisplayName("CS-008 Supplement: Null commentId throws Exception")
        void deleteComment_nullId_throwsException() {
            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.deleteComment(null));
            assertEquals("Comment ID is null", exception.getMessage());

            verify(commentLibraryDao, never()).selectById(anyLong());
        }

        /**
         * CS-007 supplement: Update failure during delete throws Exception.
         */
        @Test
        @DisplayName("CS-007 Supplement: Update failure during delete throws Exception")
        void deleteComment_updateFailure_throwsException() {
            // Arrange
            CommentLibraryPO existingComment = createComment(10L, 1L, "Comment", 1);
            when(commentLibraryDao.selectById(10L)).thenReturn(existingComment);
            when(commentLibraryDao.updateById(any(CommentLibraryPO.class))).thenReturn(0);

            // Act & Assert
            Exception exception = assertThrows(Exception.class,
                    () -> commentService.deleteComment(10L));
            assertTrue(exception.getMessage().contains("Failed to delete comment"));
        }
    }
}
