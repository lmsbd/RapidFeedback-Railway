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
import com.unimelb.swen90017.rfo.service.ExcelExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel export service implementation
 */
@Slf4j
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private SubjectDao subjectDao;

    @Autowired
    private MarkRecordDao markRecordDao;

    @Autowired
    private MarkDetailDao markDetailDao;

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupMarkRecordDao groupMarkRecordDao;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public byte[] exportIndividualProject(Long projectId) {
        ProjectPO project = projectDao.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        SubjectPO subject = subjectDao.selectById(project.getSubjectId());
        List<AssessmentVO> criteria = projectDao.getAssessmentByTemplateId(project.getTemplateId());
        List<MarkRecordPO> markRecords = markRecordDao.getByProjectId(projectId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("individual");

            CellStyle headerStyle    = buildHeaderStyle(workbook);
            CellStyle lockedStyle    = buildLockedDataStyle(workbook);
            CellStyle editableStyle  = buildEditableDataStyle(workbook);

            writeHeaderRow(sheet, headerStyle, criteria);
            writeDataRows(sheet, lockedStyle, editableStyle, project, subject, criteria, markRecords);

            autoSizeColumns(sheet, criteria);

            // Enable sheet protection: locked cells become read-only.
            // Editable cells (score / comment / total_score) use a style with setLocked(false).
            sheet.protectSheet("");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate individual Excel export", e);
        }
    }

    @Override
    public byte[] exportGroupProject(Long projectId) {
        ProjectPO project = projectDao.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        SubjectPO subject = subjectDao.selectById(project.getSubjectId());
        List<AssessmentVO> criteria = projectDao.getAssessmentByTemplateId(project.getTemplateId());
        List<ProjectGroupPO> groups = projectDao.getProjectGroupByProjectId(projectId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle   = buildHeaderStyle(workbook);
            CellStyle lockedStyle   = buildLockedDataStyle(workbook);
            CellStyle editableStyle = buildEditableDataStyle(workbook);

            for (ProjectGroupPO group : groups) {
                Sheet sheet = workbook.createSheet(sanitizeSheetName(group.getGroupName()));
                List<MarkRecordPO> markRecords =
                        markRecordDao.getByProjectAndGroup(projectId, group.getId());

                writeGroupHeaderRow(sheet, headerStyle, criteria);
                writeGroupDataRows(sheet, lockedStyle, editableStyle,
                        project, subject, criteria, markRecords, group);
                autoSizeGroupColumns(sheet, criteria);
                sheet.protectSheet("");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate group Excel export", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Write the header row.
     *
     * Display columns  : Marker_name | Subject_name | Project_name | Student_id | Student_name
     * Dynamic criteria : {name}_weighting | {name}_max_score | {name}_id | {name}_score | {name}_comment  (per criteria)
     * Trailing editable: total_score
     * System ID columns: Marker_id | Subject_id | Project_id | Mark_record_id
     */
    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle, List<AssessmentVO> criteria) {
        Row header = sheet.createRow(0);
        int col = 0;

        // Display columns
        col = writeCell(header, col, "Marker_name", headerStyle);
        col = writeCell(header, col, "Subject_name", headerStyle);
        col = writeCell(header, col, "Project_name", headerStyle);
        col = writeCell(header, col, "Student_id", headerStyle);
        col = writeCell(header, col, "Student_name", headerStyle);

        // Dynamic criteria: weighting | max_score | id | score | comment
        for (AssessmentVO c : criteria) {
            String baseName = sanitizeName(c.getName());
            col = writeCell(header, col, baseName + "_weighting", headerStyle);
            col = writeCell(header, col, baseName + "_max_score", headerStyle);
            col = writeCell(header, col, baseName + "_id", headerStyle);
            col = writeCell(header, col, baseName + "_score", headerStyle);
            col = writeCell(header, col, baseName + "_comment", headerStyle);
        }

        // Trailing editable
        col = writeCell(header, col, "total_score", headerStyle);

        // System ID columns (criteria_id already in criteria block above)
        col = writeCell(header, col, "Marker_id", headerStyle);
        col = writeCell(header, col, "Subject_id", headerStyle);
        col = writeCell(header, col, "Project_id", headerStyle);
        writeCell(header, col, "Mark_record_id", headerStyle);
    }

    /**
     * Write one data row per mark_record.
     * Column order mirrors writeHeaderRow:
     *   display info → criteria score/comment → total_score → system IDs
     */
    private void writeDataRows(Sheet sheet, CellStyle lockedStyle, CellStyle editableStyle,
                                ProjectPO project, SubjectPO subject,
                                List<AssessmentVO> criteria,
                                List<MarkRecordPO> markRecords) {
        int rowIndex = 1;
        for (MarkRecordPO record : markRecords) {
            StudentPO student = studentDao.selectById(record.getStudentId());
            UserPO marker = userDao.selectById(record.getMarkerId());

            List<MarkDetailPO> details = markDetailDao.getByMarkRecordId(record.getId());
            Map<Long, MarkDetailPO> detailByCriteriaId = details.stream()
                    .collect(Collectors.toMap(MarkDetailPO::getCriteriaId, d -> d, (a, b) -> a));

            Row row = sheet.createRow(rowIndex++);
            int col = 0;

            // Display columns — read-only
            col = writeCell(row, col, marker != null ? marker.getUsername() : "", lockedStyle);
            col = writeCell(row, col, subject != null ? subject.getName() : "", lockedStyle);
            col = writeCell(row, col, project.getName(), lockedStyle);
            col = writeCell(row, col, student != null ? student.getStudentId() : null, lockedStyle);
            col = writeCell(row, col,
                    student != null ? student.getFirstName() + " " + student.getSurname() : "",
                    lockedStyle);

            // Dynamic criteria: weighting | max_score | id (locked) + score | comment (editable)
            for (AssessmentVO c : criteria) {
                MarkDetailPO detail = detailByCriteriaId.get(c.getCriteriaId());
                col = writeCell(row, col, c.getWeighting() != null ? (long) c.getWeighting() : null, lockedStyle);
                col = writeCell(row, col, c.getMaxMark() != null ? (long) c.getMaxMark() : null, lockedStyle);
                col = writeCell(row, col, detail != null ? detail.getId() : null, lockedStyle);
                col = writeCell(row, col, detail != null ? detail.getScore() : null, editableStyle);
                col = writeCell(row, col,
                        detail != null && detail.getComment() != null ? detail.getComment() : "",
                        editableStyle);
            }

            // total_score — editable
            col = writeCell(row, col, record.getTotalScore(), editableStyle);

            // System ID columns — read-only (criteria_id is in criteria block above)
            col = writeCell(row, col, marker != null ? marker.getId() : null, lockedStyle);
            col = writeCell(row, col, subject != null ? subject.getId() : null, lockedStyle);
            col = writeCell(row, col, project.getId(), lockedStyle);
            writeCell(row, col, record.getId(), lockedStyle);
        }
    }

    /**
     * Write the header row for a group-project sheet.
     *
     * Display columns  : Marker_name | Subject_name | Project_name | Student_id | Student_name
     * Dynamic criteria : {name}_weighting | {name}_max_score | {name}_score | {name}_comment
     * Trailing editable: total_score | Group_score
     * System ID columns: group_id | Marker_id | Subject_id | Project_id |
     *                    {name}_id (per criteria) | Mark_record_id
     */
    private void writeGroupHeaderRow(Sheet sheet, CellStyle headerStyle, List<AssessmentVO> criteria) {
        Row header = sheet.createRow(0);
        int col = 0;

        col = writeCell(header, col, "Marker_name", headerStyle);
        col = writeCell(header, col, "Subject_name", headerStyle);
        col = writeCell(header, col, "Project_name", headerStyle);
        col = writeCell(header, col, "Student_id", headerStyle);
        col = writeCell(header, col, "Student_name", headerStyle);

        // Dynamic criteria: weighting | max_score | id | score | comment
        for (AssessmentVO c : criteria) {
            String baseName = sanitizeName(c.getName());
            col = writeCell(header, col, baseName + "_weighting", headerStyle);
            col = writeCell(header, col, baseName + "_max_score", headerStyle);
            col = writeCell(header, col, baseName + "_id", headerStyle);
            col = writeCell(header, col, baseName + "_score", headerStyle);
            col = writeCell(header, col, baseName + "_comment", headerStyle);
        }

        col = writeCell(header, col, "total_score", headerStyle);
        col = writeCell(header, col, "Group_score", headerStyle);
        col = writeCell(header, col, "Group_comment", headerStyle);

        // System IDs (criteria_id already in criteria block above)
        col = writeCell(header, col, "group_id", headerStyle);
        col = writeCell(header, col, "Marker_id", headerStyle);
        col = writeCell(header, col, "Subject_id", headerStyle);
        col = writeCell(header, col, "Project_id", headerStyle);
        col = writeCell(header, col, "Mark_record_id", headerStyle);
        writeCell(header, col, "group_mark_record_id", headerStyle);
    }

    /**
     * Write one data row per mark_record for a group-project sheet.
     * group_comment and group_mark_record_id are fetched from group_mark_record
     * (one row per marker per group).  A map is built once per sheet to avoid N+1 queries.
     */
    private void writeGroupDataRows(Sheet sheet, CellStyle lockedStyle, CellStyle editableStyle,
                                     ProjectPO project, SubjectPO subject,
                                     List<AssessmentVO> criteria,
                                     List<MarkRecordPO> markRecords,
                                     ProjectGroupPO group) {
        // Pre-fetch all group_mark_records for this group, keyed by marker_id
        Map<Long, GroupMarkRecordPO> gmrByMarkerId =
                groupMarkRecordDao.getAllByProjectAndGroup(project.getId(), group.getId())
                        .stream()
                        .collect(Collectors.toMap(GroupMarkRecordPO::getMarkerId, g -> g, (a, b) -> a));

        int rowIndex = 1;
        for (MarkRecordPO record : markRecords) {
            StudentPO student = studentDao.selectById(record.getStudentId());
            UserPO marker = userDao.selectById(record.getMarkerId());

            List<MarkDetailPO> details = markDetailDao.getByMarkRecordId(record.getId());
            Map<Long, MarkDetailPO> detailByCriteriaId = details.stream()
                    .collect(Collectors.toMap(MarkDetailPO::getCriteriaId, d -> d, (a, b) -> a));

            GroupMarkRecordPO gmr = marker != null ? gmrByMarkerId.get(marker.getId()) : null;

            Row row = sheet.createRow(rowIndex++);
            int col = 0;

            // Display columns — read-only
            col = writeCell(row, col, marker != null ? marker.getUsername() : "", lockedStyle);
            col = writeCell(row, col, subject != null ? subject.getName() : "", lockedStyle);
            col = writeCell(row, col, project.getName(), lockedStyle);
            col = writeCell(row, col, student != null ? student.getStudentId() : null, lockedStyle);
            col = writeCell(row, col,
                    student != null ? student.getFirstName() + " " + student.getSurname() : "",
                    lockedStyle);

            // Dynamic criteria: weighting | max_score | id (locked) + score | comment (editable)
            for (AssessmentVO c : criteria) {
                MarkDetailPO detail = detailByCriteriaId.get(c.getCriteriaId());
                col = writeCell(row, col, c.getWeighting() != null ? (long) c.getWeighting() : null, lockedStyle);
                col = writeCell(row, col, c.getMaxMark() != null ? (long) c.getMaxMark() : null, lockedStyle);
                col = writeCell(row, col, detail != null ? detail.getId() : null, lockedStyle);
                col = writeCell(row, col, detail != null ? detail.getScore() : null, editableStyle);
                col = writeCell(row, col,
                        detail != null && detail.getComment() != null ? detail.getComment() : "",
                        editableStyle);
            }

            // total_score — editable
            col = writeCell(row, col, record.getTotalScore(), editableStyle);

            // Group_score — editable (direct value)
            col = writeCell(row, col, record.getGroupScore(), editableStyle);

            // Group_comment — editable
            col = writeCell(row, col,
                    gmr != null && gmr.getComment() != null ? gmr.getComment() : "",
                    editableStyle);

            // System ID columns — read-only (criteria_id is in criteria block above)
            col = writeCell(row, col, group.getId(), lockedStyle);
            col = writeCell(row, col, marker != null ? marker.getId() : null, lockedStyle);
            col = writeCell(row, col, subject != null ? subject.getId() : null, lockedStyle);
            col = writeCell(row, col, project.getId(), lockedStyle);
            col = writeCell(row, col, record.getId(), lockedStyle);
            writeCell(row, col, gmr != null ? gmr.getId() : null, lockedStyle);
        }
    }

    /**
     * Auto-size columns for a group sheet.
     * Total = 5 display + criteria*5 (weighting, max_score, id, score, comment)
     *       + 3 (total_score, Group_score, Group_comment)
     *       + 6 system IDs (group_id, Marker_id, Subject_id, Project_id, Mark_record_id, group_mark_record_id)
     *       = 5 + criteria*5 + 3 + 6 = criteria*5 + 14
     */
    private void autoSizeGroupColumns(Sheet sheet, List<AssessmentVO> criteria) {
        int totalCols = criteria.size() * 5 + 14;
        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ---- Cell writers ----

    private int writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
        return col + 1;
    }

    private int writeCell(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
        return col + 1;
    }

    private int writeCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
        return col + 1;
    }

    // ---- Style builders ----

    private CellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        return style;
    }

    /** Read-only data cell: locked=true (default), plain white background. */
    private CellStyle buildLockedDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setLocked(true);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        return style;
    }

    /**
     * Editable data cell: locked=false + light-yellow background so users can
     * visually distinguish which cells they are allowed to modify.
     */
    private CellStyle buildEditableDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setLocked(false);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        return style;
    }

    // ---- Utilities ----

    /**
     * Auto-size all columns for readability.
     * Total columns = 5 display (Marker_name, Subject_name, Project_name, Student_id, Student_name)
     *               + criteria * 5 (weighting, max_score, id, score, comment)
     *               + 1 total_score
     *               + 4 fixed ID columns (Marker_id, Subject_id, Project_id, Mark_record_id)
     *             = 5 + criteria*5 + 1 + 4 = criteria*5 + 10
     */
    private void autoSizeColumns(Sheet sheet, List<AssessmentVO> criteria) {
        int totalCols = criteria.size() * 5 + 10;
        for (int i = 0; i < totalCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Replace spaces with underscores and strip characters that are invalid in Excel header names.
     */
    private String sanitizeName(String name) {
        if (name == null) return "Unknown";
        return name.trim().replaceAll("[\\s/\\\\*?\\[\\]:]+", "_");
    }

    /**
     * Sanitize a group name for use as an Excel sheet name.
     * Excel sheet names: max 31 chars, cannot contain / \ * ? [ ] :
     */
    private String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) return "Sheet";
        String sanitized = name.trim().replaceAll("[/\\\\*?\\[\\]:]", "_");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }
}
