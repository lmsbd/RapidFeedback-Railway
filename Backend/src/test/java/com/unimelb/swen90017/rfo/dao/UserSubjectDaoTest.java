package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.vo.SubjectDetailVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAO layer tests for UserSubjectDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference (user_subject):
 *   - Admin(1) → subjects [1,2,3,4,5]
 *   - Marker1(2) → subjects [1,2]
 *   - Marker2(3) → subjects [3,4]
 *   - Marker3(4) → subjects [5]
 *   - Marker4(5) → subjects [1,5]
 *   - Marker5(6) → subjects [2,3]
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserSubjectDao Integration Tests")
class UserSubjectDaoTest {

    @Autowired
    private UserSubjectDao userSubjectDao;

    // ==================== selectSubjectIdsByUserId Tests ====================

    @Nested
    @DisplayName("selectSubjectIdsByUserId Tests")
    class SelectSubjectIdsByUserIdTests {

        @Test
        @DisplayName("USD-001: Marker1 has 2 subjects")
        void selectSubjectIdsByUserId_marker1() {
            List<Long> ids = userSubjectDao.selectSubjectIdsByUserId(2L);
            assertEquals(2, ids.size());
            assertTrue(ids.containsAll(Arrays.asList(1L, 2L)));
        }

        @Test
        @DisplayName("USD-002: Admin has all 5 subjects")
        void selectSubjectIdsByUserId_admin() {
            List<Long> ids = userSubjectDao.selectSubjectIdsByUserId(1L);
            assertEquals(5, ids.size());
        }

        @Test
        @DisplayName("USD-003: Non-existent user returns empty")
        void selectSubjectIdsByUserId_nonExistent() {
            List<Long> ids = userSubjectDao.selectSubjectIdsByUserId(999L);
            assertTrue(ids.isEmpty());
        }
    }

    // ==================== selectSubjectDetailsByUserId Tests ====================

    @Nested
    @DisplayName("selectSubjectDetailsByUserId Tests")
    class SelectSubjectDetailsByUserIdTests {

        @Test
        @DisplayName("USD-004: Returns full subject details for Marker1")
        void selectSubjectDetailsByUserId_marker1() {
            List<SubjectDetailVO> details = userSubjectDao.selectSubjectDetailsByUserId(2L);
            assertEquals(2, details.size());
            // ORDER BY s.id → Mathematics(1), Physics(2)
            assertEquals("Mathematics", details.get(0).getName());
            assertEquals("Physics", details.get(1).getName());
            assertNotNull(details.get(0).getDescription());
        }

        @Test
        @DisplayName("USD-005: Non-existent user returns empty")
        void selectSubjectDetailsByUserId_nonExistent() {
            List<SubjectDetailVO> details = userSubjectDao.selectSubjectDetailsByUserId(999L);
            assertTrue(details.isEmpty());
        }
    }

    // ==================== selectUserIdsBySubjectId Tests ====================

    @Nested
    @DisplayName("selectUserIdsBySubjectId Tests")
    class SelectUserIdsBySubjectIdTests {

        @Test
        @DisplayName("USD-006: Subject 1 (Math) has 3 users: admin(1), marker1(2), marker4(5)")
        void selectUserIdsBySubjectId_math() {
            List<Long> ids = userSubjectDao.selectUserIdsBySubjectId(1L);
            assertEquals(3, ids.size());
            assertTrue(ids.containsAll(Arrays.asList(1L, 2L, 5L)));
        }

        @Test
        @DisplayName("USD-007: Non-existent subject returns empty")
        void selectUserIdsBySubjectId_nonExistent() {
            List<Long> ids = userSubjectDao.selectUserIdsBySubjectId(999L);
            assertTrue(ids.isEmpty());
        }
    }

    // ==================== insertOne / deleteOne Tests ====================

    @Nested
    @DisplayName("insertOne / deleteOne Tests")
    class InsertDeleteTests {

        @Test
        @DisplayName("USD-008: Insert new user-subject association")
        void insertOne_success() {
            // Marker3(4) currently only has subject 5
            int result = userSubjectDao.insertOne(4L, 1L);
            assertEquals(1, result);

            // Verify
            List<Long> ids = userSubjectDao.selectSubjectIdsByUserId(4L);
            assertTrue(ids.contains(1L));
        }

        @Test
        @DisplayName("USD-009: Delete existing user-subject association")
        void deleteOne_success() {
            // Marker1(2) has subjects [1,2]
            int result = userSubjectDao.deleteOne(2L, 1L);
            assertEquals(1, result);

            // Verify - should only have subject 2 now
            List<Long> ids = userSubjectDao.selectSubjectIdsByUserId(2L);
            assertEquals(1, ids.size());
            assertFalse(ids.contains(1L));
        }

        @Test
        @DisplayName("USD-010: Delete non-existent association returns 0")
        void deleteOne_nonExistent() {
            int result = userSubjectDao.deleteOne(999L, 999L);
            assertEquals(0, result);
        }
    }

    // ==================== insertUserFromSubject (batch) Tests ====================

    @Nested
    @DisplayName("insertUserFromSubject Tests")
    class InsertUserFromSubjectTests {

        @Test
        @DisplayName("USD-011: Batch insert multiple users for a subject")
        void insertUserFromSubject_batchInsert() {
            // Create a new subject (id=6) scenario: assign markers 2,3,4 to subject 4
            // Marker2(3) already has subject 4, so insert markers 2 and 4
            int count = userSubjectDao.insertUserFromSubject(4L, Arrays.asList(2L, 4L));
            assertEquals(2, count);

            // Verify
            List<Long> ids = userSubjectDao.selectUserIdsBySubjectId(4L);
            assertTrue(ids.contains(2L));
            assertTrue(ids.contains(4L));
        }
    }
}
