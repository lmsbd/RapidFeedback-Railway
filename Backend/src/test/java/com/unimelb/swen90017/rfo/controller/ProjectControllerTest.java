package com.unimelb.swen90017.rfo.controller;

import com.unimelb.swen90017.rfo.common.BusinessException;
import com.unimelb.swen90017.rfo.common.Result;
import com.unimelb.swen90017.rfo.pojo.constants.BaseConstants;
import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import com.unimelb.swen90017.rfo.pojo.vo.ProjectResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.SendReportResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.StudentAssessmentScoresResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.UserResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SendReportRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.StudentAssessmentScoresRequestVO;
import com.unimelb.swen90017.rfo.security.CustomUserDetails;
import com.unimelb.swen90017.rfo.service.CommentService;
import com.unimelb.swen90017.rfo.service.ProjectService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private CommentService commentService;

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "projectService", projectService);
        ReflectionTestUtils.setField(controller, "commentService", commentService);
    }

    @Test
    void hasMarkingStarted_delegatesToService() {
        when(projectService.hasMarkingStarted(11L)).thenReturn(true);

        Result<Boolean> result = controller.hasMarkingStarted(11L);

        assertTrue(result.getData());
        verify(projectService).hasMarkingStarted(11L);
    }

    @Test
    void getMarkers_validatesAndDelegatesSubjectLookup() {
        UserResponseVO marker = new UserResponseVO();
        marker.setUserId(7L);
        marker.setRole(BaseConstants.USER_ROLE_MARKER);
        marker.setUserName("marker-7");
        when(projectService.getMarkers(null, 22L)).thenReturn(List.of(marker));

        Result<List<UserResponseVO>> result = controller.getMarkers(null, 22L);

        assertEquals(1, result.getData().size());
        assertEquals(marker, result.getData().get(0));
        verify(projectService).getMarkers(null, 22L);
    }

    @Test
    void getMarkers_throwsWhenBothInputsMissing() {
        BusinessException exception = assertThrows(BusinessException.class, () -> controller.getMarkers(null, null));

        assertEquals(400, exception.getCode());
        assertEquals("Either projectId or subjectId is required", exception.getMessage());
    }

    @Test
    void getStudentAssessmentScores_usesAuthenticatedMarkerId() {
        StudentAssessmentScoresRequestVO requestVO = new StudentAssessmentScoresRequestVO();
        requestVO.setProjectId(41L);
        requestVO.setStudentId(88L);
        StudentAssessmentScoresResponseVO responseVO = new StudentAssessmentScoresResponseVO();
        responseVO.setProjectId(41L);
        responseVO.setProjectName("Project 41");
        responseVO.setProjectType("individual");
        when(projectService.getStudentAssessmentScores(41L, 88L, 123L)).thenReturn(responseVO);

        Result<StudentAssessmentScoresResponseVO> result = controller.getStudentAssessmentScores(
                requestVO,
                customUserDetails(123L, BaseConstants.USER_ROLE_MARKER));

        assertEquals(responseVO, result.getData());
        verify(projectService).getStudentAssessmentScores(41L, 88L, 123L);
    }

    @Test
    void getProjects_routesAdminToSubjectLookup() {
        ProjectResponseVO project = project(51L, "Project 51");
        when(projectService.getProjectsBySubjectId(99L, 8L)).thenReturn(List.of(project));

        Result<List<ProjectResponseVO>> result = controller.getProjectList("99", customUserDetails(8L, BaseConstants.USER_ROLE_ADMIN));

        assertEquals(1, result.getData().size());
        assertEquals(project, result.getData().get(0));
        verify(projectService).getProjectsBySubjectId(99L, 8L);
        verify(projectService, never()).getProjectsBySubjectIdAndMarkerId(99L, 8L);
    }

    @Test
    void getProjects_routesMarkerToMarkerLookup() {
        ProjectResponseVO project = project(52L, "Project 52");
        when(projectService.getProjectsBySubjectIdAndMarkerId(99L, 8L)).thenReturn(List.of(project));

        Result<List<ProjectResponseVO>> result = controller.getProjectList("99", customUserDetails(8L, BaseConstants.USER_ROLE_MARKER));

        assertEquals(1, result.getData().size());
        assertEquals(project, result.getData().get(0));
        verify(projectService).getProjectsBySubjectIdAndMarkerId(99L, 8L);
        verify(projectService, never()).getProjectsBySubjectId(99L, 8L);
    }

    @Test
    void sendReport_wrapsServiceResponse() {
        SendReportRequestVO requestVO = new SendReportRequestVO();
        requestVO.setProjectId(77L);
        SendReportResponseVO responseVO = SendReportResponseVO.builder()
                .projectName("Project 77")
                .totalStudents(12)
                .build();
        when(projectService.sendReport(77L)).thenReturn(responseVO);

        Result<SendReportResponseVO> result = controller.sendReport(requestVO);

        assertEquals("Sending reports to 12 students", result.getMessage());
        assertEquals(responseVO, result.getData());
        verify(projectService).sendReport(77L);
    }

    @Test
    void sendReport_throwsWhenProjectIdMissing() {
        SendReportRequestVO requestVO = new SendReportRequestVO();

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.sendReport(requestVO));

        assertEquals(400, exception.getCode());
        assertEquals("Project ID is required", exception.getMessage());
    }

    private CustomUserDetails customUserDetails(Long userId, Integer role) {
        UserPO user = UserPO.builder()
                .id(userId)
                .username("user-" + userId)
                .email("user-" + userId + "@example.com")
                .password("password")
                .role(role)
                .deleteStatus(BaseConstants.DELETE_STATUS_NOT_DELETED)
                .build();
        return new CustomUserDetails(user);
    }

    private ProjectResponseVO project(Long id, String name) {
        ProjectResponseVO vo = new ProjectResponseVO();
        vo.setId(id);
        vo.setName(name);
        vo.setProjectType("individual");
        vo.setMarkedCount(1);
        vo.setUnmarkedCount(2);
        return vo;
    }
}
