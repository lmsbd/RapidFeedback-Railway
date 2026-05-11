package com.unimelb.swen90017.rfo.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAO layer tests for StudentSubjectDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference (student_subject):
 *   Subject 1 (Math): students 1,2,3,4,5,6,7,8
 *   Subject 5 (CS):   students 6,7,8,9,10
 *
 * Note: INSERT IGNORE is MySQL-specific. Batch insert tests are skipped
 * since H2 doesn't support INSERT IGNORE syntax.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("StudentSubjectDao Integration Tests")
class StudentSubjectDaoTest {

    @Autowired
    private StudentSubjectDao studentSubjectDao;

    // ==================== selectStudentIdsBySubjectId Tests ====================

    @Nested
    @DisplayName("selectStudentIdsBySubjectId Tests")
    class SelectStudentIdsBySubjectIdTests {

        @Test
        @DisplayName("SSD-001: Returns 8 students for Math (subject 1)")
        void selectStudentIdsBySubjectId_math() {
            List<Long> ids = studentSubjectDao.selectStudentIdsBySubjectId(1L);
            assertEquals(8, ids.size());
        }

        @Test
        @DisplayName("SSD-002: Returns 5 students for CS (subject 5)")
        void selectStudentIdsBySubjectId_cs() {
            List<Long> ids = studentSubjectDao.selectStudentIdsBySubjectId(5L);
            assertEquals(5, ids.size());
        }

        @Test
        @DisplayName("SSD-003: Returns empty for non-existent subject")
        void selectStudentIdsBySubjectId_nonExistent() {
            List<Long> ids = studentSubjectDao.selectStudentIdsBySubjectId(999L);
            assertTrue(ids.isEmpty());
        }
    }

    // ==================== deleteOne Tests ====================

    @Nested
    @DisplayName("deleteOne Tests")
    class DeleteOneTests {

        @Test
        @DisplayName("SSD-004: Deletes existing association")
        void deleteOne_success() {
            // Student 1 is in subject 1
            int result = studentSubjectDao.deleteOne(1L, 1L);
            assertEquals(1, result);

            // Verify
            List<Long> ids = studentSubjectDao.selectStudentIdsBySubjectId(1L);
            assertFalse(ids.contains(1L));
            assertEquals(7, ids.size());
        }

        @Test
        @DisplayName("SSD-005: Returns 0 for non-existent association")
        void deleteOne_nonExistent() {
            int result = studentSubjectDao.deleteOne(999L, 999L);
            assertEquals(0, result);
        }
    }
}
