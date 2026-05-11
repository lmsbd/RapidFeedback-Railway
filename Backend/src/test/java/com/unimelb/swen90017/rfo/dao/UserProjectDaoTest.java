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
 * DAO layer tests for UserProjectDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference (user_project):
 *   - Admin(1): all 10 projects
 *   - Marker1(2): subjects 1,2
 *   - Marker3(4): subject 5
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserProjectDao Integration Tests")
class UserProjectDaoTest {

    @Autowired
    private UserProjectDao userProjectDao;

    @Nested
    @DisplayName("countBySubjectIdAndUserId Tests")
    class CountBySubjectIdAndUserIdTests {

        @Test
        @DisplayName("UPD-001: Admin has projects in all subjects")
        void countBySubjectIdAndUserId_admin() {
            int count = userProjectDao.countBySubjectIdAndUserId(1L, 1L);
            assertTrue(count > 0);
        }

        @Test
        @DisplayName("UPD-002: Marker1 has projects in Math")
        void countBySubjectIdAndUserId_marker1Math() {
            int count = userProjectDao.countBySubjectIdAndUserId(1L, 2L);
            assertTrue(count > 0);
        }

        @Test
        @DisplayName("UPD-003: Returns 0 for non-existent association")
        void countBySubjectIdAndUserId_nonExistent() {
            int count = userProjectDao.countBySubjectIdAndUserId(999L, 999L);
            assertEquals(0, count);
        }

        @Test
        @DisplayName("UPD-004: Marker3 has no projects in Math")
        void countBySubjectIdAndUserId_markerNotInSubject() {
            // Marker3(4) is only in CS (subject 5), not Math (1)
            int count = userProjectDao.countBySubjectIdAndUserId(1L, 4L);
            assertEquals(0, count);
        }
    }
}
