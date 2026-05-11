package com.unimelb.swen90017.rfo.service.impl;

import com.unimelb.swen90017.rfo.dao.FinalMarkDao;
import com.unimelb.swen90017.rfo.dao.ProjectDao;
import com.unimelb.swen90017.rfo.dao.StudentDao;
import com.unimelb.swen90017.rfo.dao.UserDao;
import com.unimelb.swen90017.rfo.pojo.po.FinalMarkPO;
import com.unimelb.swen90017.rfo.pojo.po.ProjectGroupPO;
import com.unimelb.swen90017.rfo.pojo.po.ProjectPO;
import com.unimelb.swen90017.rfo.pojo.po.StudentPO;
import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import com.unimelb.swen90017.rfo.pojo.vo.FinalMarkItemVO;
import com.unimelb.swen90017.rfo.pojo.vo.FinalMarkListResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.MarkerScoreVO;
import com.unimelb.swen90017.rfo.pojo.vo.StudentResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.LockFinalMarkRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SaveFinalMarkRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalMarkServiceImplTest {

    @Mock
    private ProjectDao projectDao;

    @Mock
    private FinalMarkDao finalMarkDao;

    @Mock
    private StudentDao studentDao;

    @Mock
    private UserDao userDao;

    private FinalMarkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinalMarkServiceImpl();
        ReflectionTestUtils.setField(service, "projectDao", projectDao);
        ReflectionTestUtils.setField(service, "finalMarkDao", finalMarkDao);
        ReflectionTestUtils.setField(service, "studentDao", studentDao);
        ReflectionTestUtils.setField(service, "userDao", userDao);
    }

    @Test
    void getFinalMarkList_individualProject_buildsRowsAndAverageScores() {
        ProjectPO project = project(1L, 10L, "Project 1", "individual");
        StudentResponseVO student = new StudentResponseVO();
        student.setId(1001L);
        student.setStudentId(2001L);
        student.setFirstName("Alice");
        student.setSurname("Lee");
        student.setEmail("alice@example.com");

        MarkerScoreVO markerScore = new MarkerScoreVO();
        markerScore.setMarkerId(501L);
        markerScore.setMarkerName("marker-501");
        markerScore.setScore(new BigDecimal("8.50"));

        UserPO admin = user(99L, "admin-99");
        FinalMarkPO finalMark = FinalMarkPO.builder()
                .id(77L)
                .projectId(1L)
                .studentId(1001L)
                .finalScore(new BigDecimal("9.00"))
                .isLocked(true)
                .build();

        when(projectDao.selectById(1L)).thenReturn(project);
        when(userDao.getAdminsBySubjectId(10L)).thenReturn(List.of(admin));
        when(projectDao.getStudentsByProjectId(1L)).thenReturn(List.of(student));
        when(projectDao.getMarkerScoresByProjectAndStudent(1L, 1001L)).thenReturn(List.of(markerScore));
        when(finalMarkDao.getByProjectAndStudent(1L, 1001L)).thenReturn(finalMark);
        when(finalMarkDao.countCompletedMarkersForStudent(1L, 1001L)).thenReturn(1);
        when(finalMarkDao.countAssignedMarkersForStudent(1L, 1001L)).thenReturn(2);
        when(projectDao.getWeightedMaxScoreByProjectId(1L)).thenReturn(new BigDecimal("100"));

        FinalMarkListResponseVO responseVO = service.getFinalMarkList(1L);

        assertEquals("individual", responseVO.getProjectType());
        assertEquals("Project 1", responseVO.getProjectName());
        assertEquals(new BigDecimal("100.00"), responseVO.getWeightedMaxScore());
        assertEquals(1, responseVO.getItems().size());

        FinalMarkItemVO item = responseVO.getItems().get(0);
        assertEquals(2001L, item.getStudentId());
        assertEquals(new BigDecimal("8.50"), item.getAverageScore());
        assertEquals(new BigDecimal("9.00"), item.getFinalScore());
        assertTrue(item.getIsLocked());
        assertEquals(1, item.getCompletedMarkers());
        assertEquals(2, item.getTotalAssignedMarkers());
        assertEquals(2, item.getMarkerScores().size());
    }

    @Test
    void getFinalMarkList_groupProject_buildsGroupRowsAndAdminPlaceholders() {
        ProjectPO project = project(2L, 20L, "Project 2", "group");
        ProjectGroupPO group = ProjectGroupPO.builder()
                .id(300L)
                .projectId(2L)
                .groupName("Team A")
                .build();
        StudentPO student = StudentPO.builder()
                .id(4001L)
                .studentId(2101L)
                .firstName("Bob")
                .surname("Tan")
                .email("bob@example.com")
                .build();
        MarkerScoreVO markerScore = new MarkerScoreVO();
        markerScore.setMarkerId(600L);
        markerScore.setMarkerName("marker-600");
        markerScore.setScore(new BigDecimal("7.00"));
        UserPO admin = user(999L, "admin-999");
        FinalMarkPO finalMark = FinalMarkPO.builder()
                .id(88L)
                .projectId(2L)
                .studentId(4001L)
                .groupId(300L)
                .finalScore(new BigDecimal("8.75"))
                .isLocked(false)
                .build();

        when(projectDao.selectById(2L)).thenReturn(project);
        when(userDao.getAdminsBySubjectId(20L)).thenReturn(List.of(admin));
        when(projectDao.getProjectGroupByProjectId(2L)).thenReturn(List.of(group));
        when(finalMarkDao.countCompletedMarkersForGroup(2L, 300L)).thenReturn(1);
        when(finalMarkDao.countAssignedMarkersForGroup(2L, 300L)).thenReturn(2);
        when(projectDao.selectStudentsByGroupIdInProject(300L)).thenReturn(List.of(student));
        when(projectDao.getMarkerScoresByProjectAndGroupStudent(2L, 300L, 4001L)).thenReturn(List.of(markerScore));
        when(finalMarkDao.getByProjectStudentAndGroup(2L, 4001L, 300L)).thenReturn(finalMark);
        when(projectDao.getWeightedMaxScoreByProjectId(2L)).thenReturn(new BigDecimal("80"));

        FinalMarkListResponseVO responseVO = service.getFinalMarkList(2L);

        assertEquals("group", responseVO.getProjectType());
        assertEquals("Project 2", responseVO.getProjectName());
        assertEquals(new BigDecimal("80.00"), responseVO.getWeightedMaxScore());
        assertEquals(1, responseVO.getItems().size());

        FinalMarkItemVO item = responseVO.getItems().get(0);
        assertEquals(2101L, item.getStudentId());
        assertEquals(300L, item.getGroupId());
        assertEquals("Team A", item.getGroupName());
        assertEquals(new BigDecimal("7.00"), item.getAverageScore());
        assertEquals(new BigDecimal("8.75"), item.getFinalScore());
        assertFalse(item.getIsLocked());
        assertEquals(1, item.getCompletedMarkers());
        assertEquals(2, item.getTotalAssignedMarkers());
        assertEquals(2, item.getMarkerScores().size());
    }

    @Test
    void saveFinalMark_individualInsertSetsUnlockedDefault() {
        StudentPO student = StudentPO.builder()
                .id(1001L)
                .studentId(2001L)
                .build();
        when(studentDao.findByStudentId(2001L)).thenReturn(student);
        when(finalMarkDao.getByProjectAndStudent(1L, 1001L)).thenReturn(null);

        SaveFinalMarkRequestVO requestVO = new SaveFinalMarkRequestVO();
        requestVO.setProjectId(1L);
        requestVO.setStudentId(2001L);
        requestVO.setFinalScore(new BigDecimal("88.5"));

        service.saveFinalMark(requestVO);

        ArgumentCaptor<FinalMarkPO> captor = ArgumentCaptor.forClass(FinalMarkPO.class);
        verify(finalMarkDao).insert(captor.capture());
        FinalMarkPO inserted = captor.getValue();
        assertEquals(1L, inserted.getProjectId());
        assertEquals(1001L, inserted.getStudentId());
        assertEquals(new BigDecimal("88.5"), inserted.getFinalScore());
        assertFalse(Boolean.TRUE.equals(inserted.getIsLocked()));
    }

    @Test
    void lockFinalMark_groupUpdateChangesLockState() {
        FinalMarkPO existing = FinalMarkPO.builder()
                .id(900L)
                .projectId(3L)
                .groupId(77L)
                .isLocked(false)
                .build();
        when(finalMarkDao.getByProjectAndGroup(3L, 77L)).thenReturn(existing);

        LockFinalMarkRequestVO requestVO = new LockFinalMarkRequestVO();
        requestVO.setProjectId(3L);
        requestVO.setGroupId(77L);
        requestVO.setIsLocked(true);

        service.lockFinalMark(requestVO);

        ArgumentCaptor<FinalMarkPO> captor = ArgumentCaptor.forClass(FinalMarkPO.class);
        verify(finalMarkDao).updateById(captor.capture());
        assertTrue(captor.getValue().getIsLocked());
    }

    @Test
    void saveFinalMark_throwsWhenLockedRecordExists() {
        StudentPO student = StudentPO.builder()
                .id(1001L)
                .studentId(2001L)
                .build();
        FinalMarkPO existing = FinalMarkPO.builder()
                .id(777L)
                .projectId(1L)
                .studentId(1001L)
                .isLocked(true)
                .build();
        when(studentDao.findByStudentId(2001L)).thenReturn(student);
        when(finalMarkDao.getByProjectAndStudent(1L, 1001L)).thenReturn(existing);

        SaveFinalMarkRequestVO requestVO = new SaveFinalMarkRequestVO();
        requestVO.setProjectId(1L);
        requestVO.setStudentId(2001L);
        requestVO.setFinalScore(new BigDecimal("90"));

        assertThrows(IllegalStateException.class, () -> service.saveFinalMark(requestVO));
    }

    private ProjectPO project(Long id, Long subjectId, String name, String type) {
        return ProjectPO.builder()
                .id(id)
                .subjectId(subjectId)
                .name(name)
                .projectType(type)
                .countdown(30L)
                .templateId(1L)
                .build();
    }

    private UserPO user(Long id, String username) {
        return UserPO.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .role(2)
                .build();
    }
}
