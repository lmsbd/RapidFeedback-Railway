package com.unimelb.swen90017.rfo.service.impl;

import com.unimelb.swen90017.rfo.dao.GroupMarkRecordDao;
import com.unimelb.swen90017.rfo.dao.MarkDetailDao;
import com.unimelb.swen90017.rfo.dao.MarkRecordDao;
import com.unimelb.swen90017.rfo.dao.ProjectDao;
import com.unimelb.swen90017.rfo.dao.StudentDao;
import com.unimelb.swen90017.rfo.dao.SubjectDao;
import com.unimelb.swen90017.rfo.dao.UserDao;
import com.unimelb.swen90017.rfo.pojo.po.GroupMarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.MarkDetailPO;
import com.unimelb.swen90017.rfo.pojo.po.MarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.ProjectGroupPO;
import com.unimelb.swen90017.rfo.pojo.po.ProjectPO;
import com.unimelb.swen90017.rfo.pojo.po.StudentPO;
import com.unimelb.swen90017.rfo.pojo.po.SubjectPO;
import com.unimelb.swen90017.rfo.pojo.po.UserPO;
import com.unimelb.swen90017.rfo.pojo.vo.AssessmentVO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceImplTest {

    @Mock
    private ProjectDao projectDao;
    @Mock
    private SubjectDao subjectDao;
    @Mock
    private MarkRecordDao markRecordDao;
    @Mock
    private MarkDetailDao markDetailDao;
    @Mock
    private StudentDao studentDao;
    @Mock
    private UserDao userDao;
    @Mock
    private GroupMarkRecordDao groupMarkRecordDao;

    private ExcelExportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExcelExportServiceImpl();
        ReflectionTestUtils.setField(service, "projectDao", projectDao);
        ReflectionTestUtils.setField(service, "subjectDao", subjectDao);
        ReflectionTestUtils.setField(service, "markRecordDao", markRecordDao);
        ReflectionTestUtils.setField(service, "markDetailDao", markDetailDao);
        ReflectionTestUtils.setField(service, "studentDao", studentDao);
        ReflectionTestUtils.setField(service, "userDao", userDao);
        ReflectionTestUtils.setField(service, "groupMarkRecordDao", groupMarkRecordDao);
    }

    @Test
    void exportIndividualProject_generatesExpectedWorkbook() throws Exception {
        ProjectPO project = project(1L, 2L, 3L, "individual", "Project 1");
        SubjectPO subject = subject(2L, "Subject 2");
        AssessmentVO criterion = criterion(101L, "Criteria 1", 60, 10, 0.5);
        MarkRecordPO record = MarkRecordPO.builder()
                .id(11L)
                .projectId(1L)
                .studentId(1001L)
                .markerId(501L)
                .totalScore(new BigDecimal("9.50"))
                .build();
        MarkDetailPO detail = MarkDetailPO.builder()
                .id(900L)
                .markRecordId(11L)
                .criteriaId(101L)
                .score(new BigDecimal("8.50"))
                .comment("Nice")
                .build();
        StudentPO student = StudentPO.builder()
                .id(1001L)
                .studentId(2001L)
                .firstName("Alice")
                .surname("Lee")
                .build();
        UserPO marker = user(501L, "marker-501");

        when(projectDao.selectById(1L)).thenReturn(project);
        when(subjectDao.selectById(2L)).thenReturn(subject);
        when(projectDao.getAssessmentByTemplateId(3L)).thenReturn(List.of(criterion));
        when(markRecordDao.getByProjectId(1L)).thenReturn(List.of(record));
        when(markDetailDao.getByMarkRecordId(11L)).thenReturn(List.of(detail));
        when(studentDao.selectById(1001L)).thenReturn(student);
        when(userDao.selectById(501L)).thenReturn(marker);

        byte[] bytes = service.exportIndividualProject(1L);
        assertTrue(bytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("individual");
            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("individual", sheet.getSheetName());

            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            assertEquals("Marker_name", formatter.formatCellValue(header.getCell(0)));
            assertEquals("Student_name", formatter.formatCellValue(header.getCell(4)));
            assertEquals("Criteria_1_weighting", formatter.formatCellValue(header.getCell(5)));
            assertEquals("total_score", formatter.formatCellValue(header.getCell(10)));

            Row row = sheet.getRow(1);
            assertEquals("marker-501", formatter.formatCellValue(row.getCell(0)));
            assertEquals("Subject 2", formatter.formatCellValue(row.getCell(1)));
            assertEquals("Project 1", formatter.formatCellValue(row.getCell(2)));
            assertEquals("2001", formatter.formatCellValue(row.getCell(3)));
            assertEquals("Alice Lee", formatter.formatCellValue(row.getCell(4)));
            assertEquals("60", formatter.formatCellValue(row.getCell(5)));
            assertEquals("10", formatter.formatCellValue(row.getCell(6)));
            assertEquals("900", formatter.formatCellValue(row.getCell(7)));
            assertEquals("8.5", formatter.formatCellValue(row.getCell(8)));
            assertEquals("Nice", formatter.formatCellValue(row.getCell(9)));
            assertEquals("9.5", formatter.formatCellValue(row.getCell(10)));
        }
    }

    @Test
    void exportGroupProject_generatesExpectedWorksheetPerGroup() throws Exception {
        ProjectPO project = project(2L, 20L, 4L, "group", "Project 2");
        SubjectPO subject = subject(20L, "Subject 20");
        AssessmentVO criterion = criterion(202L, "Criteria 2", 40, 20, 1.0);
        ProjectGroupPO group = ProjectGroupPO.builder().id(300L).projectId(2L).groupName("Team A").build();
        MarkRecordPO record = MarkRecordPO.builder()
                .id(22L)
                .projectId(2L)
                .studentId(4001L)
                .markerId(601L)
                .totalScore(new BigDecimal("7.25"))
                .groupScore(new BigDecimal("6.75"))
                .build();
        MarkDetailPO detail = MarkDetailPO.builder()
                .id(901L)
                .markRecordId(22L)
                .criteriaId(202L)
                .score(new BigDecimal("7.75"))
                .comment("Good")
                .build();
        GroupMarkRecordPO gmr = GroupMarkRecordPO.builder()
                .id(700L)
                .projectId(2L)
                .groupId(300L)
                .markerId(601L)
                .comment("Group comment")
                .build();
        StudentPO student = StudentPO.builder()
                .id(4001L)
                .studentId(2101L)
                .firstName("Bob")
                .surname("Tan")
                .build();
        UserPO marker = user(601L, "marker-601");

        when(projectDao.selectById(2L)).thenReturn(project);
        when(subjectDao.selectById(20L)).thenReturn(subject);
        when(projectDao.getAssessmentByTemplateId(4L)).thenReturn(List.of(criterion));
        when(projectDao.getProjectGroupByProjectId(2L)).thenReturn(List.of(group));
        when(markRecordDao.getByProjectAndGroup(2L, 300L)).thenReturn(List.of(record));
        when(markDetailDao.getByMarkRecordId(22L)).thenReturn(List.of(detail));
        when(groupMarkRecordDao.getAllByProjectAndGroup(2L, 300L)).thenReturn(List.of(gmr));
        when(studentDao.selectById(4001L)).thenReturn(student);
        when(userDao.selectById(601L)).thenReturn(marker);

        byte[] bytes = service.exportGroupProject(2L);
        assertTrue(bytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Team A");
            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("Team A", sheet.getSheetName());

            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            assertEquals("Group_comment", formatter.formatCellValue(header.getCell(12)));
            assertEquals("Mark_record_id", formatter.formatCellValue(header.getCell(17)));
            assertEquals("group_mark_record_id", formatter.formatCellValue(header.getCell(18)));

            Row row = sheet.getRow(1);
            assertEquals("marker-601", formatter.formatCellValue(row.getCell(0)));
            assertEquals("Project 2", formatter.formatCellValue(row.getCell(2)));
            assertEquals("2101", formatter.formatCellValue(row.getCell(3)));
            assertEquals("Bob Tan", formatter.formatCellValue(row.getCell(4)));
            assertEquals("7.75", formatter.formatCellValue(row.getCell(8)));
            assertEquals("Good", formatter.formatCellValue(row.getCell(9)));
            assertEquals("7.25", formatter.formatCellValue(row.getCell(10)));
            assertEquals("6.75", formatter.formatCellValue(row.getCell(11)));
            assertEquals("Group comment", formatter.formatCellValue(row.getCell(12)));
            assertEquals("300", formatter.formatCellValue(row.getCell(13)));
            assertEquals("601", formatter.formatCellValue(row.getCell(14)));
            assertEquals("20", formatter.formatCellValue(row.getCell(15)));
            assertEquals("2", formatter.formatCellValue(row.getCell(16)));
            assertEquals("22", formatter.formatCellValue(row.getCell(17)));
            assertEquals("700", formatter.formatCellValue(row.getCell(18)));
        }
    }

    private ProjectPO project(Long id, Long subjectId, Long templateId, String type, String name) {
        return ProjectPO.builder()
                .id(id)
                .subjectId(subjectId)
                .templateId(templateId)
                .projectType(type)
                .name(name)
                .countdown(30L)
                .build();
    }

    private SubjectPO subject(Long id, String name) {
        return SubjectPO.builder()
                .id(id)
                .name(name)
                .description("desc")
                .build();
    }

    private AssessmentVO criterion(Long criteriaId, String name, Integer weighting, Integer maxMark, Double increments) {
        AssessmentVO vo = new AssessmentVO();
        vo.setCriteriaId(criteriaId);
        vo.setName(name);
        vo.setWeighting(weighting);
        vo.setMaxMark(maxMark);
        vo.setMarkIncrements(increments);
        return vo;
    }

    private UserPO user(Long id, String username) {
        return UserPO.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .build();
    }
}
