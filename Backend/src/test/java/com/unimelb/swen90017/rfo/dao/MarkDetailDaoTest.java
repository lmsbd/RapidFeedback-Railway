package com.unimelb.swen90017.rfo.dao;

import com.unimelb.swen90017.rfo.pojo.po.MarkDetailPO;
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
 * DAO layer tests for MarkDetailDao.
 * Uses H2 in-memory database with test fixtures from test-data.sql.
 *
 * Test data reference (mark_detail for mark_record_id=1):
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
@DisplayName("MarkDetailDao Integration Tests")
class MarkDetailDaoTest {

    @Autowired
    private MarkDetailDao markDetailDao;

    // ==================== getByCriteriaId Tests ====================

    @Nested
    @DisplayName("getByCriteriaId Tests")
    class GetByCriteriaIdTests {

        @Test
        @DisplayName("MDD-001: Returns mark detail for record 1, criteria 1")
        void getByCriteriaId_existing() {
            MarkDetailPO detail = markDetailDao.getByCriteriaId(1L, 1L);
            assertNotNull(detail);
            assertEquals(0, new BigDecimal("80.0").compareTo(detail.getScore()));
            assertEquals("Good vocal delivery", detail.getComment());
            assertEquals(1, detail.getStatus());
        }

        @Test
        @DisplayName("MDD-002: Returns mark detail for record 1, criteria 3")
        void getByCriteriaId_criteria3() {
            MarkDetailPO detail = markDetailDao.getByCriteriaId(1L, 3L);
            assertNotNull(detail);
            assertEquals(0, new BigDecimal("90.0").compareTo(detail.getScore()));
            assertEquals("Clear slides", detail.getComment());
        }

        @Test
        @DisplayName("MDD-003: Returns null for non-existent criteria")
        void getByCriteriaId_nonExistent() {
            MarkDetailPO detail = markDetailDao.getByCriteriaId(1L, 999L);
            assertNull(detail);
        }

        @Test
        @DisplayName("MDD-004: Returns null for non-existent record")
        void getByCriteriaId_nonExistentRecord() {
            MarkDetailPO detail = markDetailDao.getByCriteriaId(999L, 1L);
            assertNull(detail);
        }
    }

    // ==================== getByMarkRecordId Tests ====================

    @Nested
    @DisplayName("getByMarkRecordId Tests")
    class GetByMarkRecordIdTests {

        @Test
        @DisplayName("MDD-005: Returns all 7 details for mark record 1")
        void getByMarkRecordId_record1() {
            List<MarkDetailPO> details = markDetailDao.getByMarkRecordId(1L);
            assertEquals(7, details.size());
        }

        @Test
        @DisplayName("MDD-006: Returns empty for non-existent record")
        void getByMarkRecordId_nonExistent() {
            List<MarkDetailPO> details = markDetailDao.getByMarkRecordId(999L);
            assertTrue(details.isEmpty());
        }
    }

    // ==================== insertMarkDetail Tests ====================

    @Nested
    @DisplayName("insertMarkDetail Tests")
    class InsertMarkDetailTests {

        @Test
        @DisplayName("MDD-007: Inserts new mark detail")
        void insertMarkDetail_success() {
            MarkDetailPO newDetail = MarkDetailPO.builder()
                    .markRecordId(2L)   // unsubmitted record for student 10
                    .criteriaId(1L)
                    .score(new BigDecimal("85.0"))
                    .comment("Great presentation")
                    .status(0)
                    .build();

            markDetailDao.insertMarkDetail(newDetail);

            // Verify
            MarkDetailPO saved = markDetailDao.getByCriteriaId(2L, 1L);
            assertNotNull(saved);
            assertEquals(0, new BigDecimal("85.0").compareTo(saved.getScore()));
            assertEquals("Great presentation", saved.getComment());
            assertEquals(0, saved.getStatus());
        }
    }

    // ==================== updateMarkDetail Tests ====================

    @Nested
    @DisplayName("updateMarkDetail Tests")
    class UpdateMarkDetailTests {

        @Test
        @DisplayName("MDD-008: Updates existing mark detail")
        void updateMarkDetail_success() {
            // Get existing detail
            MarkDetailPO detail = markDetailDao.getByCriteriaId(1L, 1L);
            assertNotNull(detail);

            // Update
            detail.setScore(new BigDecimal("95.0"));
            detail.setComment("Updated comment");
            detail.setStatus(0);
            markDetailDao.updateMarkDetail(detail);

            // Verify
            MarkDetailPO updated = markDetailDao.getByCriteriaId(1L, 1L);
            assertEquals(0, new BigDecimal("95.0").compareTo(updated.getScore()));
            assertEquals("Updated comment", updated.getComment());
            assertEquals(0, updated.getStatus());
        }
    }
}
