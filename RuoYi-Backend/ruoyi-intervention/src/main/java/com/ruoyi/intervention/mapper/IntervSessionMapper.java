package com.ruoyi.intervention.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.intervention.entity.IntervSession;

/**
 * 训练会话 Mapper
 */
public interface IntervSessionMapper {

    /**
     * 查询用户训练历史 (分页)
     */
    List<IntervSession> selectByUserId(
            @Param("userId") Long userId,
            @Param("tenantId") Long tenantId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 查询用户训练历史总数
     */
    long countByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 查询用户今日训练会话
     */
    List<IntervSession> selectTodayByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /**
     * 查询用户指定日期之后的训练会话
     */
    List<IntervSession> selectSinceByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /**
     * 汇总用户真实训练数据
     */
    Map<String, Object> selectUserTrainingStats(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 根据处方ID查询关联会话
     */
    List<IntervSession> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    /**
     * 插入训练会话
     */
    int insertSession(IntervSession session);

    /**
     * 更新训练会话
     */
    int updateSession(IntervSession session);
}
