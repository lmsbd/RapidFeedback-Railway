package com.unimelb.swen90017.rfo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response VO for GET /api/finalMark/list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalMarkListResponseVO {

    private List<FinalMarkItemVO> items;
    private String projectType;
    private String projectName;

    /**
     * Weighted maximum score of the project's rubric:
     * Σ (maximum_mark × weighting / 100) across all active assessment criteria,
     * rounded HALF_UP to 2 decimals. Denominator for every score in `items`.
     */
    private BigDecimal weightedMaxScore;
}
