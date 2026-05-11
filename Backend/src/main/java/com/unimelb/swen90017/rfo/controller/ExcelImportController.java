package com.unimelb.swen90017.rfo.controller;

import com.unimelb.swen90017.rfo.pojo.vo.ImportResultVO;
import com.unimelb.swen90017.rfo.service.ExcelImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel raw-data import controller.
 * All endpoints require a valid JWT and ADMIN role (enforced by Spring Security).
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
public class ExcelImportController {

    @Autowired
    private ExcelImportService excelImportService;

    /**
     * Import modified mark data for an individual-type project from an Excel (.xlsx) file.
     * Each row is matched by Mark_record_id; editable columns (score, comment, total_score)
     * are overwritten in the database. Invalid rows are skipped and reported.
     *
     * POST /api/import/individual/{projectId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/individual/{projectId}")
    public ResponseEntity<ImportResultVO> importIndividual(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {

        log.info("Excel import requested: individual projectId={}, filename={}",
                projectId, file.getOriginalFilename());

        ImportResultVO result = excelImportService.importIndividualProject(projectId, file);
        return ResponseEntity.ok(result);
    }

    /**
     * Import modified mark data for a group-type project from an Excel (.xlsx) file.
     * The file must contain one sheet per group (sheet name = group name).
     * Each row is matched by Mark_record_id and group_mark_record_id; editable columns
     * (score, comment, total_score, group_score, group_comment) are overwritten in the database.
     * Invalid rows are skipped and reported with sheet and row information.
     *
     * POST /api/import/group/{projectId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/group/{projectId}")
    public ResponseEntity<ImportResultVO> importGroup(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {

        log.info("Excel import requested: group projectId={}, filename={}",
                projectId, file.getOriginalFilename());

        ImportResultVO result = excelImportService.importGroupProject(projectId, file);
        return ResponseEntity.ok(result);
    }
}
