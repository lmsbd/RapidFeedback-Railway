package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.ProjectPO;
import com.unimelb.swen90017.rfo.pojo.po.StudentPO;
import com.unimelb.swen90017.rfo.pojo.vo.GroupResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.MarkerScoreVO;
import com.unimelb.swen90017.rfo.pojo.vo.StudentResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserDetailVO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DAO tests for ProjectDao count and statistics queries.
 *
 * Key fixtures from test-data.sql:
 * - Project 9 has students 9, 10, 8 and groups 16, 17, 18.
 * - Project 9 has 4 mark_record rows.
 * - Marker 4 marked student 9, has a draft/null total_score for student 10, and has group scores for group 16.
 * - Marker 5 marked student 9 and has a group comment/score for group 16.
 * - Marker 6 is assigned to group 17 but has no group_mark_record or group_score.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ProjectDao Count and Statistics Tests")
class ProjectDaoTest {

    @Autowired
    private ProjectDao projectDao;

    private static List<Long> studentIds(List<StudentResponseVO> students) {
        return students.stream().map(StudentResponseVO::getId).toList();
    }

    private static List<Long> groupIds(List<GroupResponseVO> groups) {
        return groups.stream().map(GroupResponseVO::getId).toList();
    }

    private static List<Long> userIds(List<UserDetailVO> users) {
        return users.stream().map(UserDetailVO::getId).toList();
    }

    private static List<Long> markerIds(List<MarkerScoreVO> scores) {
        return scores.stream().map(MarkerScoreVO::getMarkerId).toList();
    }

    @Nested
    @DisplayName("Project-level statistics")
    class ProjectStatistics {

        @Test
        @DisplayName("PD-001: countMarkRecordsByProjectId counts all mark records for a project")
        void countMarkRecordsByProjectId_countsRows() {
            assertEquals(4, projectDao.countMarkRecordsByProjectId(9L));
            assertEquals(0, projectDao.countMarkRecordsByProjectId(1L));
            assertEquals(0, projectDao.countMarkRecordsByProjectId(999L));
        }

        @Test
        @DisplayName("PD-002: getWeightedMaxScoreByProjectId calculates weighted rubric maximum")
        void getWeightedMaxScoreByProjectId_calculatesWeightedMax() {
            assertEquals(0, new BigDecimal("100.00").compareTo(projectDao.getWeightedMaxScoreByProjectId(9L)));
            assertEquals(0, BigDecimal.ZERO.compareTo(projectDao.getWeightedMaxScoreByProjectId(999L)));
        }
    }

    @Nested
    @DisplayName("Student marked/unmarked counts")
    class StudentCounts {

        @Test
        @DisplayName("PD-003: countUnmarkedStudentsByProjectId is scoped to the selected marker")
        void countUnmarkedStudentsByProjectId_countsByMarkerProgress() {
            assertEquals(2, projectDao.countUnmarkedStudentsByProjectId(9L, 4L));
            assertEquals(2, projectDao.countUnmarkedStudentsByProjectId(9L, 5L));
            assertEquals(3, projectDao.countUnmarkedStudentsByProjectId(9L, 6L));
        }

        @Test
        @DisplayName("PD-004: countMarkedStudentsByProjectId is scoped to the selected marker")
        void countMarkedStudentsByProjectId_countsByMarkerProgress() {
            assertEquals(1, projectDao.countMarkedStudentsByProjectId(9L, 4L));
            assertEquals(1, projectDao.countMarkedStudentsByProjectId(9L, 5L));
            assertEquals(0, projectDao.countMarkedStudentsByProjectId(9L, 6L));
        }

        @Test
        @DisplayName("PD-005: marker-filtered unmarked student count only considers assigned students")
        void countUnmarkedStudentsByProjectIdAndMarker_countsAssignedUnmarked() {
            assertEquals(1, projectDao.countUnmarkedStudentsByProjectIdAndMarker(9L, 4L));
            assertEquals(0, projectDao.countUnmarkedStudentsByProjectIdAndMarker(9L, 5L));
            assertEquals(0, projectDao.countUnmarkedStudentsByProjectIdAndMarker(9L, 6L));
        }

        @Test
        @DisplayName("PD-006: marker-filtered marked student count only considers assigned students")
        void countMarkedStudentsByProjectIdAndMarker_countsAssignedMarked() {
            assertEquals(1, projectDao.countMarkedStudentsByProjectIdAndMarker(9L, 4L));
            assertEquals(1, projectDao.countMarkedStudentsByProjectIdAndMarker(9L, 5L));
            assertEquals(0, projectDao.countMarkedStudentsByProjectIdAndMarker(9L, 6L));
        }
    }

    @Nested
    @DisplayName("Group marked/unmarked counts")
    class GroupCounts {

        @Test
        @DisplayName("PD-007: countUnmarkedGroupsByProjectId is scoped to group comments from the selected marker")
        void countUnmarkedGroupsByProjectId_countsByMarkerProgress() {
            assertEquals(2, projectDao.countUnmarkedGroupsByProjectId(9L, 4L));
            assertEquals(2, projectDao.countUnmarkedGroupsByProjectId(9L, 5L));
            assertEquals(3, projectDao.countUnmarkedGroupsByProjectId(9L, 6L));
        }

        @Test
        @DisplayName("PD-008: countMarkedGroupsByProjectId is scoped to group comments from the selected marker")
        void countMarkedGroupsByProjectId_countsByMarkerProgress() {
            assertEquals(1, projectDao.countMarkedGroupsByProjectId(9L, 4L));
            assertEquals(1, projectDao.countMarkedGroupsByProjectId(9L, 5L));
            assertEquals(0, projectDao.countMarkedGroupsByProjectId(9L, 6L));
        }

        @Test
        @DisplayName("PD-009: marker-filtered unmarked group count only considers assigned groups")
        void countUnmarkedGroupsByProjectIdAndMarker_countsAssignedUnmarked() {
            assertEquals(0, projectDao.countUnmarkedGroupsByProjectIdAndMarker(9L, 4L));
            assertEquals(0, projectDao.countUnmarkedGroupsByProjectIdAndMarker(9L, 5L));
            assertEquals(1, projectDao.countUnmarkedGroupsByProjectIdAndMarker(9L, 6L));
        }

        @Test
        @DisplayName("PD-010: marker-filtered marked group count requires assignment and group comment")
        void countMarkedGroupsByProjectIdAndMarker_countsAssignedMarked() {
            assertEquals(1, projectDao.countMarkedGroupsByProjectIdAndMarker(9L, 4L));
            assertEquals(1, projectDao.countMarkedGroupsByProjectIdAndMarker(9L, 5L));
            assertEquals(0, projectDao.countMarkedGroupsByProjectIdAndMarker(9L, 6L));
        }
    }

    @Nested
    @DisplayName("Student and group list queries")
    class ListQueries {

        @Test
        @DisplayName("PD-011: getUnmarkedStudentsByProjectId returns students not completed by the selected marker")
        void getUnmarkedStudentsByProjectId_returnsExpectedStudents() {
            List<StudentResponseVO> marker4Unmarked = projectDao.getUnmarkedStudentsByProjectId(9L, 4L);
            List<StudentResponseVO> marker6Unmarked = projectDao.getUnmarkedStudentsByProjectId(9L, 6L);

            assertEquals(List.of(8L, 10L), studentIds(marker4Unmarked));
            assertEquals(List.of(8L, 9L, 10L), studentIds(marker6Unmarked));
        }

        @Test
        @DisplayName("PD-012: getMarkedStudentsByProjectId returns averaged score for students marked by the selected marker")
        void getMarkedStudentsByProjectId_returnsExpectedStudents() {
            List<StudentResponseVO> marker4Marked = projectDao.getMarkedStudentsByProjectId(9L, 4L);

            assertEquals(List.of(9L), studentIds(marker4Marked));
            assertEquals(0, new BigDecimal("79.375").compareTo(marker4Marked.get(0).getTotalScore()));
        }

        @Test
        @DisplayName("PD-013: marker-filtered student lists only include assigned students")
        void markerFilteredStudentLists_returnAssignedStudentsOnly() {
            assertEquals(List.of(10L), studentIds(projectDao.getUnmarkedStudentsByProjectIdAndMarker(9L, 4L)));
            assertEquals(List.of(9L), studentIds(projectDao.getMarkedStudentsByProjectIdAndMarker(9L, 4L)));
            assertEquals(List.of(), studentIds(projectDao.getUnmarkedStudentsByProjectIdAndMarker(9L, 6L)));
            assertEquals(List.of(), studentIds(projectDao.getMarkedStudentsByProjectIdAndMarker(9L, 6L)));
        }

        @Test
        @DisplayName("PD-014: getUnmarkedGroupsByProjectId returns groups without selected marker comment")
        void getUnmarkedGroupsByProjectId_returnsExpectedGroups() {
            assertEquals(List.of(17L, 18L), groupIds(projectDao.getUnmarkedGroupsByProjectId(9L, 4L)));
            assertEquals(List.of(16L, 17L, 18L), groupIds(projectDao.getUnmarkedGroupsByProjectId(9L, 6L)));
        }

        @Test
        @Disabled("H2 does not support the correlated derived-table query used by ProjectDao.getMarkedGroupsByProjectId")
        @DisplayName("PD-015: getMarkedGroupsByProjectId returns groups with selected marker comment")
        void getMarkedGroupsByProjectId_returnsExpectedGroups() {
            List<GroupResponseVO> marker4Marked = projectDao.getMarkedGroupsByProjectId(9L, 4L);

            assertEquals(List.of(16L), groupIds(marker4Marked));
            assertEquals(0, new BigDecimal("73.38").compareTo(marker4Marked.get(0).getTotalScore()));
        }

        @Test
        @DisplayName("PD-016: marker-filtered group lists only include assigned groups")
        void markerFilteredGroupLists_returnAssignedGroupsOnly() {
            assertEquals(List.of(), groupIds(projectDao.getUnmarkedGroupsByProjectIdAndMarker(9L, 4L)));
            assertEquals(List.of(16L), groupIds(projectDao.getMarkedGroupsByProjectIdAndMarker(9L, 4L)));
            assertEquals(List.of(17L), groupIds(projectDao.getUnmarkedGroupsByProjectIdAndMarker(9L, 6L)));
            assertEquals(List.of(), groupIds(projectDao.getMarkedGroupsByProjectIdAndMarker(9L, 6L)));
        }

        @Test
        @DisplayName("PD-017: selectStudentsByGroupIdInProject returns active group members")
        void selectStudentsByGroupIdInProject_returnsActiveMembers() {
            List<StudentPO> students = projectDao.selectStudentsByGroupIdInProject(16L);

            assertEquals(List.of(6L, 9L), students.stream().map(StudentPO::getId).sorted().toList());
        }
    }

    @Nested
    @DisplayName("Marker lookup and marker score queries")
    class MarkerQueries {

        @Test
        @DisplayName("PD-018: getMarkersByStudentAndProject returns assigned markers")
        void getMarkersByStudentAndProject_returnsAssignedMarkers() {
            List<UserDetailVO> markers = projectDao.getMarkersByStudentAndProject(9L, 9L);

            assertEquals(List.of(4L, 5L), userIds(markers));
            assertEquals("marker3", markers.get(0).getUsername());
            assertEquals("marker4", markers.get(1).getUsername());
        }

        @Test
        @DisplayName("PD-019: getMarkersByGroupAndProject returns assigned group markers")
        void getMarkersByGroupAndProject_returnsAssignedMarkers() {
            List<UserDetailVO> markers = projectDao.getMarkersByGroupAndProject(9L, 16L);

            assertEquals(List.of(4L, 5L), userIds(markers));
        }

        @Test
        @DisplayName("PD-020: getMarkerScoresByProjectAndStudent returns per-marker individual scores")
        void getMarkerScoresByProjectAndStudent_returnsScores() {
            List<MarkerScoreVO> scores = projectDao.getMarkerScoresByProjectAndStudent(9L, 9L);

            assertEquals(List.of(4L, 5L), markerIds(scores));
            assertEquals(0, new BigDecimal("76.75").compareTo(scores.get(0).getScore()));
            assertEquals(0, new BigDecimal("82.00").compareTo(scores.get(1).getScore()));
        }

        @Test
        @DisplayName("PD-021: getMarkerScoresByProjectAndGroup returns null for partial group marks")
        void getMarkerScoresByProjectAndGroup_requiresCompleteGroupScores() {
            List<MarkerScoreVO> scores = projectDao.getMarkerScoresByProjectAndGroup(9L, 16L);

            assertEquals(List.of(4L, 5L), markerIds(scores));
            assertEquals(0, new BigDecimal("76.75").compareTo(scores.get(0).getScore()));
            assertNull(scores.get(1).getScore());
        }

        @Test
        @DisplayName("PD-022: getMarkerScoresByProjectAndGroupStudent returns group scores for one student")
        void getMarkerScoresByProjectAndGroupStudent_returnsScores() {
            List<MarkerScoreVO> scores = projectDao.getMarkerScoresByProjectAndGroupStudent(9L, 16L, 9L);

            assertEquals(List.of(4L, 5L), markerIds(scores));
            assertEquals(0, new BigDecimal("76.75").compareTo(scores.get(0).getScore()));
            assertEquals(0, new BigDecimal("70.00").compareTo(scores.get(1).getScore()));
        }
    }

    @Nested
    @DisplayName("Project update queries")
    class UpdateQueries {

        @Test
        @DisplayName("PD-023: updateProjectName updates name without changing countdown")
        void updateProjectName_updatesOnlyName() {
            projectDao.updateProjectName(9L, "Updated SE Project");

            ProjectPO project = projectDao.selectById(9L);
            assertNotNull(project);
            assertEquals("Updated SE Project", project.getName());
            assertEquals(1500000L, project.getCountdown());
        }

        @Test
        @DisplayName("PD-024: updateProjectNameAndCountdown updates both fields")
        void updateProjectNameAndCountdown_updatesBothFields() {
            projectDao.updateProjectNameAndCountdown(9L, "Timed SE Project", 42000L);

            ProjectPO project = projectDao.selectById(9L);
            assertNotNull(project);
            assertEquals("Timed SE Project", project.getName());
            assertEquals(42000L, project.getCountdown());
        }
    }
}
