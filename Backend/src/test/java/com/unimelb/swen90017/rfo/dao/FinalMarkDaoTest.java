package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.FinalMarkPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DAO tests for FinalMarkDao.
 *
 * Key fixtures from test-data.sql:
 * - final_mark id=1: project 9, student 9, final_score 80.00, unlocked
 * - final_mark id=2: project 9, group 16, final_score 78.50, locked
 * - final_mark id=3: project 9, student 9 + group 16, final_score 79.25
 * - marker_student: project 9/student 9 has markers 4 and 5; student 10 has marker 4
 * - marker_group: project 9/group 16 has markers 4 and 5; group 17 has marker 6
 * - group 16 has active students 9 and 6; marker 4 has group_score for both, marker 5 only for student 9
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("FinalMarkDao Tests")
class FinalMarkDaoTest {

    @Autowired
    private FinalMarkDao finalMarkDao;

    @Nested
    @DisplayName("Final mark lookup queries")
    class LookupQueries {

        @Test
        @DisplayName("FMD-001: getByProjectAndStudent returns the individual final mark")
        void getByProjectAndStudent_existing_returnsFinalMark() {
            FinalMarkPO result = finalMarkDao.getByProjectAndStudent(9L, 9L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(9L, result.getProjectId());
            assertEquals(9L, result.getStudentId());
            assertNull(result.getGroupId());
            assertEquals(0, new BigDecimal("80.00").compareTo(result.getFinalScore()));
            assertFalse(result.getIsLocked());
        }

        @Test
        @DisplayName("FMD-002: getByProjectAndStudent returns null for missing student final mark")
        void getByProjectAndStudent_missing_returnsNull() {
            FinalMarkPO result = finalMarkDao.getByProjectAndStudent(9L, 10L);

            assertNull(result);
        }

        @Test
        @DisplayName("FMD-003: getByProjectAndGroup returns the group final mark")
        void getByProjectAndGroup_existing_returnsFinalMark() {
            FinalMarkPO result = finalMarkDao.getByProjectAndGroup(9L, 16L);

            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals(9L, result.getProjectId());
            assertNull(result.getStudentId());
            assertEquals(16L, result.getGroupId());
            assertEquals(0, new BigDecimal("78.50").compareTo(result.getFinalScore()));
            assertTrue(result.getIsLocked());
        }

        @Test
        @DisplayName("FMD-004: getByProjectAndGroup returns null for missing group final mark")
        void getByProjectAndGroup_missing_returnsNull() {
            FinalMarkPO result = finalMarkDao.getByProjectAndGroup(9L, 17L);

            assertNull(result);
        }

        @Test
        @DisplayName("FMD-005: getByProjectStudentAndGroup returns combined student/group final mark")
        void getByProjectStudentAndGroup_existing_returnsFinalMark() {
            FinalMarkPO result = finalMarkDao.getByProjectStudentAndGroup(9L, 9L, 16L);

            assertNotNull(result);
            assertEquals(3L, result.getId());
            assertEquals(9L, result.getStudentId());
            assertEquals(16L, result.getGroupId());
            assertEquals(0, new BigDecimal("79.25").compareTo(result.getFinalScore()));
            assertFalse(result.getIsLocked());
        }

        @Test
        @DisplayName("FMD-006: getByProjectStudentAndGroup requires all three keys")
        void getByProjectStudentAndGroup_missingCombination_returnsNull() {
            FinalMarkPO result = finalMarkDao.getByProjectStudentAndGroup(9L, 10L, 16L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Marker completion and assignment counts")
    class MarkerCounts {

        @Test
        @DisplayName("FMD-007: countCompletedMarkersForStudent counts non-null total_score rows")
        void countCompletedMarkersForStudent_countsSubmittedMarks() {
            assertEquals(2, finalMarkDao.countCompletedMarkersForStudent(9L, 9L));
            assertEquals(0, finalMarkDao.countCompletedMarkersForStudent(9L, 10L));
        }

        @Test
        @DisplayName("FMD-008: countAssignedMarkersForStudent counts marker_student assignments")
        void countAssignedMarkersForStudent_countsAssignments() {
            assertEquals(2, finalMarkDao.countAssignedMarkersForStudent(9L, 9L));
            assertEquals(1, finalMarkDao.countAssignedMarkersForStudent(9L, 10L));
            assertEquals(0, finalMarkDao.countAssignedMarkersForStudent(9L, 8L));
        }

        @Test
        @DisplayName("FMD-009: countCompletedMarkersForGroup requires every active group member to have a group_score")
        void countCompletedMarkersForGroup_countsOnlyFullyCompletedMarkers() {
            assertEquals(1, finalMarkDao.countCompletedMarkersForGroup(9L, 16L));
            assertEquals(0, finalMarkDao.countCompletedMarkersForGroup(9L, 17L));
        }

        @Test
        @DisplayName("FMD-010: countAssignedMarkersForGroup counts marker_group assignments")
        void countAssignedMarkersForGroup_countsAssignments() {
            assertEquals(2, finalMarkDao.countAssignedMarkersForGroup(9L, 16L));
            assertEquals(1, finalMarkDao.countAssignedMarkersForGroup(9L, 17L));
            assertEquals(0, finalMarkDao.countAssignedMarkersForGroup(9L, 18L));
        }
    }
}
