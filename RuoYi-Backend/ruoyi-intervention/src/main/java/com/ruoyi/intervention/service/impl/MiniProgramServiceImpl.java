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
        return submitSession(userId, tenantId, null, equipmentCode, deviceCode, exerciseType,
                completedSets, totalReps, totalVolumeKg, durationMin, stage, sets);
    }

    @Transactional
    public Map<String, Object> submitSession(Long userId, Long tenantId, Long deviceId,
            String equipmentCode, String deviceCode, String exerciseType,
            Integer completedSets, Integer totalReps,
            Double totalVolumeKg, Integer durationMin,
            String stage, List<Map<String, Object>> sets) {

        IntervSession session = new IntervSession();
        session.setUserId(userId);
        session.setTenantId(tenantId);
        session.setDeviceId(deviceId);
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

        int completedSessions = todaySessions.stream().mapToInt(s -> s.getCompletedSets() != null ? s.getCompletedSets() : 0).sum();
        int totalDuration = todaySessions.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
        int totalReps = todaySessions.stream().mapToInt(s -> s.getTotalReps() != null ? s.getTotalReps() : 0).sum();
        int plannedSessions = calculatePlannedSets(buildRulePrescription(userId, tenantId, null, 30));
        int complianceRate = plannedSessions > 0 ? Math.min(100, completedSessions * 100 / plannedSessions) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().toString());
        result.put("completedSessions", completedSessions);
        result.put("plannedSessions", plannedSessions);
        result.put("totalDurationMin", totalDuration);
        result.put("totalReps", totalReps);
        result.put("complianceRate", complianceRate);
        return result;
    }

    /**
     * 获取用户真实训练总览，用于小程序“我的”和后台验收。
     */
    public Map<String, Object> getUserTrainingStats(Long userId, Long tenantId) {
        Map<String, Object> raw = sessionMapper.selectUserTrainingStats(userId, tenantId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", String.valueOf(userId));
        result.put("totalSessions", numberValue(raw, "totalSessions").intValue());
        result.put("totalSets", numberValue(raw, "totalSets").intValue());
        result.put("totalReps", numberValue(raw, "totalReps").intValue());
        result.put("totalDurationMin", numberValue(raw, "totalDurationMin").intValue());
        result.put("peakVolumeKg", numberValue(raw, "peakVolumeKg").doubleValue());
        result.put("latestTrainingTime", raw != null ? raw.get("latestTrainingTime") : null);
        result.put("algorithmVersion", "XIN-RULE-v1");
        result.put("generatedPlan", buildRulePrescription(userId, tenantId, null, 30));
        return result;
    }

    /**
     * 体验版规则处方：基于用户近期真实训练记录、设备类型和年龄生成不同计划。
     */
    public Map<String, Object> buildRulePrescription(Long userId, Long tenantId, String deviceType, int age) {
        List<IntervSession> recent30 = sessionMapper.selectSinceByUserId(userId, tenantId, LocalDate.now().minusDays(29));
        List<IntervSession> recent7 = sessionMapper.selectSinceByUserId(userId, tenantId, LocalDate.now().minusDays(6));
        List<IntervSession> todaySessions = sessionMapper.selectTodayByUserId(userId, tenantId, LocalDate.now());

        int sessions30 = recent30.size();
        int sessions7 = recent7.size();
        int completedSetsToday = recentTodaySets(todaySessions);
        int avgReps = averageReps(recent30);
        int userSeed = Math.abs(Objects.hashCode(userId)) % 3;
        String effectiveDeviceType = firstNonBlank(deviceType, mostRecentDeviceType(recent30), "strength_station");
        String stage = resolveStage(age, sessions30);
        int baseSets = switch (stage) {
            case "beginner" -> 3;
            case "growth" -> 4;
            case "plateau" -> 5;
            default -> 4;
        };
        int targetSets = Math.max(2, baseSets - Math.min(2, completedSetsToday));
        int targetReps = avgReps > 0 ? clamp(avgReps + userSeed - 1, 8, 15) : (sessions30 >= 8 ? 12 : 10) + userSeed;
        double targetLoad = defaultLoadByDevice(effectiveDeviceType, sessions30, userSeed);
        String mainExercise = exerciseNameByDevice(effectiveDeviceType);
        String exerciseType = exerciseTypeByDevice(effectiveDeviceType);

        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(task(1, "动态热身", "warmup", 1, 8, 0, "低强度", 45,
                completedSetsToday > 0 ? "completed" : "pending",
                "先活动肩、髋和踝关节，给主训练留出稳定发力空间。"));
        tasks.add(task(2, mainExercise, exerciseType, targetSets, targetReps, targetLoad,
                intensityByStage(stage), restByStage(stage), "pending",
                coachingTip(stage, sessions7, completedSetsToday)));

        int plannedSets = calculatePlannedSets(tasks);
        int totalDuration = todaySessions.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
        int totalReps = todaySessions.stream().mapToInt(s -> s.getTotalReps() != null ? s.getTotalReps() : 0).sum();
        int complianceRate = plannedSets > 0 ? Math.min(100, completedSetsToday * 100 / plannedSets) : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().toString());
        result.put("plannedSessions", plannedSets);
        result.put("completedSessions", completedSetsToday);
        result.put("totalDurationMin", totalDuration);
        result.put("totalSets", plannedSets);
        result.put("totalReps", totalReps);
        result.put("complianceRate", complianceRate);
        result.put("tasks", tasks);
        result.put("aiSuggestion", ruleSuggestion(stage, sessions7, sessions30, completedSetsToday, mainExercise));
        result.put("coachingReasoning", "根据最近30天训练次数、最近7天频率、今日已完成组数和设备类型生成。");
        result.put("exerciseGoal", goalByStage(stage));
        result.put("exerciseGoalEn", stage + "_rule_plan");
        result.put("userStage", stage);
        result.put("targetHrZone", null);
        result.put("healthTips", List.of(healthTip("训练节奏", "每组保留1-2次余力，动作质量优先于速度。")));
        result.put("algorithmVersion", "XIN-RULE-v1");
        result.put("generatedForUserId", String.valueOf(userId));
        result.put("generationBasis", Map.of(
                "sessionsLast7Days", sessions7,
                "sessionsLast30Days", sessions30,
                "completedSetsToday", completedSetsToday,
                "userSeed", userSeed,
                "deviceType", effectiveDeviceType));
        result.put("fallback", false);
        return result;
    }

    private int calculatePlannedSets(Map<String, Object> prescription) {
        Object tasks = prescription.get("tasks");
        return tasks instanceof List<?> ? calculatePlannedSets((List<?>) tasks) : 4;
    }

    private Number numberValue(Map<String, Object> source, String key) {
        if (source == null || source.get(key) == null) {
            return 0;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int calculatePlannedSets(List<?> tasks) {
        int total = 0;
        for (Object item : tasks) {
            if (item instanceof Map<?, ?> task) {
                Integer targetSets = toInteger(task.get("targetSets"));
                total += targetSets != null ? targetSets : 0;
            }
        }
        return total > 0 ? total : 4;
    }

    private int recentTodaySets(List<IntervSession> sessions) {
        return sessions.stream().mapToInt(s -> s.getCompletedSets() != null ? s.getCompletedSets() : 0).sum();
    }

    private int averageReps(List<IntervSession> sessions) {
        int sets = sessions.stream().mapToInt(s -> s.getCompletedSets() != null ? s.getCompletedSets() : 0).sum();
        int reps = sessions.stream().mapToInt(s -> s.getTotalReps() != null ? s.getTotalReps() : 0).sum();
        return sets > 0 ? Math.round((float) reps / sets) : 0;
    }

    private String mostRecentDeviceType(List<IntervSession> sessions) {
        return sessions.stream().map(IntervSession::getExerciseType).filter(this::hasText).findFirst().orElse(null);
    }

    private String resolveStage(int age, int sessions30) {
        if (sessions30 <= 2 || age >= 60) return "beginner";
        if (sessions30 >= 12) return "plateau";
        return "growth";
    }

    private Map<String, Object> task(int taskId, String name, String type, int sets, int reps, double load,
            String intensity, int rest, String status, String tip) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", taskId);
        task.put("exerciseName", name);
        task.put("exerciseType", type);
        task.put("targetSets", sets);
        task.put("targetReps", reps);
        task.put("targetLoadKg", load);
        task.put("targetHr", null);
        task.put("intensityLabel", intensity);
        task.put("restSeconds", rest);
        task.put("status", status);
        task.put("coachingTip", tip);
        return task;
    }

    private String ruleSuggestion(String stage, int sessions7, int sessions30, int completedSetsToday, String exercise) {
        if (completedSetsToday > 0) {
            return "今天已经完成部分训练，剩余计划建议降低追求速度，继续把动作做稳。";
        }
        if ("beginner".equals(stage)) {
            return "今天以建立习惯为主，完成热身和" + exercise + "即可，不需要追求大重量。";
        }
        if ("plateau".equals(stage) || sessions7 >= 4) {
            return "近期训练频率较高，今天建议控制强度，用标准动作突破平台期。";
        }
        return "最近30天已有" + sessions30 + "次训练记录，今天适合按计划完成主训练并记录真实数据。";
    }

    private String coachingTip(String stage, int sessions7, int completedSetsToday) {
        if (completedSetsToday > 0) return "已经开始训练，后续每组之间充分休息，避免为了完成率牺牲动作。";
        if ("beginner".equals(stage)) return "先找准动作轨迹，重量可以轻一些。";
        if (sessions7 >= 4) return "近期频率较高，注意肩背稳定和呼吸节奏。";
        return "保持均匀节奏，每组最后2次应有挑战但不变形。";
    }

    private String goalByStage(String stage) {
        return switch (stage) {
            case "beginner" -> "建立训练习惯";
            case "plateau" -> "突破训练平台";
            default -> "提升力量耐力";
        };
    }

    private String intensityByStage(String stage) {
        return "plateau".equals(stage) ? "中高强度" : ("beginner".equals(stage) ? "低中强度" : "中等强度");
    }

    private int restByStage(String stage) {
        return "plateau".equals(stage) ? 120 : ("beginner".equals(stage) ? 75 : 90);
    }

    private String exerciseNameByDevice(String deviceType) {
        return switch (firstNonBlank(deviceType, "strength_station")) {
            case "treadmill", "跑步机" -> "跑步训练";
            case "rowing", "rowing_machine", "划船机" -> "划船训练";
            case "cycling", "spin_bike", "动感单车" -> "骑行训练";
            case "chest_press", "strength", "strength_station", "力量站" -> "器械力量训练";
            default -> "综合训练";
        };
    }

    private String exerciseTypeByDevice(String deviceType) {
        return switch (firstNonBlank(deviceType, "strength_station")) {
            case "treadmill", "跑步机" -> "treadmill";
            case "rowing", "rowing_machine", "划船机" -> "rowing";
            case "cycling", "spin_bike", "动感单车" -> "cycling";
            case "chest_press" -> "chest_press";
            default -> "strength";
        };
    }

    private double defaultLoadByDevice(String deviceType, int sessions30, int userSeed) {
        double base = switch (firstNonBlank(deviceType, "strength_station")) {
            case "rowing", "rowing_machine", "划船机" -> 30.0;
            case "cycling", "spin_bike", "动感单车" -> 3.0;
            case "treadmill", "跑步机" -> 0.0;
            default -> 20.0;
        };
        if (base <= 0) return base;
        return base + (sessions30 >= 12 ? 5.0 : 0.0) + userSeed * 1.5;
    }

    private Map<String, Object> healthTip(String title, String content) {
        Map<String, Object> tip = new LinkedHashMap<>();
        tip.put("title", title);
        tip.put("content", content);
        tip.put("evidenceSource", "XIN-RULE-v1");
        tip.put("category", "training");
        return tip;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) return value;
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
