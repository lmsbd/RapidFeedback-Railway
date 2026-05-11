package com.unimelb.swen90017.rfo.service;

import com.unimelb.swen90017.rfo.pojo.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel import service
 */
public interface ExcelImportService {

    /**
     * Import raw mark data from an individual-project Excel file.
     * Each data row is matched by Mark_record_id and {name}_id columns.
     * Editable columns (score, comment, total_score) are overwritten in the database.
     * Rows with missing or invalid IDs are skipped and reported in the result.
     *
     * @param projectId the project ID
     * @param file      the .xlsx file exported from GET /api/export/individual/{projectId}
     * @return import summary including success/failure counts and failure details
     */
    ImportResultVO importIndividualProject(Long projectId, MultipartFile file);

    /**
     * Import raw mark data from a group-project Excel file.
     * The file contains one sheet per group. Each data row is matched by Mark_record_id
     * and group_mark_record_id. Editable columns (score, comment, total_score, group_score,
     * group_comment) are overwritten in the database.
     * Rows with missing or invalid IDs are skipped and reported in the result.
     *
     * @param projectId the project ID
     * @param file      the .xlsx file exported from GET /api/export/group/{projectId}
     * @return import summary including success/failure counts and failure details
     */
    ImportResultVO importGroupProject(Long projectId, MultipartFile file);
}
