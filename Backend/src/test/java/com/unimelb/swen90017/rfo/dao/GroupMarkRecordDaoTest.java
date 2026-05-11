package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.dto.GroupStudentMarkDTO;
import com.unimelb.swen90017.rfo.pojo.po.GroupMarkRecordPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DAO tests for GroupMarkRecordDao.
 *
 * Key fixtures from test-data.sql:
 * - Group 16 in project 9 has comments from markers 4 and 5.
 * - Group 17 is assigned to marker 6 but has no group_mark_record.
 * - Group 16 has students 9 and 6; marker 4 scored both, marker 5 only scored student 9.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("GroupMarkRecordDao Tests")
class GroupMarkRecordDaoTest {

    @Autowired
    private GroupMarkRecordDao groupMarkRecordDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static List<Long> recordIds(List<GroupMarkRecordPO> records) {
        return records.stream().map(GroupMarkRecordPO::getId).sorted().toList();
    }

    private static Map<Long, BigDecimal> scoresByStudent(List<GroupStudentMarkDTO> scores) {
        Map<Long, BigDecimal> result = new HashMap<>();
        scores.forEach(score -> result.put(score.getStudentId(), score.getGroupScore()));
        return result;
    }

    @Nested
    @DisplayName("Group mark record lookup queries")
    class LookupQueries {

        @Test
        @DisplayName("GMRD-001: getByProjectGroupAndMarker returns one marker comment")
        void getByProjectGroupAndMarker_existing_returnsRecord() {
            GroupMarkRecordPO result = groupMarkRecordDao.getByProjectGroupAndMarker(9L, 16L, 4L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(9L, result.getProjectId());
            assertEquals(16L, result.getGroupId());
            assertEquals(4L, result.getMarkerId());
            assertEquals("Good team effort from marker 3.", result.getComment());
        }

        @Test
        @DisplayName("GMRD-002: getByProjectGroupAndMarker returns null for missing marker comment")
        void getByProjectGroupAndMarker_missing_returnsNull() {
            assertNull(groupMarkRecordDao.getByProjectGroupAndMarker(9L, 17L, 6L));
        }

        @Test
        @DisplayName("GMRD-003: getAllByProjectAndGroup returns every marker comment for a group")
        void getAllByProjectAndGroup_returnsAllMarkerComments() {
            List<GroupMarkRecordPO> results = groupMarkRecordDao.getAllByProjectAndGroup(9L, 16L);

            assertEquals(List.of(1L, 2L), recordIds(results));
            assertEquals(List.of(), groupMarkRecordDao.getAllByProjectAndGroup(9L, 17L));
        }

        @Test
        @DisplayName("GMRD-004: getByProjectId returns all group mark records for the project")
        void getByProjectId_returnsProjectRows() {
            List<GroupMarkRecordPO> results = groupMarkRecordDao.getByProjectId(9L);

            assertEquals(List.of(1L, 2L), recordIds(results));
            assertEquals(List.of(), groupMarkRecordDao.getByProjectId(999L));
        }
    }

    @Nested
    @DisplayName("Group membership and score joins")
    class GroupMembershipAndScores {

        @Test
        @DisplayName("GMRD-005: getStudentIdsByGroupId returns active members")
        void getStudentIdsByGroupId_returnsActiveMembers() {
            assertEquals(List.of(6L, 9L), groupMarkRecordDao.getStudentIdsByGroupId(16L).stream().sorted().toList());
        }

        @Test
        @DisplayName("GMRD-006: getStudentIdsByGroupId excludes soft-deleted members")
        void getStudentIdsByGroupId_excludesSoftDeletedMembers() {
            jdbcTemplate.update("UPDATE group_student SET delete_status = 1 WHERE group_id = ? AND student_id = ?", 16L, 9L);

            assertEquals(List.of(6L), groupMarkRecordDao.getStudentIdsByGroupId(16L));
        }

        @Test
        @DisplayName("GMRD-007: getStudentGroupScores returns per-student group scores for selected marker")
        void getStudentGroupScores_returnsScoresForMarker() {
            Map<Long, BigDecimal> marker4Scores = scoresByStudent(groupMarkRecordDao.getStudentGroupScores(9L, 16L, 4L));
            Map<Long, BigDecimal> marker5Scores = scoresByStudent(groupMarkRecordDao.getStudentGroupScores(9L, 16L, 5L));

            assertEquals(0, new BigDecimal("76.75").compareTo(marker4Scores.get(9L)));
            assertEquals(0, new BigDecimal("76.75").compareTo(marker4Scores.get(6L)));
            assertEquals(0, new BigDecimal("70.00").compareTo(marker5Scores.get(9L)));
            assertNull(marker5Scores.get(6L));
        }

        @Test
        @DisplayName("GMRD-008: getStudentGroupScores excludes soft-deleted members")
        void getStudentGroupScores_excludesSoftDeletedMembers() {
            jdbcTemplate.update("UPDATE group_student SET delete_status = 1 WHERE group_id = ? AND student_id = ?", 16L, 9L);

            List<GroupStudentMarkDTO> results = groupMarkRecordDao.getStudentGroupScores(9L, 16L, 4L);

            assertEquals(List.of(6L), results.stream().map(GroupStudentMarkDTO::getStudentId).toList());
            assertEquals(0, new BigDecimal("76.75").compareTo(results.get(0).getGroupScore()));
        }

        @Test
        @DisplayName("GMRD-009: getMarkerIdsByGroup returns distinct markers who scored active members")
        void getMarkerIdsByGroup_returnsDistinctMarkers() {
            assertEquals(List.of(4L, 5L), groupMarkRecordDao.getMarkerIdsByGroup(9L, 16L).stream().sorted().toList());
            assertEquals(List.of(4L), groupMarkRecordDao.getMarkerIdsByGroup(9L, 17L));
        }

        @Test
        @DisplayName("GMRD-010: getMarkerIdsByGroup excludes soft-deleted members")
        void getMarkerIdsByGroup_excludesSoftDeletedMembers() {
            jdbcTemplate.update("UPDATE group_student SET delete_status = 1 WHERE group_id = ? AND student_id = ?", 16L, 9L);

            assertEquals(List.of(4L), groupMarkRecordDao.getMarkerIdsByGroup(9L, 16L));
        }
    }

    @Nested
    @DisplayName("Aggregate and persistence behavior")
    class AggregateAndPersistence {

        @Test
        @DisplayName("GMRD-011: getGroupTotalScore averages per-marker minimum group scores")
        void getGroupTotalScore_averagesPerMarkerMinimumScores() {
            BigDecimal totalScore = groupMarkRecordDao.getGroupTotalScore(9L, 16L);

            assertEquals(0, new BigDecimal("73.375").compareTo(totalScore));
            assertNull(groupMarkRecordDao.getGroupTotalScore(9L, 17L));
        }

        @Test
        @DisplayName("GMRD-012: getGroupTotalScore excludes soft-deleted group members")
        void getGroupTotalScore_excludesSoftDeletedMembers() {
            jdbcTemplate.update("UPDATE group_student SET delete_status = 1 WHERE group_id = ? AND student_id = ?", 16L, 9L);

            BigDecimal totalScore = groupMarkRecordDao.getGroupTotalScore(9L, 16L);

            assertEquals(0, new BigDecimal("76.75").compareTo(totalScore));
        }

        @Test
        @DisplayName("GMRD-013: upsertGroupComment updates an existing marker comment")
        void upsertGroupComment_updatesExistingComment() {
            LocalDateTime markTime = LocalDateTime.of(2025, 10, 2, 14, 15);

            groupMarkRecordDao.upsertGroupComment(9L, 16L, 4L, "Updated group comment", markTime);

            GroupMarkRecordPO updated = groupMarkRecordDao.getByProjectGroupAndMarker(9L, 16L, 4L);
            assertNotNull(updated);
            assertEquals(1L, updated.getId());
            assertEquals("Updated group comment", updated.getComment());
            assertEquals(markTime, updated.getMarkTime());
        }

        @Test
        @DisplayName("GMRD-014: upsertGroupComment inserts a missing marker comment")
        void upsertGroupComment_insertsMissingComment() {
            LocalDateTime markTime = LocalDateTime.of(2025, 10, 2, 15, 45);

            groupMarkRecordDao.upsertGroupComment(9L, 17L, 6L, "New marker comment", markTime);

            GroupMarkRecordPO inserted = groupMarkRecordDao.getByProjectGroupAndMarker(9L, 17L, 6L);
            assertNotNull(inserted);
            assertEquals(9L, inserted.getProjectId());
            assertEquals(17L, inserted.getGroupId());
            assertEquals(6L, inserted.getMarkerId());
            assertEquals("New marker comment", inserted.getComment());
            assertEquals(markTime, inserted.getMarkTime());
        }
    }
}
