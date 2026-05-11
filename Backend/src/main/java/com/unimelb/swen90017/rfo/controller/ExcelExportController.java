package com.unimelb.swen90017.rfo.controller;

import com.unimelb.swen90017.rfo.service.ExcelExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Excel raw-data export controller.
 * All endpoints require a valid JWT and ADMIN role (enforced by Spring Security).
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
public class ExcelExportController {

    @Autowired
    private ExcelExportService excelExportService;

    /**
     * Export raw mark data for an individual-type project as an Excel (.xlsx) file.
     *
     * GET /api/export/individual/{projectId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/individual/{projectId}")
    public ResponseEntity<byte[]> exportIndividual(@PathVariable Long projectId) {
        log.info("Excel export requested: individual projectId={}", projectId);

        byte[] data = excelExportService.exportIndividualProject(projectId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("project_" + projectId + "_individual.xlsx")
                        .build());
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * Export raw mark data for a group-type project as an Excel (.xlsx) file.
     * Each group is a separate sheet named after the group.
     *
     * GET /api/export/group/{projectId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/group/{projectId}")
    public ResponseEntity<byte[]> exportGroup(@PathVariable Long projectId) {
        log.info("Excel export requested: group projectId={}", projectId);

        byte[] data = excelExportService.exportGroupProject(projectId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("project_" + projectId + "_group.xlsx")
                        .build());
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
