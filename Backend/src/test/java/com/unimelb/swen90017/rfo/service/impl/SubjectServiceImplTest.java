package com.unimelb.swen90017.rfo.service.impl;

import com.unimelb.swen90017.rfo.dao.StudentDao;
import com.unimelb.swen90017.rfo.dao.StudentProjectDao;
import com.unimelb.swen90017.rfo.dao.StudentSubjectDao;
import com.unimelb.swen90017.rfo.dao.SubjectDao;
import com.unimelb.swen90017.rfo.dao.UserDao;
import com.unimelb.swen90017.rfo.dao.UserProjectDao;
import com.unimelb.swen90017.rfo.dao.UserSubjectDao;
import com.unimelb.swen90017.rfo.pojo.constants.BaseConstants;
import com.unimelb.swen90017.rfo.pojo.po.StudentPO;
import com.unimelb.swen90017.rfo.pojo.po.SubjectPO;
import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import com.unimelb.swen90017.rfo.pojo.vo.StudentResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SubjectRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceImplTest {

    @Mock
    private SubjectDao subjectDao;

    @Mock
    private StudentDao studentDao;

    @Mock
    private StudentSubjectDao studentSubjectDao;

    @Mock
    private UserSubjectDao userSubjectDao;

    @Mock
    private StudentProjectDao studentProjectDao;

    @Mock
    private UserProjectDao userProjectDao;

    @Mock
    private UserDao userDao;

    private SubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubjectServiceImpl();
        ReflectionTestUtils.setField(service, "subjectDao", subjectDao);
        ReflectionTestUtils.setField(service, "studentDao", studentDao);
        ReflectionTestUtils.setField(service, "studentSubjectDao", studentSubjectDao);
        ReflectionTestUtils.setField(service, "userSubjectDao", userSubjectDao);
        ReflectionTestUtils.setField(service, "studentProjectDao", studentProjectDao);
        ReflectionTestUtils.setField(service, "userProjectDao", userProjectDao);
        ReflectionTestUtils.setField(service, "userDao", userDao);
    }

    @Test
    void updateSubjectsDetail_rewritesStudentsAndMarkersByBusinessStudentNumber() throws Exception {
        SubjectRequestVO requestVO = subjectRequest(
                9L,
                "Updated subject",
                "Updated description",
                List.of(student(2002L, "existing@example.com", "Existing", "Student"),
                        student(2003L, "new@example.com", "New", "Student")),
                List.of(52L, 53L)
        );

        SubjectPO subjectPO = SubjectPO.builder()
                .id(9L)
                .name("Original subject")
                .description("Original description")
                .build();

        StudentPO existingStudent = StudentPO.builder()
                .id(301L)
                .studentId(2002L)
                .email("existing@example.com")
                .firstName("Existing")
                .surname("Student")
                .deleteStatus("0")
                .build();

        when(subjectDao.selectById(9L)).thenReturn(subjectPO);
        when(studentSubjectDao.selectStudentIdsBySubjectId(9L)).thenReturn(List.of(301L));
        when(studentDao.selectById(301L)).thenReturn(existingStudent);
        when(studentDao.findByStudentId(2003L)).thenReturn(null);
        doAnswer(invocation -> {
            StudentPO inserted = invocation.getArgument(0);
            inserted.setId(401L);
            return 1;
        }).when(studentDao).insert(any(StudentPO.class));
        when(userSubjectDao.selectUserIdsBySubjectId(9L)).thenReturn(List.of(51L, 52L));
        when(userDao.selectById(51L)).thenReturn(user(51L, BaseConstants.USER_ROLE_MARKER, "marker-51"));
        when(userDao.selectById(52L)).thenReturn(user(52L, BaseConstants.USER_ROLE_MARKER, "marker-52"));
        when(userDao.selectById(53L)).thenReturn(user(53L, BaseConstants.USER_ROLE_MARKER, "marker-53"));
        when(userProjectDao.countBySubjectIdAndUserId(9L, 51L)).thenReturn(0);
        when(subjectDao.updateById(any(SubjectPO.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.updateSubjectsDetail(requestVO, 100L));

        verify(subjectDao).updateById(subjectPO);
        assertEquals("Updated subject", subjectPO.getName());
        assertEquals("Updated description", subjectPO.getDescription());
        verify(studentDao).insert(any(StudentPO.class));
        verify(studentSubjectDao).insertOne(401L, 9L);
        verify(studentSubjectDao, never()).deleteOne(301L, 9L);
        verify(userSubjectDao).insertOne(53L, 9L);
        verify(userSubjectDao).deleteOne(51L, 9L);
    }

    @Test
    void updateSubjectsDetail_throwsWhenRemovedStudentIsAlreadyAssignedToProject() {
        SubjectRequestVO requestVO = subjectRequest(
                9L,
                "Updated subject",
                "Updated description",
                List.of(),
                List.of(51L)
        );

        SubjectPO subjectPO = SubjectPO.builder()
                .id(9L)
                .name("Original subject")
                .description("Original description")
                .build();

        StudentPO existingStudent = StudentPO.builder()
                .id(301L)
                .studentId(2002L)
                .email("existing@example.com")
                .firstName("Existing")
                .surname("Student")
                .deleteStatus("0")
                .build();

        when(subjectDao.selectById(9L)).thenReturn(subjectPO);
        when(studentSubjectDao.selectStudentIdsBySubjectId(9L)).thenReturn(List.of(301L));
        when(studentDao.selectById(301L)).thenReturn(existingStudent);
        when(studentProjectDao.countBySubjectIdAndStudentId(9L, 301L)).thenReturn(1);
        when(userSubjectDao.selectUserIdsBySubjectId(9L)).thenReturn(List.of(51L));
        when(userDao.selectById(51L)).thenReturn(user(51L, BaseConstants.USER_ROLE_MARKER, "marker-51"));

        Exception exception = assertThrows(Exception.class,
                () -> service.updateSubjectsDetail(requestVO, 100L));

        assertEquals("Student 2002 is already assigned to a project under this subject and cannot be removed",
                exception.getMessage());
        verify(subjectDao, never()).updateById(any(SubjectPO.class));
        verify(studentSubjectDao, never()).deleteOne(any(), any());
    }

    @Test
    void updateSubjectsDetail_throwsWhenAddedUserIsNotAMarker() {
        SubjectRequestVO requestVO = subjectRequest(
                9L,
                "Updated subject",
                "Updated description",
                List.of(student(2002L, "existing@example.com", "Existing", "Student")),
                List.of(52L)
        );

        SubjectPO subjectPO = SubjectPO.builder()
                .id(9L)
                .name("Original subject")
                .description("Original description")
                .build();

        StudentPO existingStudent = StudentPO.builder()
                .id(301L)
                .studentId(2002L)
                .email("existing@example.com")
                .firstName("Existing")
                .surname("Student")
                .deleteStatus("0")
                .build();

        when(subjectDao.selectById(9L)).thenReturn(subjectPO);
        when(studentSubjectDao.selectStudentIdsBySubjectId(9L)).thenReturn(List.of(301L));
        when(studentDao.selectById(301L)).thenReturn(existingStudent);
        when(userSubjectDao.selectUserIdsBySubjectId(9L)).thenReturn(List.of(51L));
        when(userDao.selectById(51L)).thenReturn(user(51L, BaseConstants.USER_ROLE_MARKER, "marker-51"));
        when(userDao.selectById(52L)).thenReturn(user(52L, BaseConstants.USER_ROLE_ADMIN, "admin-52"));

        Exception exception = assertThrows(Exception.class,
                () -> service.updateSubjectsDetail(requestVO, 100L));

        assertEquals("User 52 is not a marker", exception.getMessage());
        verify(subjectDao, never()).updateById(any(SubjectPO.class));
        verify(userSubjectDao, never()).insertOne(52L, 9L);
    }

    private SubjectRequestVO subjectRequest(Long id, String name, String description,
                                            List<StudentResponseVO> students, List<Long> markerIds) {
        SubjectRequestVO requestVO = new SubjectRequestVO();
        requestVO.setId(id);
        requestVO.setName(name);
        requestVO.setDescription(description);
        requestVO.setStudents(students);
        requestVO.setMarkerIds(markerIds);
        return requestVO;
    }

    private StudentResponseVO student(Long studentId, String email, String firstName, String surname) {
        StudentResponseVO vo = new StudentResponseVO();
        vo.setStudentId(studentId);
        vo.setEmail(email);
        vo.setFirstName(firstName);
        vo.setSurname(surname);
        return vo;
    }

    private UserPO user(Long id, Integer role, String username) {
        return UserPO.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .password("password")
                .role(role)
                .deleteStatus(BaseConstants.DELETE_STATUS_NOT_DELETED)
                .build();
    }
}
