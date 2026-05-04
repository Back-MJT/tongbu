package com.ruoyi.iot.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.iot.domain.entity.TrainingSessionRecord;
import com.ruoyi.iot.domain.entity.TrainingSetDetail;

/**
 * 训练记录 Mapper
 */
public interface TrainingSessionMapper
{
    List<TrainingSessionRecord> selectTrainingSessionList(TrainingSessionRecord query);

    TrainingSessionRecord selectTrainingSessionById(@Param("sessionId") Long sessionId, @Param("tenantId") Long tenantId);

    List<TrainingSetDetail> selectTrainingSetsBySessionId(@Param("sessionId") Long sessionId, @Param("tenantId") Long tenantId);
}
