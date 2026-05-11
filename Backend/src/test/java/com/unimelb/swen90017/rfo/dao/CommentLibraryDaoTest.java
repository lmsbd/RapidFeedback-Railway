package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.CommentLibraryPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DAO tests for CommentLibraryDao.
 *
 * Key fixtures from test-data.sql:
 * - Template element 1 has five comments: two positive, one neutral, two negative.
 * - Assessment criteria id 1 points to template element 1.
 * - delete_status is tested per transaction so seed data stays stable.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CommentLibraryDao Tests")
class CommentLibraryDaoTest {

    @Autowired
    private CommentLibraryDao commentLibraryDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static List<String> contents(List<CommentLibraryPO> comments) {
        return comments.stream().map(CommentLibraryPO::getContent).toList();
    }

    private static List<Integer> types(List<CommentLibraryPO> comments) {
        return comments.stream().map(CommentLibraryPO::getCommentType).toList();
    }

    @Nested
    @DisplayName("Template element comment queries")
    class TemplateElementQueries {

        @Test
        @DisplayName("CLD-001: findByTemplateElementId returns active comments for an element")
        void findByTemplateElementId_returnsActiveComments() {
            List<CommentLibraryPO> comments = commentLibraryDao.findByTemplateElementId(1L);

            assertEquals(5, comments.size());
            assertEquals(List.of(
                    "Excellent vocal clarity and confident delivery throughout.",
                    "Good pacing, allowing audience to absorb information.",
                    "Speaking too quickly, hard to follow at times.",
                    "Low voice volume, difficult to hear in the back.",
                    "Consider varying your pace for emphasis."
            ), contents(comments));
        }

        @Test
        @DisplayName("CLD-002: findByTemplateElementId returns empty list for missing element")
        void findByTemplateElementId_missing_returnsEmptyList() {
            assertEquals(List.of(), commentLibraryDao.findByTemplateElementId(999L));
        }

        @Test
        @DisplayName("CLD-003: findByTemplateElementId excludes soft-deleted comments")
        void findByTemplateElementId_excludesSoftDeletedRows() {
            jdbcTemplate.update(
                    "UPDATE comment_library SET delete_status = 1 WHERE template_element_id = ? AND content = ?",
                    1L,
                    "Good pacing, allowing audience to absorb information."
            );

            List<CommentLibraryPO> comments = commentLibraryDao.findByTemplateElementId(1L);

            assertEquals(4, comments.size());
            assertEquals(List.of(
                    "Excellent vocal clarity and confident delivery throughout.",
                    "Speaking too quickly, hard to follow at times.",
                    "Low voice volume, difficult to hear in the back.",
                    "Consider varying your pace for emphasis."
            ), contents(comments));
        }
    }

    @Nested
    @DisplayName("Criteria comment queries")
    class CriteriaQueries {

        @Test
        @DisplayName("CLD-004: findByCriteriaId joins through assessment criteria and orders by comment type")
        void findByCriteriaId_joinsCriteriaAndOrdersByType() {
            List<CommentLibraryPO> comments = commentLibraryDao.findByCriteriaId(1L);

            assertEquals(5, comments.size());
            assertEquals(List.of(2, 2, 1, 0, 0), types(comments));
            assertEquals(List.of(
                    "Excellent vocal clarity and confident delivery throughout.",
                    "Good pacing, allowing audience to absorb information.",
                    "Consider varying your pace for emphasis.",
                    "Speaking too quickly, hard to follow at times.",
                    "Low voice volume, difficult to hear in the back."
            ), contents(comments));
        }

        @Test
        @DisplayName("CLD-005: findByCriteriaId returns empty list for missing criteria")
        void findByCriteriaId_missing_returnsEmptyList() {
            assertEquals(List.of(), commentLibraryDao.findByCriteriaId(999L));
        }

        @Test
        @DisplayName("CLD-006: findByCriteriaId excludes soft-deleted joined comments")
        void findByCriteriaId_excludesSoftDeletedRows() {
            jdbcTemplate.update(
                    "UPDATE comment_library SET delete_status = 1 WHERE template_element_id = ? AND comment_type = ?",
                    1L,
                    2
            );

            List<CommentLibraryPO> comments = commentLibraryDao.findByCriteriaId(1L);

            assertEquals(3, comments.size());
            assertEquals(List.of(1, 0, 0), types(comments));
            assertEquals(List.of(
                    "Consider varying your pace for emphasis.",
                    "Speaking too quickly, hard to follow at times.",
                    "Low voice volume, difficult to hear in the back."
            ), contents(comments));
        }
    }

    @Nested
    @DisplayName("Persistence behavior")
    class PersistenceBehavior {

        @Test
        @DisplayName("CLD-007: insert creates a retrievable active comment")
        void insert_createsComment() {
            CommentLibraryPO comment = CommentLibraryPO.builder()
                    .templateElementId(1L)
                    .content("New reusable comment")
                    .commentType(1)
                    .deleteStatus(0)
                    .build();

            assertEquals(1, commentLibraryDao.insert(comment));

            CommentLibraryPO saved = commentLibraryDao.selectById(comment.getId());
            assertNotNull(saved);
            assertEquals(1L, saved.getTemplateElementId());
            assertEquals("New reusable comment", saved.getContent());
            assertEquals(1, saved.getCommentType());
            assertEquals(0, saved.getDeleteStatus());
        }

        @Test
        @DisplayName("CLD-008: updateById changes content and comment type")
        void updateById_updatesComment() {
            CommentLibraryPO comment = commentLibraryDao.selectById(1L);
            assertNotNull(comment);

            comment.setContent("Updated clarity praise");
            comment.setCommentType(1);
            assertEquals(1, commentLibraryDao.updateById(comment));

            CommentLibraryPO updated = commentLibraryDao.selectById(1L);
            assertNotNull(updated);
            assertEquals("Updated clarity praise", updated.getContent());
            assertEquals(1, updated.getCommentType());
        }

        @Test
        @DisplayName("CLD-009: updateById can soft-delete a comment")
        void updateById_softDeletesComment() {
            CommentLibraryPO comment = commentLibraryDao.selectById(1L);
            assertNotNull(comment);
            comment.setDeleteStatus(1);

            assertEquals(1, commentLibraryDao.updateById(comment));

            assertNotNull(commentLibraryDao.selectById(1L));
            List<CommentLibraryPO> activeComments = commentLibraryDao.findByTemplateElementId(1L);
            assertEquals(4, activeComments.size());
        }

        @Test
        @DisplayName("CLD-010: deleteById physically removes a comment row")
        void deleteById_removesComment() {
            assertEquals(1, commentLibraryDao.deleteById(1L));

            assertNull(commentLibraryDao.selectById(1L));
            assertEquals(4, commentLibraryDao.findByTemplateElementId(1L).size());
        }
    }
}
