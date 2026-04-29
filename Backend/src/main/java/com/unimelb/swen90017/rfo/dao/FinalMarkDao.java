package com.unimelb.swen90017.rfo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimelb.swen90017.rfo.pojo.po.FinalMarkPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Final mark data access interface
 */
@Mapper
public interface FinalMarkDao extends BaseMapper<FinalMarkPO> {

    @Select("SELECT * FROM final_mark WHERE project_id = #{projectId} AND student_id = #{studentId} LIMIT 1")
    FinalMarkPO getByProjectAndStudent(@Param("projectId") Long projectId,
                                       @Param("studentId") Long studentId);

    @Select("SELECT * FROM final_mark WHERE project_id = #{projectId} AND group_id = #{groupId} LIMIT 1")
    FinalMarkPO getByProjectAndGroup(@Param("projectId") Long projectId,
                                     @Param("groupId") Long groupId);

    @Select("SELECT * FROM final_mark WHERE project_id = #{projectId} "
            + "AND student_id = #{studentId} AND group_id = #{groupId} LIMIT 1")
    FinalMarkPO getByProjectStudentAndGroup(@Param("projectId") Long projectId,
                                            @Param("studentId") Long studentId,
                                            @Param("groupId") Long groupId);

    @Select("SELECT COUNT(*) FROM mark_record " +
            "WHERE project_id = #{projectId} AND student_id = #{studentId} AND total_score IS NOT NULL")
    int countCompletedMarkersForStudent(@Param("projectId") Long projectId,
                                        @Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(*)
        FROM (
            SELECT am.marker_id
            FROM (
                SELECT marker_id FROM marker_group
                WHERE project_id = #{projectId} AND group_id = #{groupId}
                UNION
                SELECT marker_id FROM group_mark_record
                WHERE project_id = #{projectId} AND group_id = #{groupId}
            ) am
            INNER JOIN group_mark_record gmr
              ON gmr.project_id = #{projectId}
             AND gmr.group_id = #{groupId}
             AND gmr.marker_id = am.marker_id
            INNER JOIN group_student gs
              ON gs.group_id = #{groupId}
             AND (gs.delete_status = 0 OR gs.delete_status IS NULL)
            LEFT JOIN mark_record mr
              ON mr.project_id = #{projectId}
             AND mr.marker_id = am.marker_id
             AND mr.student_id = gs.student_id
             AND mr.group_score IS NOT NULL
            GROUP BY am.marker_id
            HAVING COUNT(DISTINCT mr.student_id) = COUNT(DISTINCT gs.student_id)
        ) t
        """)
    int countCompletedMarkersForGroup(@Param("projectId") Long projectId,
                                      @Param("groupId") Long groupId);

    @Select("SELECT COUNT(*) FROM marker_student WHERE project_id = #{projectId} AND student_id = #{studentId}")
    int countAssignedMarkersForStudent(@Param("projectId") Long projectId,
                                       @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM marker_group WHERE project_id = #{projectId} AND group_id = #{groupId}")
    int countAssignedMarkersForGroup(@Param("projectId") Long projectId,
                                     @Param("groupId") Long groupId);
}
