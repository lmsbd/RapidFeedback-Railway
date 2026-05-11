package com.unimelb.swen90017.rfo.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAO layer tests for StudentProjectDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference (student_project):
 *   - student 1 (Alice) in subject 1 (Math), project 1 (Calculus)
 *   - student 9 (Iris) in subject 5 (CS), project 9 (SE)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("StudentProjectDao Integration Tests")
class StudentProjectDaoTest {

    @Autowired
    private StudentProjectDao studentProjectDao;

    @Nested
    @DisplayName("countBySubjectIdAndStudentId Tests")
    class CountBySubjectIdAndStudentIdTests {

        @Test
        @DisplayName("SPD-001: Returns count > 0 for existing association")
        void countBySubjectIdAndStudentId_existing() {
            // Alice (1) has projects in Math (subject 1)
            int count = studentProjectDao.countBySubjectIdAndStudentId(1L, 1L);
            assertTrue(count > 0);
        }

        @Test
        @DisplayName("SPD-002: Returns 0 for non-existent association")
        void countBySubjectIdAndStudentId_nonExistent() {
            int count = studentProjectDao.countBySubjectIdAndStudentId(999L, 999L);
            assertEquals(0, count);
        }

        @Test
        @DisplayName("SPD-003: Returns 0 when student is not in subject's project")
        void countBySubjectIdAndStudentId_studentNotInSubject() {
            // Iris (9) is in CS (5) but not in Math (1)
            int count = studentProjectDao.countBySubjectIdAndStudentId(1L, 9L);
            assertEquals(0, count);
        }
    }
}
