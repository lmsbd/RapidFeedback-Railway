package com.unimelb.swen90017.rfo.service.impl;

import com.unimelb.swen90017.rfo.dao.GroupMarkRecordDao;
import com.unimelb.swen90017.rfo.dao.MarkDetailDao;
import com.unimelb.swen90017.rfo.dao.MarkRecordDao;
import com.unimelb.swen90017.rfo.dao.ProjectDao;
import com.unimelb.swen90017.rfo.pojo.po.GroupMarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.MarkDetailPO;
import com.unimelb.swen90017.rfo.pojo.po.MarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.ProjectPO;
import com.unimelb.swen90017.rfo.pojo.vo.ImportResultVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportServiceImplTest {

    @Mock
    private ProjectDao projectDao;
    @Mock
    private MarkRecordDao markRecordDao;
    @Mock
    private MarkDetailDao markDetailDao;
    @Mock
    private GroupMarkRecordDao groupMarkRecordDao;

    private ExcelImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExcelImportServiceImpl();
        ReflectionTestUtils.setField(service, "projectDao", projectDao);
        ReflectionTestUtils.setField(service, "markRecordDao", markRecordDao);
        ReflectionTestUtils.setField(service, "markDetailDao", markDetailDao);
        ReflectionTestUtils.setField(service, "groupMarkRecordDao", groupMarkRecordDao);
    }

    @Test
    void importIndividualProject_updatesMarkRecordAndDetails() throws Exception {
        ProjectPO project = project(1L);
        MarkRecordPO record = MarkRecordPO.builder()
                .id(11L)
                .projectId(1L)
                .studentId(1001L)
                .markerId(501L)
                .build();
        MarkDetailPO detail = MarkDetailPO.builder()
                .id(900L)
                .markRecordId(11L)
                .criteriaId(101L)
                .build();

        when(projectDao.selectById(1L)).thenReturn(project);
        when(markRecordDao.selectById(11L)).thenReturn(record);
        when(markDetailDao.selectById(900L)).thenReturn(detail);

        ImportResultVO result = service.importIndividualProject(1L, multipartFile(individualWorkbookBytes()));

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessRows());
        assertEquals(0, result.getFailedRows());
        assertTrue(result.getFailures().isEmpty());
        assertEquals(new BigDecimal("8.5"), record.getTotalScore());
        assertEquals(new BigDecimal("7.25"), detail.getScore());
        assertEquals("Great", detail.getComment());
        verify(markRecordDao).updateById(record);
        verify(markDetailDao).updateById(detail);
    }

    @Test
    void importGroupProject_updatesGroupAndDetailRows() throws Exception {
        ProjectPO project = project(2L);
        MarkRecordPO record = MarkRecordPO.builder()
                .id(22L)
                .projectId(2L)
                .studentId(1002L)
                .markerId(601L)
                .build();
        MarkDetailPO detail = MarkDetailPO.builder()
                .id(901L)
                .markRecordId(22L)
                .criteriaId(202L)
                .build();
        GroupMarkRecordPO gmr = GroupMarkRecordPO.builder()
                .id(700L)
                .projectId(2L)
                .groupId(300L)
                .markerId(601L)
                .build();

        when(projectDao.selectById(2L)).thenReturn(project);
        when(markRecordDao.selectById(22L)).thenReturn(record);
        when(markDetailDao.selectById(901L)).thenReturn(detail);
        when(groupMarkRecordDao.selectById(700L)).thenReturn(gmr);

        ImportResultVO result = service.importGroupProject(2L, multipartFile(groupWorkbookBytes()));

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessRows());
        assertEquals(0, result.getFailedRows());
        assertTrue(result.getFailures().isEmpty());
        assertEquals(new BigDecimal("8.8"), record.getTotalScore());
        assertEquals(new BigDecimal("7.9"), record.getGroupScore());
        assertEquals(new BigDecimal("7.0"), detail.getScore());
        assertEquals("Well done", detail.getComment());
        assertEquals("Updated group comment", gmr.getComment());
        verify(markRecordDao).updateById(record);
        verify(markDetailDao).updateById(detail);
        verify(groupMarkRecordDao).updateById(gmr);
    }

    private ProjectPO project(Long id) {
        return ProjectPO.builder()
                .id(id)
                .subjectId(10L)
                .templateId(1L)
                .name("Project " + id)
                .projectType(id == 1L ? "individual" : "group")
                .build();
    }

    private byte[] individualWorkbookBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("individual");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "Marker_name", "Subject_name", "Project_name", "Student_id", "Student_name",
                    "Criteria_1_weighting", "Criteria_1_max_score", "Criteria_1_id", "Criteria_1_score", "Criteria_1_comment",
                    "total_score", "Marker_id", "Subject_id", "Project_id", "Mark_record_id"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("marker-501");
            row.createCell(1).setCellValue("Subject");
            row.createCell(2).setCellValue("Project");
            row.createCell(3).setCellValue(2001);
            row.createCell(4).setCellValue("Alice Lee");
            row.createCell(5).setCellValue(60);
            row.createCell(6).setCellValue(10);
            row.createCell(7).setCellValue(900);
            row.createCell(8).setCellValue(7.25);
            row.createCell(9).setCellValue("Great");
            row.createCell(10).setCellValue(8.5);
            row.createCell(11).setCellValue(501);
            row.createCell(12).setCellValue(10);
            row.createCell(13).setCellValue(1);
            row.createCell(14).setCellValue(11);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] groupWorkbookBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Team A");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "Marker_name", "Subject_name", "Project_name", "Student_id", "Student_name",
                    "Criteria_1_weighting", "Criteria_1_max_score", "Criteria_1_id", "Criteria_1_score", "Criteria_1_comment",
                    "total_score", "Group_score", "Group_comment", "group_id", "Marker_id", "Subject_id", "Project_id",
                    "Mark_record_id", "group_mark_record_id"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("marker-601");
            row.createCell(1).setCellValue("Subject");
            row.createCell(2).setCellValue("Project");
            row.createCell(3).setCellValue(2101);
            row.createCell(4).setCellValue("Bob Tan");
            row.createCell(5).setCellValue(40);
            row.createCell(6).setCellValue(20);
            row.createCell(7).setCellValue(901);
            row.createCell(8).setCellValue(7.0);
            row.createCell(9).setCellValue("Well done");
            row.createCell(10).setCellValue(8.8);
            row.createCell(11).setCellValue(7.9);
            row.createCell(12).setCellValue("Updated group comment");
            row.createCell(13).setCellValue(300);
            row.createCell(14).setCellValue(601);
            row.createCell(15).setCellValue(10);
            row.createCell(16).setCellValue(2);
            row.createCell(17).setCellValue(22);
            row.createCell(18).setCellValue(700);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile multipartFile(byte[] bytes) {
        return new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }
}
