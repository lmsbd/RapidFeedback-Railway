package com.unimelb.swen90017.rfo.service.impl;

import com.unimelb.swen90017.rfo.common.BusinessException;
import com.unimelb.swen90017.rfo.dao.ProjectDao;
import com.unimelb.swen90017.rfo.dao.SubjectDao;
import com.unimelb.swen90017.rfo.pojo.po.ProjectPO;
import com.unimelb.swen90017.rfo.pojo.vo.ProjectResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectDao projectDao;

    @Mock
    private SubjectDao subjectDao;

    private ProjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProjectServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", projectDao);
        ReflectionTestUtils.setField(service, "projectDao", projectDao);
        ReflectionTestUtils.setField(service, "subjectDao", subjectDao);
    }

    @Test
    void hasMarkingStarted_returnsTrueWhenMarkRecordsExist() {
        ProjectPO project = project(1L, 10L, "individual");
        when(projectDao.selectById(1L)).thenReturn(project);
        when(projectDao.countMarkRecordsByProjectId(1L)).thenReturn(3);

        boolean started = service.hasMarkingStarted(1L);

        assertTrue(started);
    }

    @Test
    void hasMarkingStarted_throws404WhenProjectMissing() {
        when(projectDao.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.hasMarkingStarted(1L));

        assertEquals(404, exception.getCode());
        assertEquals("Project not found", exception.getMessage());
    }

    @Test
    void getMarkers_returnsMarkersResolvedFromProjectId() {
        ProjectPO project = project(5L, 77L, "group");
        List<UserResponseVO> markers = List.of(userResponse(101L, 2, "marker-a"));
        when(projectDao.selectById(5L)).thenReturn(project);
        when(subjectDao.getMarkersBySubjectId(77L)).thenReturn(markers);

        List<UserResponseVO> result = service.getMarkers(5L, null);

        assertEquals(markers, result);
    }

    @Test
    void getMarkers_returnsEmptyListWhenSubjectDaoReturnsNull() {
        when(subjectDao.getMarkersBySubjectId(88L)).thenReturn(null);

        List<UserResponseVO> result = service.getMarkers(null, 88L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getProjectsBySubjectId_returnsCountsForAdmin() {
        ProjectPO project = project(11L, 20L, "individual");
        when(projectDao.getProjectsBySubjectId(20L)).thenReturn(List.of(project));
        when(projectDao.countMarkedStudentsByProjectId(11L, 99L)).thenReturn(2);
        when(projectDao.countUnmarkedStudentsByProjectId(11L, 99L)).thenReturn(5);

        List<ProjectResponseVO> result = service.getProjectsBySubjectId(20L, 99L);

        assertEquals(1, result.size());
        ProjectResponseVO vo = result.get(0);
        assertEquals(11L, vo.getId());
        assertEquals("Project 11", vo.getName());
        assertEquals("individual", vo.getProjectType());
        assertEquals(2, vo.getMarkedCount());
        assertEquals(5, vo.getUnmarkedCount());
    }

    @Test
    void getProjectsBySubjectIdAndMarkerId_returnsCountsForMarker() {
        ProjectPO project = project(12L, 20L, "group");
        when(projectDao.getProjectsBySubjectIdAndMarkerId(20L, 88L)).thenReturn(List.of(project));
        when(projectDao.countMarkedGroupsByProjectIdAndMarker(12L, 88L)).thenReturn(1);
        when(projectDao.countUnmarkedGroupsByProjectIdAndMarker(12L, 88L)).thenReturn(4);

        List<ProjectResponseVO> result = service.getProjectsBySubjectIdAndMarkerId(20L, 88L);

        assertEquals(1, result.size());
        ProjectResponseVO vo = result.get(0);
        assertEquals(12L, vo.getId());
        assertEquals("Project 12", vo.getName());
        assertEquals("group", vo.getProjectType());
        assertEquals(1, vo.getMarkedCount());
        assertEquals(4, vo.getUnmarkedCount());
    }

    private ProjectPO project(Long id, Long subjectId, String projectType) {
        return ProjectPO.builder()
                .id(id)
                .name("Project " + id)
                .subjectId(subjectId)
                .projectType(projectType)
                .countdown(30L)
                .templateId(0L)
                .build();
    }

    private UserResponseVO userResponse(Long userId, Integer role, String userName) {
        UserResponseVO vo = new UserResponseVO();
        vo.setUserId(userId);
        vo.setRole(role);
        vo.setUserName(userName);
        return vo;
    }
}
