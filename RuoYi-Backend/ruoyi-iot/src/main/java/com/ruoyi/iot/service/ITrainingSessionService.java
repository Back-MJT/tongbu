package com.ruoyi.iot.service;

import java.util.List;
import com.ruoyi.iot.domain.entity.TrainingSessionRecord;
import com.ruoyi.iot.domain.model.TrainingSessionDetail;

/**
 * 训练记录 Service
 */
public interface ITrainingSessionService
{
    List<TrainingSessionRecord> selectTrainingSessionList(TrainingSessionRecord query);

    TrainingSessionDetail selectTrainingSessionDetail(Long sessionId);
}
