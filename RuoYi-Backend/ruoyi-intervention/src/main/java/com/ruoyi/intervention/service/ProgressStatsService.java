package com.ruoyi.intervention.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 训练进度统计服务 — 依从率、连续天数、训练量趋势.
 * Computes training progress statistics: compliance rate, streak days,
 * volume trends, and session frequency patterns.
 *
 * <p>Per Board v1.1 Section 3.3:
 * <ul>
 *   <li>Compliance rate = completed sessions / planned sessions</li>
 *   <li>Streak = consecutive days with completed training</li>
 *   <li>Volume trend = weekly training minutes over last 8 weeks</li>
 *   <li>Intensity progression = slope of intensity over last 4 weeks</li>
 * </ul>
 *
 * Ticket: XIN-107
 */
@Service
public class ProgressStatsService
{
    private static final Logger log = LoggerFactory.getLogger(ProgressStatsService.class);

    // ========== In-Memory Storage (Phase 1) ==========

    /** Daily completion records: userId → list of daily records */
    private final Map<String, List<DailyCompletionRecord>> completionHistory = new LinkedHashMap<>();

    // ========== Daily Completion Record ==========

    /**
     * 每日完成记录 / Daily completion record.
     */
    public static class DailyCompletionRecord
    {
        private LocalDate date;
        private boolean planned;
        private boolean completed;
        private int durationMinutes;
        private String activityType;
        private double intensityScore;  // 0-100

        public DailyCompletionRecord() {}

        public DailyCompletionRecord(LocalDate date, boolean planned, boolean completed,
                                      int durationMinutes, String activityType, double intensityScore)
        {
            this.date = date;
            this.planned = planned;
            this.completed = completed;
            this.durationMinutes = durationMinutes;
            this.activityType = activityType;
            this.intensityScore = intensityScore;
        }

        // Getters & Setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public boolean isPlanned() { return planned; }
        public void setPlanned(boolean planned) { this.planned = planned; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getActivityType() { return activityType; }
        public void setActivityType(String activityType) { this.activityType = activityType; }
        public double getIntensityScore() { return intensityScore; }
        public void setIntensityScore(double intensityScore) { this.intensityScore = intensityScore; }
    }

    // ========== Data Input ==========

    /**
     * 记录每日完成 / Record a daily completion.
     */
    public void recordCompletion(String userId, DailyCompletionRecord record)
    {
        completionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(record);
        log.debug("recordCompletion: userId={}, date={}, completed={}", userId, record.getDate(), record.isCompleted());
    }

    /**
     * 批量记录 / Record multiple completions.
     */
    public void recordCompletions(String userId, List<DailyCompletionRecord> records)
    {
        completionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).addAll(records);
    }

    // ========== Compliance Rate ==========

    /**
     * 计算依从率 / Calculate compliance rate over a period.
     *
     * <p>Compliance = completed sessions / planned sessions (trailing N days).
     *
     * @param userId   用户ID
     * @param trailing 回溯天数
     * @return compliance rate 0.0-1.0
     */
    public double complianceRate(String userId, int trailing)
    {
        List<DailyCompletionRecord> records = getRecordsForPeriod(userId, trailing);
        if (records.isEmpty()) return 0.0;

        long planned = records.stream().filter(DailyCompletionRecord::isPlanned).count();
        if (planned == 0) return 0.0;

        long completed = records.stream()
            .filter(r -> r.isPlanned() && r.isCompleted())
            .count();

        return round3((double) completed / planned);
    }

    /**
     * 获取依从率详细统计 / Get detailed compliance statistics.
     */
    public Map<String, Object> complianceDetail(String userId, int trailing)
    {
        Map<String, Object> detail = new LinkedHashMap<>();
        List<DailyCompletionRecord> records = getRecordsForPeriod(userId, trailing);

        long planned = records.stream().filter(DailyCompletionRecord::isPlanned).count();
        long completed = records.stream()
            .filter(r -> r.isPlanned() && r.isCompleted()).count();
        long missed = planned - completed;

        detail.put("periodDays", trailing);
        detail.put("plannedSessions", planned);
        detail.put("completedSessions", completed);
        detail.put("missedSessions", missed);
        detail.put("complianceRate", planned > 0 ? round3((double) completed / planned) : 0.0);
        detail.put("grade", complianceGrade(planned > 0 ? (double) completed / planned : 0.0));

        return detail;
    }

    // ========== Streak Days ==========

    /**
     * 计算连续训练天数 / Calculate current training streak.
     *
     * <p>Streak = consecutive days (ending today or yesterday) where training was completed.
     *
     * @param userId 用户ID
     * @return 当前连续天数
     */
    public int currentStreak(String userId)
    {
        List<DailyCompletionRecord> records = completionHistory.getOrDefault(userId, Collections.emptyList());
        if (records.isEmpty()) return 0;

        // Sort by date descending
        List<DailyCompletionRecord> sorted = new ArrayList<>(records);
        sorted.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Check if today or yesterday has a completed session
        if (sorted.isEmpty()) return 0;

        DailyCompletionRecord latest = sorted.get(0);
        if (!latest.getDate().equals(today) && !latest.getDate().equals(yesterday))
        {
            return 0; // Streak broken — no recent training
        }

        int streak = 0;
        LocalDate expectedDate = latest.getDate();

        for (DailyCompletionRecord r : sorted)
        {
            if (r.getDate().equals(expectedDate) && r.isCompleted())
            {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            }
            else if (r.getDate().isBefore(expectedDate))
            {
                break; // Gap found
            }
        }

        return streak;
    }

    /**
     * 计算最长连续天数 / Calculate the longest training streak.
     */
    public int longestStreak(String userId)
    {
        List<DailyCompletionRecord> records = completionHistory.getOrDefault(userId, Collections.emptyList());
        if (records.isEmpty()) return 0;

        List<DailyCompletionRecord> sorted = new ArrayList<>(records);
        sorted.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate prevDate = null;

        for (DailyCompletionRecord r : sorted)
        {
            if (r.isCompleted())
            {
                if (prevDate == null || r.getDate().equals(prevDate.plusDays(1)))
                {
                    currentStreak++;
                }
                else
                {
                    currentStreak = 1;
                }
                maxStreak = Math.max(maxStreak, currentStreak);
                prevDate = r.getDate();
            }
            else
            {
                currentStreak = 0;
                prevDate = r.getDate();
            }
        }

        return maxStreak;
    }

    // ========== Volume Trends ==========

    /**
     * 计算训练量趋势 / Calculate weekly training volume trend.
     *
     * @param userId     用户ID
     * @param numWeeks   回溯周数
     * @return 周训练量列表 (earliest first)
     */
    public List<Map<String, Object>> volumeTrend(String userId, int numWeeks)
    {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int w = numWeeks - 1; w >= 0; w--)
        {
            LocalDate weekStart = today.minusWeeks(w);
            LocalDate weekEnd = weekStart.plusDays(6);

            List<DailyCompletionRecord> weekRecords = getRecordsForDateRange(userId, weekStart, weekEnd);

            int totalMinutes = weekRecords.stream()
                .filter(DailyCompletionRecord::isCompleted)
                .mapToInt(DailyCompletionRecord::getDurationMinutes)
                .sum();

            long completedDays = weekRecords.stream()
                .filter(DailyCompletionRecord::isCompleted)
                .count();

            Map<String, Object> week = new LinkedHashMap<>();
            week.put("weekStart", weekStart.toString());
            week.put("weekEnd", weekEnd.toString());
            week.put("totalMinutes", totalMinutes);
            week.put("completedDays", completedDays);
            week.put("avgDailyMinutes", completedDays > 0 ? round1((double) totalMinutes / completedDays) : 0.0);
            trend.add(week);
        }

        return trend;
    }

    /**
     * 计算强度进展趋势 / Calculate intensity progression slope.
     *
     * @param userId    用户ID
     * @param trailing  回溯天数
     * @return slope (positive = improving, negative = declining)
     */
    public double intensitySlope(String userId, int trailing)
    {
        List<DailyCompletionRecord> records = getRecordsForPeriod(userId, trailing);
        List<DailyCompletionRecord> completedOnly = records.stream()
            .filter(DailyCompletionRecord::isCompleted)
            .collect(java.util.stream.Collectors.toList());

        if (completedOnly.size() < 3) return 0.0;

        // Simple linear regression: y = intensityScore, x = day index
        int n = completedOnly.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++)
        {
            double x = i;
            double y = completedOnly.get(i).getIntensityScore();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return 0.0;

        return round3((n * sumXY - sumX * sumY) / denominator);
    }

    // ========== Comprehensive Stats ==========

    /**
     * 获取用户完整进度统计 / Get comprehensive progress stats for a user.
     *
     * @param userId 用户ID
     * @return 统计Map
     */
    public Map<String, Object> getFullStats(String userId)
    {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Compliance (trailing 28 days = 4 weeks)
        stats.put("compliance4Week", complianceDetail(userId, 28));
        stats.put("compliance7Day", complianceRate(userId, 7));

        // Streaks
        stats.put("currentStreak", currentStreak(userId));
        stats.put("longestStreak", longestStreak(userId));

        // Volume trend (last 8 weeks)
        stats.put("volumeTrend8Weeks", volumeTrend(userId, 8));

        // Intensity progression (last 28 days)
        stats.put("intensitySlope4Week", intensitySlope(userId, 28));

        // Summary
        Map<String, Object> compliance4 = (Map<String, Object>) stats.get("compliance4Week");
        double rate = compliance4 != null ? ((Number) compliance4.getOrDefault("complianceRate", 0.0)).doubleValue() : 0.0;

        String trendLabel;
        double slope = (double) stats.get("intensitySlope4Week");
        if (slope > 0.5) trendLabel = "improving";
        else if (slope > -0.5) trendLabel = "stable";
        else trendLabel = "declining";

        stats.put("overallTrend", trendLabel);
        stats.put("isOnTrack", rate >= 0.7 && !"declining".equals(trendLabel));

        log.debug("getFullStats: userId={}, compliance={:.0%}, streak={}, trend={}",
                  userId, rate, stats.get("currentStreak"), trendLabel);
        return stats;
    }

    // ========== Internal Helpers ==========

    private List<DailyCompletionRecord> getRecordsForPeriod(String userId, int trailingDays)
    {
        LocalDate cutoff = LocalDate.now().minusDays(trailingDays);
        List<DailyCompletionRecord> all = completionHistory.getOrDefault(userId, Collections.emptyList());

        List<DailyCompletionRecord> result = new ArrayList<>();
        for (DailyCompletionRecord r : all)
        {
            if (r.getDate() != null && !r.getDate().isBefore(cutoff))
            {
                result.add(r);
            }
        }
        result.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return result;
    }

    private List<DailyCompletionRecord> getRecordsForDateRange(String userId, LocalDate from, LocalDate to)
    {
        List<DailyCompletionRecord> all = completionHistory.getOrDefault(userId, Collections.emptyList());
        List<DailyCompletionRecord> result = new ArrayList<>();
        for (DailyCompletionRecord r : all)
        {
            if (r.getDate() != null && !r.getDate().isBefore(from) && !r.getDate().isAfter(to))
            {
                result.add(r);
            }
        }
        return result;
    }

    private String complianceGrade(double rate)
    {
        if (rate >= 0.9) return "A";
        if (rate >= 0.8) return "B+";
        if (rate >= 0.7) return "B";
        if (rate >= 0.6) return "C+";
        if (rate >= 0.5) return "C";
        if (rate >= 0.3) return "D";
        return "F";
    }

    private static double round1(double val) { return Math.round(val * 10.0) / 10.0; }
    private static double round3(double val) { return Math.round(val * 1000.0) / 1000.0; }
}
