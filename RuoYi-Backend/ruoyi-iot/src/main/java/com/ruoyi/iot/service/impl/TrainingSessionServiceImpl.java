package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.tenant.TenantContextHolder;
import com.ruoyi.iot.domain.entity.TrainingSessionRecord;
import com.ruoyi.iot.domain.model.TrainingSessionDetail;
import com.ruoyi.iot.mapper.TrainingSessionMapper;
import com.ruoyi.iot.service.ITrainingSessionService;

/**
 * 训练记录 Service 实现
 */
@Service
public class TrainingSessionServiceImpl implements ITrainingSessionService
{
    @Autowired
    private TrainingSessionMapper trainingSessionMapper;

    @Override
    public List<TrainingSessionRecord> selectTrainingSessionList(TrainingSessionRecord query)
    {
        query.setTenantId(TenantContextHolder.getTenantId());
        return trainingSessionMapper.selectTrainingSessionList(query);
    }

    @Override
    public TrainingSessionDetail selectTrainingSessionDetail(Long sessionId)
    {
        Long tenantId = TenantContextHolder.getTenantId();
        TrainingSessionDetail detail = new TrainingSessionDetail();
        detail.setSession(trainingSessionMapper.selectTrainingSessionById(sessionId, tenantId));
        detail.setSets(trainingSessionMapper.selectTrainingSetsBySessionId(sessionId, tenantId));
        return detail;
    }
}
