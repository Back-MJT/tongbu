package com.ruoyi.iot.service.impl;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public Map<String, Object> selectUserTrainingSummary(Long userId)
    {
        Long tenantId = TenantContextHolder.getTenantId();
        Map<String, Object> summary = trainingSessionMapper.selectUserTrainingSummary(userId, tenantId);
        Map<String, Object> basis = trainingSessionMapper.selectUserRecentTrainingBasis(userId, tenantId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", String.valueOf(userId));
        result.put("totalSessions", numberValue(summary, "totalSessions").intValue());
        result.put("totalSets", numberValue(summary, "totalSets").intValue());
        result.put("totalReps", numberValue(summary, "totalReps").intValue());
        result.put("totalDurationMin", numberValue(summary, "totalDurationMin").intValue());
        result.put("peakVolumeKg", numberValue(summary, "peakVolumeKg").doubleValue());
        result.put("latestTrainingTime", summary != null ? summary.get("latestTrainingTime") : null);
        result.put("algorithmVersion", "XIN-RULE-v1");
        result.put("generatedForUserId", String.valueOf(userId));
        result.put("generationBasis", Map.of(
                "sessionsLast7Days", numberValue(basis, "sessionsLast7Days").intValue(),
                "sessionsLast30Days", numberValue(basis, "sessionsLast30Days").intValue(),
                "completedSetsToday", numberValue(basis, "completedSetsToday").intValue(),
                "userSeed", Math.abs(Objects.hashCode(userId)) % 3,
                "deviceType", basis != null && basis.get("deviceType") != null ? String.valueOf(basis.get("deviceType")) : "strength_station"));
        return result;
    }

    private Number numberValue(Map<String, Object> source, String key)
    {
        if (source == null || source.get(key) == null) return 0;
        Object value = source.get(key);
        if (value instanceof Number number) return number;
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
