package com.unimelb.swen90017.rfo.controller;

import com.unimelb.swen90017.rfo.common.Result;
import com.unimelb.swen90017.rfo.pojo.vo.FinalMarkListResponseVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.LockFinalMarkRequestVO;
import com.unimelb.swen90017.rfo.pojo.vo.request.SaveFinalMarkRequestVO;
import com.unimelb.swen90017.rfo.service.FinalMarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalMarkControllerTest {

    @Mock
    private FinalMarkService finalMarkService;

    private FinalMarkController controller;

    @BeforeEach
    void setUp() {
        controller = new FinalMarkController();
        ReflectionTestUtils.setField(controller, "finalMarkService", finalMarkService);
    }

    @Test
    void getFinalMarkList_delegatesToService() {
        FinalMarkListResponseVO responseVO = FinalMarkListResponseVO.builder()
                .projectName("Project A")
                .projectType("individual")
                .weightedMaxScore(new BigDecimal("100.00"))
                .build();
        when(finalMarkService.getFinalMarkList(11L)).thenReturn(responseVO);

        Result<FinalMarkListResponseVO> result = controller.getFinalMarkList(11L);

        assertEquals(responseVO, result.getData());
        verify(finalMarkService).getFinalMarkList(11L);
    }

    @Test
    void saveFinalMark_delegatesToService() {
        SaveFinalMarkRequestVO requestVO = new SaveFinalMarkRequestVO();
        requestVO.setProjectId(11L);
        requestVO.setStudentId(22L);
        requestVO.setFinalScore(new BigDecimal("88.5"));

        Result<Void> result = controller.saveFinalMark(requestVO);

        assertEquals(200, result.getCode());
        verify(finalMarkService).saveFinalMark(requestVO);
    }

    @Test
    void lockFinalMark_delegatesToService() {
        LockFinalMarkRequestVO requestVO = new LockFinalMarkRequestVO();
        requestVO.setProjectId(11L);
        requestVO.setStudentId(22L);
        requestVO.setIsLocked(true);

        Result<Void> result = controller.lockFinalMark(requestVO);

        assertEquals(200, result.getCode());
        verify(finalMarkService).lockFinalMark(requestVO);
    }
}
