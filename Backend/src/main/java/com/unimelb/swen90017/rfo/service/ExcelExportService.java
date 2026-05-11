package com.unimelb.swen90017.rfo.service;

/**
 * Excel export service
 */
public interface ExcelExportService {

    /**
     * Export raw data for an individual-type project.
     * Each row represents one marker's assessment of one student.
     *
     * @param projectId the project ID
     * @return byte array of the .xlsx file
     */
    byte[] exportIndividualProject(Long projectId);

    /**
     * Export raw data for a group-type project.
     * Each group gets its own sheet (named after the group).
     * Each row represents one marker's assessment of one student in that group.
     * Group_score cells in rows 2+ use a formula referencing row 2 so that
     * editing the first row's Group_score propagates to all rows in the sheet.
     *
     * @param projectId the project ID
     * @return byte array of the .xlsx file
     */
    byte[] exportGroupProject(Long projectId);
}
