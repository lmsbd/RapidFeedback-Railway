package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.GroupMarkDetailPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAO layer tests for GroupMarkDetailDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference (group_mark_detail for group_mark_record_id=1):
 *   - criteria_id=1: score=80.0, comment='Good vocal delivery', status=1
 *   - criteria_id=2: score=75.0, comment='', status=1
 *   - criteria_id=3: score=90.0, comment='Clear slides', status=1
 *   - criteria_id=4: score=70.0, comment='', status=1
 *   - criteria_id=5: score=75.0, comment='', status=1
 *   - criteria_id=6: score=80.0, comment='', status=1
 *   - criteria_id=7: score=60.0, comment='', status=1
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("GroupMarkDetailDao Integration Tests")
class GroupMarkDetailDaoTest {

    @Autowired
    private GroupMarkDetailDao groupMarkDetailDao;

    // ==================== getByCriteriaId Tests ====================

    @Nested
    @DisplayName("getByCriteriaId Tests")
    class GetByCriteriaIdTests {

        @Test
        @DisplayName("GMDD-001: Returns detail for record 1, criteria 1")
        void getByCriteriaId_existing() {
            GroupMarkDetailPO detail = groupMarkDetailDao.getByCriteriaId(1L, 1L);
            assertNotNull(detail);
            assertEquals(0, new BigDecimal("80.0").compareTo(detail.getScore()));
            assertEquals("Good vocal delivery", detail.getComment());
            assertEquals(1, detail.getStatus());
        }

        @Test
        @DisplayName("GMDD-002: Returns detail for record 1, criteria 3")
        void getByCriteriaId_criteria3() {
            GroupMarkDetailPO detail = groupMarkDetailDao.getByCriteriaId(1L, 3L);
            assertNotNull(detail);
            assertEquals(0, new BigDecimal("90.0").compareTo(detail.getScore()));
            assertEquals("Clear slides", detail.getComment());
        }

        @Test
        @DisplayName("GMDD-003: Returns null for non-existent criteria")
        void getByCriteriaId_nonExistent() {
            GroupMarkDetailPO detail = groupMarkDetailDao.getByCriteriaId(1L, 999L);
            assertNull(detail);
        }

        @Test
        @DisplayName("GMDD-004: Returns null for non-existent record")
        void getByCriteriaId_nonExistentRecord() {
            GroupMarkDetailPO detail = groupMarkDetailDao.getByCriteriaId(999L, 1L);
            assertNull(detail);
        }
    }

    // ==================== getByGroupMarkRecordId Tests ====================

    @Nested
    @DisplayName("getByGroupMarkRecordId Tests")
    class GetByGroupMarkRecordIdTests {

        @Test
        @DisplayName("GMDD-005: Returns all 7 details for group mark record 1")
        void getByGroupMarkRecordId_record1() {
            List<GroupMarkDetailPO> details = groupMarkDetailDao.getByGroupMarkRecordId(1L);
            assertEquals(7, details.size());
        }

        @Test
        @DisplayName("GMDD-006: Returns empty for non-existent record")
        void getByGroupMarkRecordId_nonExistent() {
            List<GroupMarkDetailPO> details = groupMarkDetailDao.getByGroupMarkRecordId(999L);
            assertTrue(details.isEmpty());
        }
    }

    // ==================== insertGroupMarkDetail Tests ====================

    @Nested
    @DisplayName("insertGroupMarkDetail Tests")
    class InsertGroupMarkDetailTests {

        @Test
        @DisplayName("GMDD-007: Inserts new group mark detail")
        void insertGroupMarkDetail_success() {
            // First create a new group_mark_record to avoid unique constraint issues
            // Use record 1 with a criteria that doesn't exist yet (e.g., 8)
            GroupMarkDetailPO newDetail = GroupMarkDetailPO.builder()
                    .groupMarkRecordId(1L)
                    .criteriaId(8L)
                    .score(new BigDecimal("88.0"))
                    .comment("Excellent teamwork")
                    .status(0)
                    .build();

            groupMarkDetailDao.insertGroupMarkDetail(newDetail);

            // Verify
            GroupMarkDetailPO saved = groupMarkDetailDao.getByCriteriaId(1L, 8L);
            assertNotNull(saved);
            assertEquals(0, new BigDecimal("88.0").compareTo(saved.getScore()));
            assertEquals("Excellent teamwork", saved.getComment());
            assertEquals(0, saved.getStatus());
        }
    }

    // ==================== updateGroupMarkDetail Tests ====================

    @Nested
    @DisplayName("updateGroupMarkDetail Tests")
    class UpdateGroupMarkDetailTests {

        @Test
        @DisplayName("GMDD-008: Updates existing group mark detail")
        void updateGroupMarkDetail_success() {
            // Get existing detail
            GroupMarkDetailPO detail = groupMarkDetailDao.getByCriteriaId(1L, 1L);
            assertNotNull(detail);

            // Update
            detail.setScore(new BigDecimal("95.0"));
            detail.setComment("Updated group comment");
            detail.setStatus(0);
            groupMarkDetailDao.updateGroupMarkDetail(detail);

            // Verify
            GroupMarkDetailPO updated = groupMarkDetailDao.getByCriteriaId(1L, 1L);
            assertEquals(0, new BigDecimal("95.0").compareTo(updated.getScore()));
            assertEquals("Updated group comment", updated.getComment());
            assertEquals(0, updated.getStatus());
        }
    }
}
