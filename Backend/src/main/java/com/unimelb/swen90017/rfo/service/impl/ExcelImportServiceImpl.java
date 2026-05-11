package com.unimelb.swen90017.rfo.service.impl;

import com.unimelb.swen90017.rfo.dao.GroupMarkRecordDao;
import com.unimelb.swen90017.rfo.dao.MarkDetailDao;
import com.unimelb.swen90017.rfo.dao.MarkRecordDao;
import com.unimelb.swen90017.rfo.dao.ProjectDao;
import com.unimelb.swen90017.rfo.pojo.po.GroupMarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.MarkDetailPO;
import com.unimelb.swen90017.rfo.pojo.po.MarkRecordPO;
import com.unimelb.swen90017.rfo.pojo.po.ProjectPO;
import com.unimelb.swen90017.rfo.pojo.vo.ImportFailureVO;
import com.unimelb.swen90017.rfo.pojo.vo.ImportResultVO;
import com.unimelb.swen90017.rfo.service.ExcelImportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel import service implementation
 */
@Slf4j
@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    private static final String COL_MARK_RECORD_ID       = "Mark_record_id";
    private static final String COL_TOTAL_SCORE          = "total_score";
    private static final String COL_GROUP_SCORE          = "Group_score";
    private static final String COL_GROUP_COMMENT        = "Group_comment";
    private static final String COL_GROUP_MARK_RECORD_ID = "group_mark_record_id";
    private static final String SUFFIX_ID                = "_id";
    private static final String SUFFIX_SCORE             = "_score";
    private static final String SUFFIX_COMMENT           = "_comment";

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private MarkRecordDao markRecordDao;

    @Autowired
    private MarkDetailDao markDetailDao;

    @Autowired
    private GroupMarkRecordDao groupMarkRecordDao;

    @Override
    public ImportResultVO importIndividualProject(Long projectId, MultipartFile file) {
        ProjectPO project = projectDao.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        List<ImportFailureVO> failures = new ArrayList<>();
        int totalRows = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("individual");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'individual' not found in uploaded file");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Header row is missing");
            }

            Map<String, Integer> colIndex = buildColumnIndexMap(headerRow);

            Integer markRecordIdCol = colIndex.get(COL_MARK_RECORD_ID);
            Integer totalScoreCol   = colIndex.get(COL_TOTAL_SCORE);
            if (markRecordIdCol == null || totalScoreCol == null) {
                throw new IllegalArgumentException(
                        "Required columns Mark_record_id or total_score not found in header");
            }

            List<String> criteriaBaseNames = extractCriteriaBaseNames(colIndex);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                totalRows++;

                try {
                    processIndividualRow(row, i + 1, projectId, colIndex, markRecordIdCol,
                            totalScoreCol, criteriaBaseNames, failures);
                } catch (Exception e) {
                    log.warn("Unexpected error processing row {}: {}", i + 1, e.getMessage());
                    failures.add(ImportFailureVO.builder()
                            .row(i + 1)
                            .reason("Unexpected error: " + e.getMessage())
                            .build());
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse uploaded Excel file", e);
        }

        int failedRows = failures.size();
        int successRows = totalRows - failedRows;

        return ImportResultVO.builder()
                .totalRows(totalRows)
                .successRows(successRows)
                .failedRows(failedRows)
                .failures(failures)
                .build();
    }

    @Override
    public ImportResultVO importGroupProject(Long projectId, MultipartFile file) {
        ProjectPO project = projectDao.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        List<ImportFailureVO> failures = new ArrayList<>();
        int totalRows = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Uploaded file contains no sheets");
            }

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    log.warn("Sheet '{}' has no header row, skipping", sheetName);
                    continue;
                }

                Map<String, Integer> colIndex = buildColumnIndexMap(headerRow);

                Integer markRecordIdCol      = colIndex.get(COL_MARK_RECORD_ID);
                Integer totalScoreCol        = colIndex.get(COL_TOTAL_SCORE);
                Integer groupScoreCol        = colIndex.get(COL_GROUP_SCORE);
                Integer groupCommentCol      = colIndex.get(COL_GROUP_COMMENT);
                Integer groupMarkRecordIdCol = colIndex.get(COL_GROUP_MARK_RECORD_ID);

                if (markRecordIdCol == null || totalScoreCol == null
                        || groupScoreCol == null || groupCommentCol == null
                        || groupMarkRecordIdCol == null) {
                    throw new IllegalArgumentException(
                            "Sheet '" + sheetName + "' is missing required columns: "
                            + "Mark_record_id, total_score, Group_score, Group_comment, group_mark_record_id");
                }

                List<String> criteriaBaseNames = extractCriteriaBaseNames(colIndex);

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }
                    totalRows++;

                    try {
                        processGroupRow(row, i + 1, sheetName, projectId, colIndex,
                                markRecordIdCol, totalScoreCol, groupScoreCol,
                                groupCommentCol, groupMarkRecordIdCol,
                                criteriaBaseNames, failures);
                    } catch (Exception e) {
                        log.warn("Unexpected error processing sheet '{}' row {}: {}",
                                sheetName, i + 1, e.getMessage());
                        failures.add(ImportFailureVO.builder()
                                .sheet(sheetName)
                                .row(i + 1)
                                .reason("Unexpected error: " + e.getMessage())
                                .build());
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse uploaded Excel file", e);
        }

        int failedRows = failures.size();
        int successRows = totalRows - failedRows;

        return ImportResultVO.builder()
                .totalRows(totalRows)
                .successRows(successRows)
                .failedRows(failedRows)
                .failures(failures)
                .build();
    }

    private void processIndividualRow(Row row, int excelRowNum, Long projectId,
                                      Map<String, Integer> colIndex,
                                      int markRecordIdCol, int totalScoreCol,
                                      List<String> criteriaBaseNames,
                                      List<ImportFailureVO> failures) {

        Long markRecordId = readLong(row.getCell(markRecordIdCol));
        if (markRecordId == null) {
            failures.add(ImportFailureVO.builder()
                    .row(excelRowNum)
                    .reason("Mark_record_id is missing or invalid")
                    .build());
            return;
        }

        MarkRecordPO markRecord = markRecordDao.selectById(markRecordId);
        if (markRecord == null) {
            failures.add(ImportFailureVO.builder()
                    .row(excelRowNum)
                    .reason("Mark_record_id not found: " + markRecordId)
                    .build());
            return;
        }
        if (!markRecord.getProjectId().equals(projectId)) {
            failures.add(ImportFailureVO.builder()
                    .row(excelRowNum)
                    .reason("Mark_record_id " + markRecordId + " does not belong to project " + projectId)
                    .build());
            return;
        }

        // Update mark_record.total_score
        markRecord.setTotalScore(readBigDecimal(row.getCell(totalScoreCol)));
        markRecordDao.updateById(markRecord);

        // Update mark_detail.score and mark_detail.comment for each criterion
        updateCriteriaDetails(row, excelRowNum, null, markRecordId, colIndex, criteriaBaseNames);
    }

    private void processGroupRow(Row row, int excelRowNum, String sheetName, Long projectId,
                                 Map<String, Integer> colIndex,
                                 int markRecordIdCol, int totalScoreCol,
                                 int groupScoreCol, int groupCommentCol,
                                 int groupMarkRecordIdCol,
                                 List<String> criteriaBaseNames,
                                 List<ImportFailureVO> failures) {

        Long markRecordId = readLong(row.getCell(markRecordIdCol));
        if (markRecordId == null) {
            failures.add(ImportFailureVO.builder()
                    .sheet(sheetName)
                    .row(excelRowNum)
                    .reason("Mark_record_id is missing or invalid")
                    .build());
            return;
        }

        MarkRecordPO markRecord = markRecordDao.selectById(markRecordId);
        if (markRecord == null) {
            failures.add(ImportFailureVO.builder()
                    .sheet(sheetName)
                    .row(excelRowNum)
                    .reason("Mark_record_id not found: " + markRecordId)
                    .build());
            return;
        }
        if (!markRecord.getProjectId().equals(projectId)) {
            failures.add(ImportFailureVO.builder()
                    .sheet(sheetName)
                    .row(excelRowNum)
                    .reason("Mark_record_id " + markRecordId + " does not belong to project " + projectId)
                    .build());
            return;
        }

        // Update mark_record.total_score and mark_record.group_score
        markRecord.setTotalScore(readBigDecimal(row.getCell(totalScoreCol)));
        markRecord.setGroupScore(readBigDecimal(row.getCell(groupScoreCol)));
        markRecordDao.updateById(markRecord);

        // Update group_mark_record.comment via group_mark_record_id
        Long gmrId = readLong(row.getCell(groupMarkRecordIdCol));
        if (gmrId != null) {
            GroupMarkRecordPO gmr = groupMarkRecordDao.selectById(gmrId);
            if (gmr == null) {
                log.warn("Sheet '{}' row {}: group_mark_record id={} not found, skipping group comment",
                        sheetName, excelRowNum, gmrId);
            } else if (!gmr.getProjectId().equals(projectId)) {
                log.warn("Sheet '{}' row {}: group_mark_record id={} does not belong to project {}, skipping",
                        sheetName, excelRowNum, gmrId, projectId);
            } else {
                gmr.setComment(readString(row.getCell(groupCommentCol)));
                groupMarkRecordDao.updateById(gmr);
            }
        }

        // Update mark_detail.score and mark_detail.comment for each criterion
        updateCriteriaDetails(row, excelRowNum, sheetName, markRecordId, colIndex, criteriaBaseNames);
    }

    private void updateCriteriaDetails(Row row, int excelRowNum, String sheetName,
                                       Long expectedMarkRecordId,
                                       Map<String, Integer> colIndex,
                                       List<String> criteriaBaseNames) {
        for (String baseName : criteriaBaseNames) {
            Integer idCol      = colIndex.get(baseName + SUFFIX_ID);
            Integer scoreCol   = colIndex.get(baseName + SUFFIX_SCORE);
            Integer commentCol = colIndex.get(baseName + SUFFIX_COMMENT);

            if (idCol == null || scoreCol == null || commentCol == null) {
                continue;
            }

            Long detailId = readLong(row.getCell(idCol));
            if (detailId == null) {
                // No mark_detail row for this criterion yet — skip silently
                continue;
            }

            MarkDetailPO detail = markDetailDao.selectById(detailId);
            if (detail == null) {
                log.warn("Sheet '{}' row {}: mark_detail id={} not found, skipping criteria '{}'",
                        sheetName, excelRowNum, detailId, baseName);
                continue;
            }

            // Verify this detail belongs to the current mark_record (prevent cross-record tampering)
            if (!detail.getMarkRecordId().equals(expectedMarkRecordId)) {
                log.warn("Sheet '{}' row {}: mark_detail id={} does not belong to mark_record id={}, skipping criteria '{}'",
                        sheetName, excelRowNum, detailId, expectedMarkRecordId, baseName);
                continue;
            }

            detail.setScore(readBigDecimal(row.getCell(scoreCol)));
            detail.setComment(readString(row.getCell(commentCol)));
            markDetailDao.updateById(detail);
        }
    }

    /**
     * Build a map of header column name → column index.
     */
    private Map<String, Integer> buildColumnIndexMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String name = cell.getStringCellValue().trim();
                if (!name.isEmpty()) {
                    map.put(name, i);
                }
            }
        }
        return map;
    }

    /**
     * Extract criteria base names from header columns.
     * A criteria column set is identified by {baseName}_id, excluding known system ID columns.
     */
    private List<String> extractCriteriaBaseNames(Map<String, Integer> colIndex) {
        List<String> baseNames = new ArrayList<>();
        for (String colName : colIndex.keySet()) {
            if (colName.endsWith(SUFFIX_ID)) {
                String base = colName.substring(0, colName.length() - SUFFIX_ID.length());
                if (!base.equals("Marker") && !base.equals("Subject")
                        && !base.equals("Project") && !base.equals("Mark_record")
                        && !base.equals("group") && !base.equals("group_mark_record")
                        && !base.equals("Student")) {
                    baseNames.add(base);
                }
            }
        }
        return baseNames;
    }

    // ---- Cell readers ----

    private Long readLong(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Long.parseLong(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal readBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) return null;
            try {
                return new BigDecimal(val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String readString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return "";
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
