package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.dto.StudentDTO;
import com.unimelb.swen90017.rfo.pojo.vo.UserResponseVO;
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
 * DAO layer tests for SubjectDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference:
 *   - Subject 1 = Mathematics (students: Alice=1..Henry=8 via student_subject)
 *   - Subject 5 = Computer Science (students: Frank=6, Grace=7, Henry=8, Iris=9, Jack=10)
 *   - Marker-Subject: Marker1(2)→[1,2], Marker4(5)→[1,5], Admin(1)→all
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SubjectDao Integration Tests")
class SubjectDaoTest {

    @Autowired
    private SubjectDao subjectDao;

    // ==================== getStudentsBySubjectId Tests ====================

    @Nested
    @DisplayName("getStudentsBySubjectId Tests")
    class GetStudentsBySubjectIdTests {

        @Test
        @DisplayName("SD-001: Returns students for Mathematics (subject 1)")
        void getStudentsBySubjectId_mathReturns8Students() {
            List<StudentDTO> students = subjectDao.getStudentsBySubjectId(1L);
            // student_subject for subject 1: students 1,2,3,4,5,6,7,8
            assertEquals(8, students.size());
        }

        @Test
        @DisplayName("SD-002: Returns correct student fields (DTO mapping)")
        void getStudentsBySubjectId_correctFieldMapping() {
            List<StudentDTO> students = subjectDao.getStudentsBySubjectId(1L);
            // Alice (id=1, student_id=1510000) should be first (ORDER BY s.student_id)
            StudentDTO alice = students.get(0);
            assertEquals(1L, alice.getId());
            assertEquals(1510000L, alice.getStudentId());
            assertEquals("Alice", alice.getFirstName());
            assertEquals("Chen", alice.getSurname());
            assertNotNull(alice.getEmail());
        }

        @Test
        @DisplayName("SD-003: Returns empty for non-existent subject")
        void getStudentsBySubjectId_nonExistentSubject() {
            List<StudentDTO> students = subjectDao.getStudentsBySubjectId(999L);
            assertTrue(students.isEmpty());
        }
    }

    // ==================== getMarkersBySubjectId Tests ====================

    @Nested
    @DisplayName("getMarkersBySubjectId Tests")
    class GetMarkersBySubjectIdTests {

        @Test
        @DisplayName("SD-004: Returns markers for Mathematics (subject 1)")
        void getMarkersBySubjectId_mathReturns2Markers() {
            // user_subject for subject 1 with role=2: marker1(2), marker4(5)
            // Admin(1) has role=1 → filtered out
            List<UserResponseVO> markers = subjectDao.getMarkersBySubjectId(1L);
            assertEquals(2, markers.size());
            assertTrue(markers.stream().anyMatch(m -> m.getUserName().equals("marker1")));
            assertTrue(markers.stream().anyMatch(m -> m.getUserName().equals("marker4")));
        }

        @Test
        @DisplayName("SD-005: Admin (role=1) is not included")
        void getMarkersBySubjectId_excludesAdmin() {
            List<UserResponseVO> markers = subjectDao.getMarkersBySubjectId(1L);
            assertTrue(markers.stream().noneMatch(m -> m.getUserId() == 1L));
        }

        @Test
        @DisplayName("SD-006: Returns empty for subject with no markers")
        void getMarkersBySubjectId_nonExistentSubject() {
            List<UserResponseVO> markers = subjectDao.getMarkersBySubjectId(999L);
            assertTrue(markers.isEmpty());
        }
    }

    // ==================== getStudentIdsBySubjectId Tests ====================

    @Nested
    @DisplayName("getStudentIdsBySubjectId Tests")
    class GetStudentIdsBySubjectIdTests {

        @Test
        @DisplayName("SD-007: Returns student IDs for subject 5 (CS)")
        void getStudentIdsBySubjectId_csReturns5() {
            // student_subject for subject 5: students 6,7,8,9,10
            List<Long> ids = subjectDao.getStudentIdsBySubjectId(5L);
            assertEquals(5, ids.size());
        }

        @Test
        @DisplayName("SD-008: Returns empty for non-existent subject")
        void getStudentIdsBySubjectId_nonExistent() {
            List<Long> ids = subjectDao.getStudentIdsBySubjectId(999L);
            assertTrue(ids.isEmpty());
        }
    }
}
