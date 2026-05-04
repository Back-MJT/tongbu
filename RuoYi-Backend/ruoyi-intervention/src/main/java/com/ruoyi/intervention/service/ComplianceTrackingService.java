package com.ruoyi.intervention.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ruoyi.intervention.domain.enums.ComplianceLevel;
import com.ruoyi.intervention.domain.enums.FeedbackType;
import com.ruoyi.intervention.domain.model.FeedbackEntry;
import com.ruoyi.intervention.domain.model.LowComplianceAlert;
import com.ruoyi.intervention.domain.model.UserComplianceStatus;

/**
 * 依从性追踪服务 — 干预方案执行依从性监测
 * Compliance tracking service for monitoring intervention plan adherence.
 *
 * <p>Evidence base / 循证参考:
 * <ul>
 *   <li>Klasnja et al. 2015: Just-in-Time Adaptive Interventions (JITAIs)</li>
 *   <li>Nahum-Shani et al. 2018: JITAI design patterns</li>
 *   <li>Liao et al. 2020: Micro-randomized trials for JITAI optimization</li>
 * </ul>
 *
 * <p>Acceptance criteria (XIN-62):
 * <ul>
 *   <li>Given: user has not performed an intervention for 3 consecutive days</li>
 *   <li>When: viewing the intervention tracking page</li>
 *   <li>Then: user shows low compliance indicator (orange)</li>
 *   <li>And: homepage shows compliance warning banner</li>
 * </ul>
 *
 * <p>Compliance levels / 依从性等级:
 * <ul>
 *   <li>HIGH   (green)  — compliant, following plan closely</li>
 *   <li>MEDIUM (yellow) — minor lapses but acceptable</li>
 *   <li>LOW    (orange) — 3+ consecutive days without intervention</li>
 *   <li>NONE   (gray)   — no active prescription or no data</li>
 * </ul>
 *
 * Migrated from: intervention-engine/src/algorithms/compliance_tracking.py
 * Ticket: XIN-83
 */
@Service
public class ComplianceTrackingService
{
    private static final Logger log = LoggerFactory.getLogger(ComplianceTrackingService.class);

    // ========== Configuration Constants (from ComplianceConfig) ==========

    /** 连续未执行天数阈值(LOW触发) / Days without intervention to trigger LOW level */
    private static final int LOW_COMPLIANCE_THRESHOLD_DAYS = 3;

    /** 依从率计算窗口(天) / Lookback window for compliance rate calculation */
    private static final int COMPLIANCE_RATE_WINDOW_DAYS = 7;

    /** 中等依从率阈值 / Compliance rate below this triggers MEDIUM level */
    private static final double MEDIUM_COMPLIANCE_RATE_THRESHOLD = 0.6;

    /** 长期回溯窗口(天) / Long lookback window for last execution detection */
    private static final int LONG_LOOKBACK_DAYS = 90;

    // ========== Instance State (in-memory, Phase 1) ==========

    /** 用户最后执行时间 / user_id → last execution datetime */
    private final Map<String, LocalDateTime> lastExecution = new HashMap<>();

    /** 用户执行历史 / user_id → list of execution datetimes */
    private final Map<String, List<LocalDateTime>> executionHistory = new HashMap<>();

    /** 用户活跃处方数量 / user_id → active prescription count */
    private final Map<String, Integer> activePrescriptionCounts = new HashMap<>();

    /** 反馈记录存储 / user_id → list of feedback entries */
    private final Map<String, List<FeedbackEntry>> feedbackStore = new HashMap<>();

    // ========== Recording ==========

    /**
     * 记录用户执行了一次干预 / Record that a user performed an intervention.
     *
     * @param userId         用户标识 / User identifier
     * @param interventionId 干预处方ID / Prescription/intervention that was performed
     * @param performedAt    执行时间 / Timestamp of the execution, null = now
     */
    public void recordInterventionExecution(String userId, String interventionId, LocalDateTime performedAt)
    {
        if (performedAt == null)
        {
            performedAt = LocalDateTime.now();
        }

        // Update last execution only if more recent
        LocalDateTime current = lastExecution.get(userId);
        if (current == null || performedAt.isAfter(current))
        {
            lastExecution.put(userId, performedAt);
        }

        // Append to execution history
        executionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(performedAt);

        log.debug("recordInterventionExecution: userId={}, interventionId={}, performedAt={}",
                  userId, interventionId, performedAt);
    }

    /**
     * 记录反馈条目 / Store a feedback entry for compliance tracking.
     *
     * @param entry 反馈条目 / Feedback entry
     */
    public void recordFeedback(FeedbackEntry entry)
    {
        String userId = entry.getUserId();
        feedbackStore.computeIfAbsent(userId, k -> new ArrayList<>()).add(entry);

        // If this is a compliance feedback with an explicit timestamp, also record the execution.
        // When timestamp is null, the feedback is about a past event (e.g. user reporting retroactively)
        // and should NOT update the last-execution tracker, which would incorrectly reset daysWithout.
        if (entry.getFeedbackType() == FeedbackType.COMPLIANCE)
        {
            LocalDateTime ts = toLocalDateTime(entry.getTimestamp());
            if (ts != null)
            {
                recordInterventionExecution(userId, entry.getInterventionId(), ts);
            }
        }
    }

    /**
     * 设置用户活跃处方数 / Set the active prescription count for a user.
     *
     * @param userId 用户标识
     * @param count  活跃处方数
     */
    public void setActivePrescriptionCount(String userId, int count)
    {
        activePrescriptionCounts.put(userId, count);
    }

    // ========== Compliance Query ==========

    /**
     * 获取单个用户的依从性状态 / Get compliance status for a single user.
     *
     * <p>Evidence: klasnja_2015_jitai, nahum_shani_2018_jitai_design
     *
     * @param userId 用户标识 / User identifier
     * @return 依从性状态 / UserComplianceStatus
     */
    public UserComplianceStatus getUserCompliance(String userId)
    {
        UserComplianceStatus status = new UserComplianceStatus();
        status.setUserId(userId);
        status.setComputedAt(LocalDateTime.now());
        status.setEvidenceRef("klasnja_2015_jitai");

        int activeCount = activePrescriptionCounts.getOrDefault(userId, 0);
        status.setActivePrescriptionCount(activeCount);

        // No active prescription → NONE
        if (activeCount == 0 && !lastExecution.containsKey(userId))
        {
            status.setComplianceLevel(ComplianceLevel.NONE);
            status.setDaysWithoutIntervention(0);
            status.setComplianceRate7d(null);
            status.setLevelReason("无活跃处方或无执行记录");
            log.debug("getUserCompliance: userId={} → NONE (no prescriptions, no data)", userId);
            return status;
        }

        // Days without intervention
        LocalDateTime lastExec = lastExecution.get(userId);
        int daysWithout;
        String lastDateStr = null;

        if (lastExec != null)
        {
            LocalDate lastDate = lastExec.toLocalDate();
            LocalDate today = LocalDate.now();
            daysWithout = (int) java.time.temporal.ChronoUnit.DAYS.between(lastDate, today);
            lastDateStr = lastDate.toString();
        }
        else
        {
            // Never executed
            daysWithout = activeCount > 0 ? 999 : 0;
        }

        status.setDaysWithoutIntervention(daysWithout);
        status.setLastInterventionDate(lastDateStr);

        // 7-day compliance rate
        Double rate7d = computeComplianceRate7d(userId);
        status.setComplianceRate7d(rate7d);

        // Determine compliance level
        ComplianceLevel level;
        String reason;

        if (daysWithout >= LOW_COMPLIANCE_THRESHOLD_DAYS)
        {
            level = ComplianceLevel.LOW;
            reason = String.format("连续%d天未执行干预方案", daysWithout);
        }
        else if (rate7d != null && rate7d < MEDIUM_COMPLIANCE_RATE_THRESHOLD)
        {
            level = ComplianceLevel.MEDIUM;
            reason = String.format("7日依从率%.0f%%，低于%.0f%%阈值", rate7d * 100, MEDIUM_COMPLIANCE_RATE_THRESHOLD * 100);
        }
        else if (daysWithout == 0 && rate7d != null && rate7d >= MEDIUM_COMPLIANCE_RATE_THRESHOLD)
        {
            level = ComplianceLevel.HIGH;
            reason = "今日已执行，依从性良好";
        }
        else if (lastExec != null)
        {
            level = ComplianceLevel.HIGH;
            reason = String.format("最近执行于%s，依从性良好", lastDateStr);
        }
        else
        {
            level = ComplianceLevel.NONE;
            reason = "无执行记录";
        }

        status.setComplianceLevel(level);
        status.setLevelReason(reason);

        log.debug("getUserCompliance: userId={} → {} (daysWithout={}, rate7d={})",
                  userId, level.getCode(), daysWithout, rate7d);
        return status;
    }

    /**
     * 批量获取低依从性用户告警 / Get low-compliance alerts for all specified users.
     *
     * <p>Returns alerts for users with LOW compliance level.
     *
     * @param userIds 用户ID列表 / List of user IDs to check
     * @return 低依从性告警列表 / List of LowComplianceAlert for LOW users
     */
    public List<LowComplianceAlert> getLowComplianceAlerts(List<String> userIds)
    {
        List<LowComplianceAlert> alerts = new ArrayList<>();

        for (String userId : userIds)
        {
            UserComplianceStatus status = getUserCompliance(userId);
            if (status.getComplianceLevel() == ComplianceLevel.LOW)
            {
                LowComplianceAlert alert = new LowComplianceAlert();
                alert.setAlertId(UUID.randomUUID().toString());
                alert.setUserId(userId);
                alert.setComplianceLevel(ComplianceLevel.LOW);
                alert.setDaysWithoutIntervention(status.getDaysWithoutIntervention());
                alert.setLastInterventionDate(status.getLastInterventionDate());
                alert.setSeverity("warning");
                alert.setTitle("低依从性警告");
                alert.setMessage(String.format("用户%s已连续%d天未执行干预方案，请关注",
                                               userId, status.getDaysWithoutIntervention()));
                alerts.add(alert);
            }
        }

        log.debug("getLowComplianceAlerts: checked {} users, found {} low-compliance",
                  userIds.size(), alerts.size());
        return alerts;
    }

    /**
     * 获取所有用户的依从性汇总 / Get a summary of compliance across all users.
     *
     * @param userIds 所有用户ID / All user IDs to summarize
     * @return 汇总数据 / Dict with counts by level and overall stats
     */
    public Map<String, Object> getComplianceSummary(List<String> userIds)
    {
        List<UserComplianceStatus> statuses = new ArrayList<>();
        for (String uid : userIds)
        {
            statuses.add(getUserCompliance(uid));
        }

        // Count by level
        Map<String, Integer> byLevel = new LinkedHashMap<>();
        byLevel.put("high", 0);
        byLevel.put("medium", 0);
        byLevel.put("low", 0);
        byLevel.put("none", 0);

        for (UserComplianceStatus s : statuses)
        {
            String key = s.getComplianceLevel().getCode();
            byLevel.put(key, byLevel.getOrDefault(key, 0) + 1);
        }

        // Low compliance users
        List<Map<String, Object>> lowUsers = new ArrayList<>();
        for (UserComplianceStatus s : statuses)
        {
            if (s.getComplianceLevel() == ComplianceLevel.LOW)
            {
                Map<String, Object> lowInfo = new HashMap<>();
                lowInfo.put("user_id", s.getUserId());
                lowInfo.put("days_without", s.getDaysWithoutIntervention());
                lowUsers.add(lowInfo);
            }
        }

        // Average compliance rate
        double rateSum = 0;
        int rateCount = 0;
        for (UserComplianceStatus s : statuses)
        {
            if (s.getComplianceRate7d() != null)
            {
                rateSum += s.getComplianceRate7d();
                rateCount++;
            }
        }
        double avgRate = rateCount > 0 ? rateSum / rateCount : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_users", statuses.size());
        summary.put("by_compliance_level", byLevel);
        summary.put("low_compliance_count", byLevel.getOrDefault("low", 0));
        summary.put("low_compliance_users", lowUsers);
        summary.put("compliance_rate_avg", avgRate);

        log.debug("getComplianceSummary: total={}, low={}", statuses.size(), byLevel.get("low"));
        return summary;
    }

    // ========== 7-Day Compliance Rate ==========

    /**
     * 计算7日依从率 / Compute 7-day compliance rate.
     *
     * <p>Uses elapsed days (from first execution in window to now) rather than
     * the fixed window size, so a single execution today gives rate=1.0.
     *
     * @param userId 用户标识 / User to compute rate for
     * @return 依从率 0.0-1.0，无数据时返回null / Compliance rate or null
     */
    private Double computeComplianceRate7d(String userId)
    {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(COMPLIANCE_RATE_WINDOW_DAYS);
        List<LocalDateTime> history = executionHistory.getOrDefault(userId, new ArrayList<>());

        // Count unique days with at least one execution within the window
        Set<LocalDate> execDates = new HashSet<>();
        LocalDate earliestDate = null;

        for (LocalDateTime dt : history)
        {
            if (!dt.isBefore(cutoff))
            {
                LocalDate d = dt.toLocalDate();
                execDates.add(d);
                if (earliestDate == null || d.isBefore(earliestDate))
                {
                    earliestDate = d;
                }
            }
        }

        if (execDates.isEmpty())
        {
            return null;
        }

        // Elapsed days from first execution date to today, capped at window
        LocalDate today = LocalDate.now();
        int elapsedDays = (int) Math.min(COMPLIANCE_RATE_WINDOW_DAYS,
                                         java.time.temporal.ChronoUnit.DAYS.between(earliestDate, today) + 1);

        return Math.min(1.0, (double) execDates.size() / elapsedDays);
    }

    // ========== Feedback Query ==========

    /**
     * 获取用户的反馈历史 / Get feedback history for a user.
     *
     * @param userId       用户标识
     * @param feedbackType 反馈类型过滤（可选）
     * @param since        起始时间过滤（可选）
     * @param limit        最大返回数量
     * @return 反馈条目列表
     */
    public List<FeedbackEntry> getFeedbackHistory(String userId, FeedbackType feedbackType,
                                                   LocalDateTime since, int limit)
    {
        List<FeedbackEntry> entries = feedbackStore.getOrDefault(userId, new ArrayList<>());

        List<FeedbackEntry> filtered = entries.stream()
            .filter(e -> feedbackType == null || e.getFeedbackType() == feedbackType)
            .filter(e -> since == null || toLocalDateTime(e.getTimestamp()).isAfter(since) ||
                         toLocalDateTime(e.getTimestamp()).isEqual(since))
            .collect(Collectors.toList());

        if (filtered.size() > limit)
        {
            filtered = filtered.subList(filtered.size() - limit, filtered.size());
        }

        return filtered;
    }

    // ========== Reset (for testing) ==========

    /**
     * 获取所有被跟踪的用户ID列表 / Get all tracked user IDs.
     *
     * <p>XIN-146: Returns the union of users in lastExecution, executionHistory,
     * and activePrescriptionCounts maps. Used by CoachApiController to list
     * coach-managed users without requiring a separate user store.
     *
     * @return All known user IDs
     */
    public List<String> getTrackedUserIds()
    {
        Set<String> allUsers = new java.util.HashSet<>();
        allUsers.addAll(lastExecution.keySet());
        allUsers.addAll(executionHistory.keySet());
        allUsers.addAll(activePrescriptionCounts.keySet());
        return new ArrayList<>(allUsers);
    }

    /**
     * 重置所有内存存储(测试用) / Reset all in-memory storage (for testing).
     */
    public void resetAll()
    {
        lastExecution.clear();
        executionHistory.clear();
        activePrescriptionCounts.clear();
        feedbackStore.clear();
    }

    // ========== Private Helpers ==========

    /**
     * Convert java.util.Date to LocalDateTime.
     */
    private static LocalDateTime toLocalDateTime(Date date)
    {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
