package com.unimelb.swen90017.rfo.service;

import com.unimelb.swen90017.rfo.dao.GroupMarkRecordDao;
import com.unimelb.swen90017.rfo.pojo.dto.GroupStudentMarkDTO;
import com.unimelb.swen90017.rfo.pojo.po.GroupMarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import com.unimelb.swen90017.rfo.pojo.vo.GroupMarkResponseVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetails;
import com.unimelb.swen90017.rfo.service.impl.MarkServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarkServiceImpl.
 *
 * Currently covers getGroupMark().
 * saveMark() and saveGroupMark() are deferred pending business logic stabilization.
 *
 * @see com.unimelb.swen90017.rfo.service.impl.MarkServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarkService Unit Tests")
class MarkServiceTest {

    private static final Long MARKER_ID = 4L;

    @Mock
    private GroupMarkRecordDao groupMarkRecordDao;

    @InjectMocks
    private MarkServiceImpl markService;

    @BeforeEach
    void setUpSecurityContext() {
        UserPO marker = UserPO.builder()
                .id(MARKER_ID)
                .username("marker3")
                .email("marker3@example.com")
                .password("password")
                .role(2)
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(marker);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== getGroupMark Tests ====================

    @Nested
    @DisplayName("getGroupMark Tests")
    class GetGroupMarkTests {

        @Test
        @DisplayName("MS-001: Should return group mark with comment and student scores")
        void getGroupMark_withExistingRecord_returnsFullResponse() {
            // Arrange
            GroupMarkRecordPO record = new GroupMarkRecordPO();
            record.setComment("Good team effort");

            GroupStudentMarkDTO s1 = new GroupStudentMarkDTO();
            s1.setStudentId(100L);
            s1.setGroupScore(BigDecimal.valueOf(85));

            GroupStudentMarkDTO s2 = new GroupStudentMarkDTO();
            s2.setStudentId(101L);
            s2.setGroupScore(BigDecimal.valueOf(90));

            when(groupMarkRecordDao.getByProjectGroupAndMarker(1L, 10L, MARKER_ID)).thenReturn(record);
            when(groupMarkRecordDao.getStudentGroupScores(1L, 10L, MARKER_ID)).thenReturn(Arrays.asList(s1, s2));

            // Act
            GroupMarkResponseVO result = markService.getGroupMark(1L, 10L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getProjectId());
            assertEquals(10L, result.getGroupId());
            assertEquals("Good team effort", result.getComment());
            assertEquals(2, result.getStudents().size());
            assertEquals(100L, result.getStudents().get(0).getStudentId());
            assertEquals(BigDecimal.valueOf(85), result.getStudents().get(0).getGroupScore());

            verify(groupMarkRecordDao).getByProjectGroupAndMarker(1L, 10L, MARKER_ID);
            verify(groupMarkRecordDao).getStudentGroupScores(1L, 10L, MARKER_ID);
        }

        @Test
        @DisplayName("MS-002: Should return null comment when group not yet marked")
        void getGroupMark_noRecord_returnsNullComment() {
            // Arrange
            when(groupMarkRecordDao.getByProjectGroupAndMarker(1L, 10L, MARKER_ID)).thenReturn(null);
            when(groupMarkRecordDao.getStudentGroupScores(1L, 10L, MARKER_ID)).thenReturn(Collections.emptyList());

            // Act
            GroupMarkResponseVO result = markService.getGroupMark(1L, 10L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getProjectId());
            assertEquals(10L, result.getGroupId());
            assertNull(result.getComment());
            assertTrue(result.getStudents().isEmpty());
        }

        @Test
        @DisplayName("MS-003: Should return correct projectId and groupId in response")
        void getGroupMark_passesCorrectParams() {
            // Arrange
            when(groupMarkRecordDao.getByProjectGroupAndMarker(5L, 20L, MARKER_ID)).thenReturn(null);
            when(groupMarkRecordDao.getStudentGroupScores(5L, 20L, MARKER_ID)).thenReturn(Collections.emptyList());

            // Act
            GroupMarkResponseVO result = markService.getGroupMark(5L, 20L);

            // Assert
            assertEquals(5L, result.getProjectId());
            assertEquals(20L, result.getGroupId());

            verify(groupMarkRecordDao).getByProjectGroupAndMarker(5L, 20L, MARKER_ID);
            verify(groupMarkRecordDao).getStudentGroupScores(5L, 20L, MARKER_ID);
        }

        @Test
        @DisplayName("MS-004: Should return empty students when no scores recorded")
        void getGroupMark_recordExistsButNoStudentScores() {
            // Arrange - group record exists with comment but no student scores yet
            GroupMarkRecordPO record = new GroupMarkRecordPO();
            record.setComment("Pending individual scoring");

            when(groupMarkRecordDao.getByProjectGroupAndMarker(1L, 10L, MARKER_ID)).thenReturn(record);
            when(groupMarkRecordDao.getStudentGroupScores(1L, 10L, MARKER_ID)).thenReturn(Collections.emptyList());

            // Act
            GroupMarkResponseVO result = markService.getGroupMark(1L, 10L);

            // Assert
            assertEquals("Pending individual scoring", result.getComment());
            assertTrue(result.getStudents().isEmpty());
        }
    }
}
