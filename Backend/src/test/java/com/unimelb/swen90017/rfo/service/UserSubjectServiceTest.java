package com.unimelb.swen90017.rfo.service;

import com.unimelb.swen90017.rfo.dao.UserSubjectDao;
import com.unimelb.swen90017.rfo.pojo.vo.SubjectDetailVO;
import com.unimelb.swen90017.rfo.service.impl.UserSubjectServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserSubjectServiceImpl.
 *
 * Covers test cases USS-001 through USS-005 as defined in Test-Plan.md.
 * Uses Mockito to isolate from database layer.
 *
 * Note: The service is a thin delegation layer over UserSubjectDao.
 * Filtering logic (e.g. delete_status) is handled at the SQL/DAO level,
 * so we verify the service correctly passes through DAO results.
 *
 * @see com.unimelb.swen90017.rfo.service.impl.UserSubjectServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSubjectService Unit Tests")
class UserSubjectServiceTest {

    @Mock
    private UserSubjectDao userSubjectDao;

    @InjectMocks
    private UserSubjectServiceImpl userSubjectService;

    // ==================== GetSubjectIdsByUserId Tests ====================

    @Nested
    @DisplayName("GetSubjectIdsByUserId Tests")
    class GetSubjectIdsByUserIdTests {

        /**
         * USS-001: Get user's subject ID list.
         * Verifies that the service returns all subject IDs associated with the user.
         */
        @Test
        @DisplayName("USS-001: Get subject IDs for valid user returns ID list")
        void getSubjectIdsByUserId_withValidUser_returnsIdList() {
            // Arrange
            Long userId = 1L;
            List<Long> expectedIds = Arrays.asList(1L, 2L, 5L);
            when(userSubjectDao.selectSubjectIdsByUserId(userId)).thenReturn(expectedIds);

            // Act
            List<Long> result = userSubjectService.getSubjectIdsByUserId(userId);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(1L, result.get(0));
            assertEquals(2L, result.get(1));
            assertEquals(5L, result.get(2));

            verify(userSubjectDao).selectSubjectIdsByUserId(userId);
        }

        /**
         * USS-002: User has no associated subjects returns empty list.
         */
        @Test
        @DisplayName("USS-002: User with no subjects returns empty list")
        void getSubjectIdsByUserId_noSubjects_returnsEmptyList() {
            // Arrange
            Long userId = 99L;
            when(userSubjectDao.selectSubjectIdsByUserId(userId)).thenReturn(Collections.emptyList());

            // Act
            List<Long> result = userSubjectService.getSubjectIdsByUserId(userId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(userSubjectDao).selectSubjectIdsByUserId(userId);
        }

        /**
         * USS-005: Non-existent user returns empty list.
         */
        @Test
        @DisplayName("USS-005: Non-existent userId returns empty list")
        void getSubjectIdsByUserId_nonExistentUser_returnsEmptyList() {
            // Arrange
            Long invalidUserId = 999L;
            when(userSubjectDao.selectSubjectIdsByUserId(invalidUserId)).thenReturn(Collections.emptyList());

            // Act
            List<Long> result = userSubjectService.getSubjectIdsByUserId(invalidUserId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        /**
         * USS-001 supplement: Single subject association.
         */
        @Test
        @DisplayName("USS-001 Supplement: User with single subject returns singleton list")
        void getSubjectIdsByUserId_singleSubject_returnsSingletonList() {
            // Arrange
            Long userId = 4L;
            when(userSubjectDao.selectSubjectIdsByUserId(userId)).thenReturn(List.of(5L));

            // Act
            List<Long> result = userSubjectService.getSubjectIdsByUserId(userId);

            // Assert
            assertEquals(1, result.size());
            assertEquals(5L, result.get(0));
        }
    }

    // ==================== GetSubjectDetailsByUserId Tests ====================

    @Nested
    @DisplayName("GetSubjectDetailsByUserId Tests")
    class GetSubjectDetailsByUserIdTests {

        /**
         * USS-003: Get user's subject details returns list with name and description.
         */
        @Test
        @DisplayName("USS-003: Get subject details returns complete subject info")
        void getSubjectDetailsByUserId_withValidUser_returnsDetailsList() {
            // Arrange
            Long userId = 2L;
            SubjectDetailVO math = new SubjectDetailVO(1L, "Mathematics", "Advanced mathematics and calculus");
            SubjectDetailVO physics = new SubjectDetailVO(2L, "Physics", "Classical and modern physics");
            when(userSubjectDao.selectSubjectDetailsByUserId(userId)).thenReturn(Arrays.asList(math, physics));

            // Act
            List<SubjectDetailVO> result = userSubjectService.getSubjectDetailsByUserId(userId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());

            // First subject
            assertEquals(1L, result.get(0).getId());
            assertEquals("Mathematics", result.get(0).getName());
            assertEquals("Advanced mathematics and calculus", result.get(0).getDescription());

            // Second subject
            assertEquals(2L, result.get(1).getId());
            assertEquals("Physics", result.get(1).getName());
            assertEquals("Classical and modern physics", result.get(1).getDescription());

            verify(userSubjectDao).selectSubjectDetailsByUserId(userId);
        }

        /**
         * USS-004: Deleted subjects are filtered by DAO query (WHERE delete_status = 0 in SQL JOIN).
         * Service correctly passes through DAO's filtered result.
         */
        @Test
        @DisplayName("USS-004: Deleted subjects filtered at DAO level")
        void getSubjectDetailsByUserId_deletedSubjectsFilteredByDao() {
            // Arrange - DAO query joins with subject table and only returns active subjects
            Long userId = 1L;
            SubjectDetailVO activeSubject = new SubjectDetailVO(1L, "Mathematics", "Active subject");
            // Deleted subjects are NOT returned by the DAO SQL query
            when(userSubjectDao.selectSubjectDetailsByUserId(userId))
                    .thenReturn(List.of(activeSubject));

            // Act
            List<SubjectDetailVO> result = userSubjectService.getSubjectDetailsByUserId(userId);

            // Assert - only active subject returned
            assertEquals(1, result.size());
            assertEquals("Mathematics", result.get(0).getName());
        }

        /**
         * USS-002 applied to details: User with no subjects returns empty list.
         */
        @Test
        @DisplayName("USS-002 (details): User with no subjects returns empty detail list")
        void getSubjectDetailsByUserId_noSubjects_returnsEmptyList() {
            // Arrange
            Long userId = 99L;
            when(userSubjectDao.selectSubjectDetailsByUserId(userId)).thenReturn(Collections.emptyList());

            // Act
            List<SubjectDetailVO> result = userSubjectService.getSubjectDetailsByUserId(userId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        /**
         * USS-005 applied to details: Non-existent user returns empty list.
         */
        @Test
        @DisplayName("USS-005 (details): Non-existent user returns empty detail list")
        void getSubjectDetailsByUserId_nonExistentUser_returnsEmptyList() {
            // Arrange
            Long invalidUserId = 999L;
            when(userSubjectDao.selectSubjectDetailsByUserId(invalidUserId)).thenReturn(Collections.emptyList());

            // Act
            List<SubjectDetailVO> result = userSubjectService.getSubjectDetailsByUserId(invalidUserId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        /**
         * USS-003 supplement: Subject with null description.
         */
        @Test
        @DisplayName("USS-003 Supplement: Subject with null description is handled")
        void getSubjectDetailsByUserId_nullDescription_handledCorrectly() {
            // Arrange
            Long userId = 3L;
            SubjectDetailVO subjectNoDesc = new SubjectDetailVO(3L, "Chemistry", null);
            when(userSubjectDao.selectSubjectDetailsByUserId(userId)).thenReturn(List.of(subjectNoDesc));

            // Act
            List<SubjectDetailVO> result = userSubjectService.getSubjectDetailsByUserId(userId);

            // Assert
            assertEquals(1, result.size());
            assertEquals("Chemistry", result.get(0).getName());
            assertNull(result.get(0).getDescription());
        }

        /**
         * USS-003 supplement: Admin user with all subjects.
         */
        @Test
        @DisplayName("USS-003 Supplement: Admin user returns all associated subjects")
        void getSubjectDetailsByUserId_adminUser_returnsAllSubjects() {
            // Arrange
            Long adminUserId = 1L;
            List<SubjectDetailVO> allSubjects = Arrays.asList(
                    new SubjectDetailVO(1L, "Mathematics", "Advanced mathematics"),
                    new SubjectDetailVO(2L, "Physics", "Classical physics"),
                    new SubjectDetailVO(3L, "Chemistry", "Organic chemistry"),
                    new SubjectDetailVO(4L, "Biology", "Cell biology"),
                    new SubjectDetailVO(5L, "Computer Science", "Programming")
            );
            when(userSubjectDao.selectSubjectDetailsByUserId(adminUserId)).thenReturn(allSubjects);

            // Act
            List<SubjectDetailVO> result = userSubjectService.getSubjectDetailsByUserId(adminUserId);

            // Assert
            assertEquals(5, result.size());
            assertEquals("Mathematics", result.get(0).getName());
            assertEquals("Computer Science", result.get(4).getName());
        }
    }
}
