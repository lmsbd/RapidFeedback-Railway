package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.MarkRecordPO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DAO tests for MarkRecordDao.
 *
 * Key fixtures from test-data.sql:
 * - Project 9 has mark records for students 9, 10, and 6.
 * - Student 9 has two marker rows: marker 4 scored 76.75, marker 5 scored 82.00.
 * - Student 10 has a draft/null score from marker 4.
 * - Group 16 has active members 9 and 6; group_student soft delete is tested per transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MarkRecordDao Tests")
class MarkRecordDaoTest {

    @Autowired
    private MarkRecordDao markRecordDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static List<Long> recordIds(List<MarkRecordPO> records) {
        return records.stream().map(MarkRecordPO::getId).sorted().toList();
    }

    @Nested
    @DisplayName("Mark record lookup queries")
    class LookupQueries {

        @Test
        @DisplayName("MRD-001: getByProjectAndStudent returns one matching row")
        void getByProjectAndStudent_existing_returnsRecord() {
            MarkRecordPO result = markRecordDao.getByProjectAndStudent(9L, 9L);

            assertNotNull(result);
            assertEquals(9L, result.getProjectId());
            assertEquals(9L, result.getStudentId());
            assertEquals(4L, result.getMarkerId());
            assertEquals(0, new BigDecimal("76.75").compareTo(result.getTotalScore()));
        }

        @Test
        @DisplayName("MRD-002: getByProjectAndStudent returns null for missing student")
        void getByProjectAndStudent_missing_returnsNull() {
            assertNull(markRecordDao.getByProjectAndStudent(9L, 8L));
        }

        @Test
        @DisplayName("MRD-003: getAllByProjectAndStudent returns all marker rows")
        void getAllByProjectAndStudent_returnsAllMarkers() {
            List<MarkRecordPO> results = markRecordDao.getAllByProjectAndStudent(9L, 9L);

            assertEquals(List.of(1L, 3L), recordIds(results));
        }

        @Test
        @DisplayName("MRD-004: getByProjectAndStudentAndMarker filters to one marker")
        void getByProjectAndStudentAndMarker_filtersByMarker() {
            MarkRecordPO marker5Record = markRecordDao.getByProjectAndStudentAndMarker(9L, 9L, 5L);
            MarkRecordPO missingMarker = markRecordDao.getByProjectAndStudentAndMarker(9L, 10L, 5L);

            assertNotNull(marker5Record);
            assertEquals(3L, marker5Record.getId());
            assertEquals(0, new BigDecimal("82.00").compareTo(marker5Record.getTotalScore()));
            assertNull(missingMarker);
        }

        @Test
        @DisplayName("MRD-005: getByProjectId returns every mark row for the project")
        void getByProjectId_returnsProjectRows() {
            List<MarkRecordPO> results = markRecordDao.getByProjectId(9L);

            assertEquals(List.of(1L, 2L, 3L, 4L), recordIds(results));
            assertEquals(List.of(), markRecordDao.getByProjectId(999L));
        }
    }

    @Nested
    @DisplayName("Group join queries")
    class GroupJoinQueries {

        @Test
        @DisplayName("MRD-006: getByProjectAndGroup returns all active group member mark rows")
        void getByProjectAndGroup_returnsActiveMemberRows() {
            List<MarkRecordPO> results = markRecordDao.getByProjectAndGroup(9L, 16L);

            assertEquals(List.of(1L, 3L, 4L), recordIds(results));
        }

        @Test
        @DisplayName("MRD-007: getByProjectAndGroup excludes soft-deleted group members")
        void getByProjectAndGroup_excludesSoftDeletedGroupMembers() {
            jdbcTemplate.update("UPDATE group_student SET delete_status = 1 WHERE group_id = ? AND student_id = ?", 16L, 6L);

            List<MarkRecordPO> results = markRecordDao.getByProjectAndGroup(9L, 16L);

            assertEquals(List.of(1L, 3L), recordIds(results));
        }

        @Test
        @DisplayName("MRD-008: getGroupIdByStudentAndProject returns active group id")
        void getGroupIdByStudentAndProject_returnsActiveGroup() {
            assertEquals(16L, markRecordDao.getGroupIdByStudentAndProject(9L, 9L));
            assertNull(markRecordDao.getGroupIdByStudentAndProject(1L, 9L));
        }

        @Test
        @DisplayName("MRD-009: getGroupIdByStudentAndProject excludes soft-deleted memberships")
        void getGroupIdByStudentAndProject_excludesSoftDeletedMembership() {
            jdbcTemplate.update("UPDATE group_student SET delete_status = 1 WHERE group_id = ? AND student_id = ?", 16L, 9L);

            assertNull(markRecordDao.getGroupIdByStudentAndProject(9L, 9L));
        }
    }

    @Nested
    @DisplayName("Criteria and persistence behavior")
    class CriteriaAndPersistence {

        @Test
        @DisplayName("MRD-010: criteria metadata queries return weighting and maximum mark")
        void criteriaMetadataQueries_returnValues() {
            assertEquals(15, markRecordDao.getWeightingByCriteriaId(1L));
            assertEquals(100, markRecordDao.getMaximumMarkByCriteriaId(1L));
            assertNull(markRecordDao.getWeightingByCriteriaId(999L));
            assertNull(markRecordDao.getMaximumMarkByCriteriaId(999L));
        }

        @Test
        @DisplayName("MRD-011: BaseMapper insert creates a retrievable mark record")
        void insert_createsRecord() {
            MarkRecordPO record = MarkRecordPO.builder()
                    .projectId(9L)
                    .studentId(8L)
                    .markerId(6L)
                    .totalScore(new BigDecimal("88.50"))
                    .groupScore(null)
                    .markTime(LocalDateTime.of(2025, 10, 2, 9, 30))
                    .build();

            assertEquals(1, markRecordDao.insert(record));

            MarkRecordPO saved = markRecordDao.selectById(record.getId());
            assertNotNull(saved);
            assertEquals(8L, saved.getStudentId());
            assertEquals(6L, saved.getMarkerId());
            assertEquals(0, new BigDecimal("88.50").compareTo(saved.getTotalScore()));
        }

        @Test
        @DisplayName("MRD-012: BaseMapper updateById persists score changes")
        void updateById_updatesScores() {
            MarkRecordPO record = markRecordDao.selectById(2L);
            assertNotNull(record);
            assertNull(record.getTotalScore());

            record.setTotalScore(new BigDecimal("69.25"));
            record.setGroupScore(new BigDecimal("71.00"));
            record.setMarkTime(LocalDateTime.of(2025, 10, 2, 10, 0));
            assertEquals(1, markRecordDao.updateById(record));

            MarkRecordPO updated = markRecordDao.selectById(2L);
            assertNotNull(updated);
            assertEquals(0, new BigDecimal("69.25").compareTo(updated.getTotalScore()));
            assertEquals(0, new BigDecimal("71.00").compareTo(updated.getGroupScore()));
            assertEquals(LocalDateTime.of(2025, 10, 2, 10, 0), updated.getMarkTime());
        }
    }
}
