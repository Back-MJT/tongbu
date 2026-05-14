package com.ruoyi.iot.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.iot.domain.entity.TrainingSessionRecord;
import com.ruoyi.iot.domain.model.TrainingSessionDetail;

/**
 * 训练记录 Service
 */
public interface ITrainingSessionService
{
    List<TrainingSessionRecord> selectTrainingSessionList(TrainingSessionRecord query);

    TrainingSessionDetail selectTrainingSessionDetail(Long sessionId);

    Map<String, Object> selectUserTrainingSummary(Long userId);
}
