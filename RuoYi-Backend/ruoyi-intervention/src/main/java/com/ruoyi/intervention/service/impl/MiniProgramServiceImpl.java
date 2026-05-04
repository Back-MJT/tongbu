package com.ruoyi.intervention.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.intervention.entity.IotDeviceBinding;
import com.ruoyi.intervention.entity.IntervSession;
import com.ruoyi.intervention.entity.TrainingSetRecord;
import com.ruoyi.intervention.mapper.IotDeviceBindingMapper;
import com.ruoyi.intervention.mapper.IntervSessionMapper;
import com.ruoyi.intervention.mapper.TrainingSetRecordMapper;

/**
 * 小程序业务服务
 * XIN-147: 替代 MiniProgramController 中的 mock/TODO 逻辑
 */
@Service
public class MiniProgramServiceImpl {

    @Autowired
    private IotDeviceBindingMapper bindingMapper;

    @Autowired
    private IntervSessionMapper sessionMapper;

    @Autowired
    private TrainingSetRecordMapper trainingSetRecordMapper;

    // ══════════════════════════════════════════════════════════════
    // 设备绑定
    // ══════════════════════════════════════════════════════════════

    /**
     * 获取用户所有有效绑定设备
     */
    public List<Map<String, Object>> getUserDevices(Long userId, Long tenantId) {
        List<IotDeviceBinding> bindings = bindingMapper.selectByUserId(userId, tenantId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (IotDeviceBinding b : bindings) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bindingId", b.getBindingId());
            item.put("deviceId", b.getDeviceId());
            item.put("deviceCode", b.getDeviceCode());
            item.put("deviceName", b.getDeviceName() != null ? b.getDeviceName() : b.getDeviceCode());
            item.put("status", b.getStatus());
            item.put("boundAt", b.getBoundAt());
            result.add(item);
        }
        return result;
    }

    /**
     * 绑定设备
     * @return 绑定结果
     */
    @Transactional
    public Map<String, Object> bindDevice(Long userId, Long tenantId, String deviceCode, String deviceName) {
        // 检查是否已被其他用户绑定
        IotDeviceBinding existing = bindingMapper.selectActiveByDeviceCode(deviceCode, tenantId);
        if (existing != null && !existing.getUserId().equals(userId)) {
            return Map.of("success", false, "msg", "该设备已被其他用户绑定");
        }

        // 如果用户已有该设备绑定，先解绑
        List<IotDeviceBinding> userBindings = bindingMapper.selectByUserId(userId, tenantId);
        for (IotDeviceBinding ub : userBindings) {
            if (deviceCode.equals(ub.getDeviceCode())) {
                // 已有绑定，直接返回
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("success", true);
                r.put("bindingId", ub.getBindingId());
                r.put("deviceCode", deviceCode);
                r.put("deviceName", ub.getDeviceName() != null ? ub.getDeviceName() : deviceCode);
                r.put("status", "already_bound");
                r.put("msg", "设备已在绑定状态");
                return r;
            }
        }

        // 创建新绑定
        IotDeviceBinding binding = new IotDeviceBinding();
        binding.setUserId(userId);
        binding.setDeviceId(null); // deviceId 由 manufacturer 模块管理
        binding.setDeviceCode(deviceCode);
        binding.setDeviceName(deviceName != null ? deviceName : deviceCode);
        binding.setTenantId(tenantId);
        binding.setStatus("active");
        binding.setBoundAt(LocalDateTime.now());
        binding.setDelFlag("0");
        binding.setCreateBy(String.valueOf(userId));

        int rows = bindingMapper.insertBinding(binding);
        if (rows <= 0) {
            return Map.of("success", false, "msg", "绑定失败，请重试");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("bindingId", binding.getBindingId());
        result.put("deviceCode", deviceCode);
        result.put("deviceName", binding.getDeviceName());
        result.put("status", "active");
        result.put("msg", "设备绑定成功");
        return result;
    }

    /**
     * 解绑设备
     */
    @Transactional
    public boolean unbindDevice(Long userId, Long tenantId, Long bindingId) {
        IotDeviceBinding binding = bindingMapper.selectByBindingId(bindingId);
        if (binding == null || !binding.getUserId().equals(userId) || !binding.getTenantId().equals(tenantId)) {
            return false;
        }
        return bindingMapper.unbindDevice(bindingId, userId, tenantId) > 0;
    }

    // ══════════════════════════════════════════════════════════════
    // 训练会话
    // ══════════════════════════════════════════════════════════════

    /**
     * 获取用户训练历史 (分页)
     */
    public Map<String, Object> getTrainingHistory(Long userId, Long tenantId, int page, int size) {
        int offset = (page - 1) * size;
        List<IntervSession> sessions = sessionMapper.selectByUserId(userId, tenantId, offset, size);
        long total = sessionMapper.countByUserId(userId, tenantId);

        List<Map<String, Object>> records = new ArrayList<>();
        for (IntervSession s : sessions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", s.getSessionId());
            item.put("exerciseType", s.getExerciseType());
            item.put("completedSets", s.getCompletedSets());
            item.put("totalReps", s.getTotalReps());
            item.put("totalVolumeKg", s.getTotalVolumeKg());
            item.put("durationMin", s.getDurationMinutes());
            item.put("sessionDate", s.getSessionDate());
            item.put("sessionTime", s.getSessionTime());
            item.put("stage", s.getStage());
            records.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 提交训练会话
     */
    @Transactional
    public Map<String, Object> submitSession(Long userId, Long tenantId,
            String equipmentCode, String deviceCode, String exerciseType,
            Integer completedSets, Integer totalReps,
            Double totalVolumeKg, Integer durationMin,
            String stage, List<Map<String, Object>> sets) {

        IntervSession session = new IntervSession();
        session.setUserId(userId);
        session.setTenantId(tenantId);
        session.setEquipmentCode(equipmentCode);
        session.setDeviceCode(deviceCode);
        session.setExerciseType(exerciseType);
        session.setCompletedSets(completedSets != null ? completedSets : 0);
        session.setTotalReps(totalReps != null ? totalReps : 0);
        session.setTotalVolumeKg(totalVolumeKg != null ? BigDecimal.valueOf(totalVolumeKg) : BigDecimal.ZERO);
        session.setDurationMinutes(durationMin != null ? durationMin : 0);
        session.setSessionDate(LocalDate.now());
        session.setSessionTime(LocalDateTime.now());
        session.setStage(stage);
        session.setDelFlag("0");
        session.setCreateBy(String.valueOf(userId));

        sessionMapper.insertSession(session);
        insertTrainingSets(session.getSessionId(), userId, tenantId, equipmentCode, deviceCode, sets);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("status", "recorded");
        return result;
    }

    /**
     * 获取用户今日训练汇总
     */
    public Map<String, Object> getTodayProgress(Long userId, Long tenantId) {
        List<IntervSession> todaySessions = sessionMapper.selectTodayByUserId(userId, tenantId, LocalDate.now());

        int completedSessions = todaySessions.size();
        int totalDuration = todaySessions.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
        int totalReps = todaySessions.stream().mapToInt(s -> s.getTotalReps() != null ? s.getTotalReps() : 0).sum();
        // 计划训练组数: 默认4组
        int plannedSessions = 4;
        int complianceRate = plannedSessions > 0 ? Math.min(100, completedSessions * 100 / plannedSessions) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completedSessions", completedSessions);
        result.put("plannedSessions", plannedSessions);
        result.put("totalDurationMin", totalDuration);
        result.put("totalReps", totalReps);
        result.put("complianceRate", complianceRate);
        return result;
    }

    private void insertTrainingSets(Long sessionId, Long userId, Long tenantId,
            String equipmentCode, String deviceCode, List<Map<String, Object>> sets) {
        if (sets == null || sets.isEmpty()) {
            return;
        }

        List<TrainingSetRecord> records = new ArrayList<>();
        for (Map<String, Object> set : sets) {
            if (set == null) {
                continue;
            }
            TrainingSetRecord record = new TrainingSetRecord();
            record.setSessionId(sessionId);
            record.setUserId(userId);
            record.setTenantId(tenantId);
            record.setEquipmentCode(equipmentCode);
            record.setDeviceCode(deviceCode);
            record.setSetNo(toInteger(set.get("setNo")));
            record.setReps(toInteger(set.get("reps")));
            record.setDurationSec(toInteger(set.get("durationSec")));
            record.setStartedAt(toDateTime(set.get("startedAt")));
            record.setEndedAt(toDateTime(set.get("endedAt")));
            record.setCreateBy(String.valueOf(userId));
            records.add(record);
        }

        if (!records.isEmpty()) {
            trainingSetRecordMapper.insertBatch(records);
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(number.longValue()),
                    ZoneId.systemDefault());
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
