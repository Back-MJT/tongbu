package com.ruoyi.intervention.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ruoyi.intervention.domain.model.DeviceData;

/**
 * IMU数据聚合服务 — 将原始IMU传感器数据聚合为训练摘要.
 * Aggregates raw IMU/device data into training summaries for the AI engine.
 *
 * <p>Per Board v1.1 Layer 1-2:
 * <ul>
 *   <li>Raw IMU (accel/gyro at 50Hz) → session-level summaries</li>
 *   <li>Heart rate statistics (min/avg/max)</li>
 *   <li>Step count aggregation</li>
 *   <li>Calorie estimation</li>
 *   <li>Activity classification (sedentary/walking/running/strength)</li>
 *   <li>Readiness score for daily task generation</li>
 * </ul>
 *
 * <p>Phase 1: in-memory aggregation. Phase 2: SQL-based aggregation from PostgreSQL.
 *
 * Ticket: XIN-107
 */
@Service
public class ImuDataAggregationService
{
    private static final Logger log = LoggerFactory.getLogger(ImuDataAggregationService.class);

    // ========== Activity Classification Thresholds ==========
    // Based on acceleration magnitude (m/s^2)

    /** Sedentary: accel magnitude < 1.3g */
    private static final double SEDENTARY_THRESHOLD = 1.3;
    /** Walking: accel magnitude 1.3-2.0g */
    private static final double WALKING_THRESHOLD = 2.0;
    /** Running: accel magnitude 2.0-3.5g */
    private static final double RUNNING_THRESHOLD = 3.5;
    /** Strength training: high gyro variance + moderate accel */

    // ========== In-Memory Storage (Phase 1) ==========
    private final List<DeviceData> rawDataBuffer = new ArrayList<>();

    // ========== Session Aggregation ==========

    /**
     * 聚合训练会话数据 / Aggregate device data into a session summary.
     *
     * <p>Takes a list of DeviceData points (typically from one workout session)
     * and computes summary statistics.
     *
     * @param dataPoints IMU data points for the session
     * @return Session summary map
     */
    public Map<String, Object> aggregateSession(List<DeviceData> dataPoints)
    {
        if (dataPoints == null || dataPoints.isEmpty())
        {
            return emptySessionSummary();
        }

        Map<String, Object> summary = new LinkedHashMap<>();

        // Time range
        LocalDateTime startTime = dataPoints.get(0).getTimestamp();
        LocalDateTime endTime = dataPoints.get(dataPoints.size() - 1).getTimestamp();
        long durationMinutes = startTime != null && endTime != null
            ? Duration.between(startTime, endTime).toMinutes() : 0;

        summary.put("startTime", startTime != null ? startTime.toString() : null);
        summary.put("endTime", endTime != null ? endTime.toString() : null);
        summary.put("durationMinutes", durationMinutes);
        summary.put("dataPointCount", dataPoints.size());

        // Acceleration statistics
        DoubleSummaryStatistics accelStats = dataPoints.stream()
            .mapToDouble(DeviceData::accelerationMagnitude)
            .summaryStatistics();
        summary.put("accelMin", round2(accelStats.getMin()));
        summary.put("accelAvg", round2(accelStats.getAverage()));
        summary.put("accelMax", round2(accelStats.getMax()));

        // Heart rate statistics
        List<Integer> hrValues = new ArrayList<>();
        for (DeviceData d : dataPoints)
        {
            if (d.getHeartRate() != null && d.getHeartRate() > 0)
            {
                hrValues.add(d.getHeartRate());
            }
        }
        if (!hrValues.isEmpty())
        {
            DoubleSummaryStatistics hrStats = hrValues.stream()
                .mapToDouble(Integer::doubleValue)
                .summaryStatistics();
            summary.put("heartRateMin", (int) hrStats.getMin());
            summary.put("heartRateAvg", (int) hrStats.getAverage());
            summary.put("heartRateMax", (int) hrStats.getMax());
        }

        // Step count (last value is cumulative)
        DeviceData lastPoint = dataPoints.get(dataPoints.size() - 1);
        DeviceData firstPoint = dataPoints.get(0);
        int stepsThisSession = 0;
        if (lastPoint.getStepCount() != null && firstPoint.getStepCount() != null)
        {
            stepsThisSession = lastPoint.getStepCount() - firstPoint.getStepCount();
        }
        else if (lastPoint.getStepCount() != null)
        {
            stepsThisSession = lastPoint.getStepCount();
        }
        summary.put("steps", stepsThisSession);

        // Calories
        double totalCalories = dataPoints.stream()
            .mapToDouble(d -> d.getCaloriesBurned() != null ? d.getCaloriesBurned() : 0)
            .sum();
        summary.put("caloriesBurned", round2(totalCalories));

        // Rep count
        int totalReps = dataPoints.stream()
            .mapToInt(d -> d.getRepCount() != null ? d.getRepCount() : 0)
            .sum();
        summary.put("repCount", totalReps);

        // Activity classification
        String activityType = classifyActivity(accelStats.getAverage());
        summary.put("activityType", activityType);

        // Intensity level
        String intensity = classifyIntensity(accelStats.getAverage(), hrValues);
        summary.put("intensity", intensity);

        // Device info
        summary.put("deviceId", firstPoint.getDeviceId());
        summary.put("userId", firstPoint.getUserId());

        log.debug("aggregateSession: {} points, {}min, type={}, intensity={}",
                  dataPoints.size(), durationMinutes, activityType, intensity);
        return summary;
    }

    // ========== Daily Aggregation ==========

    /**
     * 聚合每日训练数据 / Aggregate all sessions for a day into a daily summary.
     *
     * @param sessions List of session summaries (from aggregateSession)
     * @return Daily summary map
     */
    public Map<String, Object> aggregateDaily(List<Map<String, Object>> sessions)
    {
        Map<String, Object> daily = new LinkedHashMap<>();

        if (sessions == null || sessions.isEmpty())
        {
            daily.put("totalSessions", 0);
            daily.put("totalDurationMinutes", 0L);
            daily.put("totalSteps", 0);
            daily.put("totalCalories", 0.0);
            daily.put("completedPlan", false);
            return daily;
        }

        long totalDuration = 0;
        int totalSteps = 0;
        double totalCalories = 0.0;
        List<Integer> allHr = new ArrayList<>();
        String dominantActivity = "";
        double maxAvgAccel = 0;

        for (Map<String, Object> session : sessions)
        {
            totalDuration += ((Number) session.getOrDefault("durationMinutes", 0)).longValue();
            totalSteps += ((Number) session.getOrDefault("steps", 0)).intValue();
            totalCalories += ((Number) session.getOrDefault("caloriesBurned", 0)).doubleValue();

            if (session.containsKey("heartRateAvg"))
            {
                allHr.add(((Number) session.get("heartRateAvg")).intValue());
            }

            double avgAccel = ((Number) session.getOrDefault("accelAvg", 0)).doubleValue();
            if (avgAccel > maxAvgAccel)
            {
                maxAvgAccel = avgAccel;
                dominantActivity = (String) session.getOrDefault("activityType", "sedentary");
            }
        }

        daily.put("totalSessions", sessions.size());
        daily.put("totalDurationMinutes", totalDuration);
        daily.put("totalSteps", totalSteps);
        daily.put("totalCalories", round2(totalCalories));
        daily.put("dominantActivity", dominantActivity);

        if (!allHr.isEmpty())
        {
            daily.put("avgHeartRate", (int) allHr.stream().mapToInt(Integer::intValue).average().orElse(0));
        }

        // Daily readiness: based on duration (>=30min = good, >=15min = moderate)
        double readiness = Math.min(100.0, totalDuration >= 30 ? 80 + Math.min(20, totalSteps / 500.0)
                              : totalDuration >= 15 ? 50 + totalDuration : totalDuration * 3);
        daily.put("readinessScore", round2(readiness));

        // Completed plan: >=30min or >=1 session with moderate+ intensity
        boolean completedPlan = totalDuration >= 30 || sessions.size() >= 1;
        daily.put("completedPlan", completedPlan);

        log.debug("aggregateDaily: {} sessions, {}min, {} steps, readiness={}",
                  sessions.size(), totalDuration, totalSteps, round2(readiness));
        return daily;
    }

    // ========== Readiness Score ==========

    /**
     * 计算训练准备度评分 / Calculate readiness score for daily task generation.
     *
     * <p>Factors:
     * <ul>
     *   <li>Resting heart rate (lower = better recovery)</li>
     *   <li>Previous day activity level</li>
     *   <li>Days since last training</li>
     *   <li>Step count trend</li>
     * </ul>
     *
     * @param restingHr     静息心率 (nullable)
     * @param yesterdaySteps 昨日步数 (nullable)
     * @param daysSinceLast  距上次训练天数
     * @param avgWeeklyFreq  近4周平均训练频率
     * @return readiness score 0-100
     */
    public double calculateReadinessScore(Integer restingHr, Integer yesterdaySteps,
                                           int daysSinceLast, double avgWeeklyFreq)
    {
        double score = 70.0; // baseline

        // Heart rate factor (lower resting HR = better)
        if (restingHr != null)
        {
            if (restingHr < 60) score += 15;       // athlete-level
            else if (restingHr < 70) score += 10;   // good
            else if (restingHr < 80) score += 0;    // normal
            else score -= 10;                        // elevated, may need rest
        }

        // Yesterday's activity
        if (yesterdaySteps != null)
        {
            if (yesterdaySteps > 10000) score += 5;
            else if (yesterdaySteps < 3000) score += 10; // rested = good
            else score += 3;
        }

        // Recovery: days since last session
        if (daysSinceLast == 0) score -= 15;      // trained today already
        else if (daysSinceLast == 1) score += 5;   // 1 day rest = ideal
        else if (daysSinceLast <= 3) score += 0;   // moderate rest
        else if (daysSinceLast <= 7) score -= 5;   // some detraining
        else score -= 15;                           // significant break

        // Frequency factor
        if (avgWeeklyFreq >= 3) score += 5;
        else if (avgWeeklyFreq >= 2) score += 3;

        return round2(Math.max(0, Math.min(100, score)));
    }

    // ========== Data Buffer Management ==========

    /**
     * 添加原始数据到缓冲区 / Add raw device data to the buffer.
     */
    public void addRawData(DeviceData data)
    {
        if (data != null)
        {
            rawDataBuffer.add(data);
        }
    }

    /**
     * 获取指定时间范围的原始数据 / Get raw data for a time range.
     */
    public List<DeviceData> getRawDataInRange(LocalDateTime from, LocalDateTime to)
    {
        List<DeviceData> result = new ArrayList<>();
        for (DeviceData d : rawDataBuffer)
        {
            if (d.getTimestamp() != null
                && !d.getTimestamp().isBefore(from)
                && !d.getTimestamp().isAfter(to))
            {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * 获取用户指定日期的原始数据 / Get raw data for a user on a specific date.
     */
    public List<DeviceData> getRawDataForUserOnDate(String userId, LocalDate date)
    {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<DeviceData> result = new ArrayList<>();
        for (DeviceData d : rawDataBuffer)
        {
            if (userId.equals(d.getUserId())
                && d.getTimestamp() != null
                && !d.getTimestamp().isBefore(startOfDay)
                && !d.getTimestamp().isAfter(endOfDay))
            {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * 清除过期数据 / Clear data older than specified days.
     */
    public int clearOldData(int olderThanDays)
    {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        int before = rawDataBuffer.size();
        rawDataBuffer.removeIf(d -> d.getTimestamp() != null && d.getTimestamp().isBefore(cutoff));
        int cleared = before - rawDataBuffer.size();
        if (cleared > 0)
        {
            log.debug("clearOldData: removed {} entries older than {} days", cleared, olderThanDays);
        }
        return cleared;
    }

    // ========== Classification Helpers ==========

    /**
     * 根据加速度幅值分类活动类型 / Classify activity type from average acceleration magnitude.
     */
    private String classifyActivity(double avgAccelMagnitude)
    {
        // avgAccelMagnitude is in g (gravity units)
        if (avgAccelMagnitude < SEDENTARY_THRESHOLD) return "sedentary";
        if (avgAccelMagnitude < WALKING_THRESHOLD) return "walking";
        if (avgAccelMagnitude < RUNNING_THRESHOLD) return "running";
        return "vigorous";  // strength training, HIIT, etc.
    }

    /**
     * 根据加速度和心率分类强度 / Classify exercise intensity.
     */
    private String classifyIntensity(double avgAccelMagnitude, List<Integer> hrValues)
    {
        double avgHr = hrValues.isEmpty() ? 0 : hrValues.stream().mapToInt(Integer::intValue).average().orElse(0);

        if (avgHr > 0)
        {
            // Heart rate based (more accurate)
            if (avgHr < 100) return "light";
            if (avgHr < 130) return "moderate";
            if (avgHr < 160) return "vigorous";
            return "very_vigorous";
        }

        // Fallback: acceleration based
        if (avgAccelMagnitude < 1.5) return "light";
        if (avgAccelMagnitude < 2.5) return "moderate";
        if (avgAccelMagnitude < 3.5) return "vigorous";
        return "very_vigorous";
    }

    // ========== Utility ==========

    private static double round2(double val)
    {
        return Math.round(val * 100.0) / 100.0;
    }

    private static Map<String, Object> emptySessionSummary()
    {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("dataPointCount", 0);
        empty.put("durationMinutes", 0L);
        empty.put("activityType", "none");
        empty.put("intensity", "none");
        return empty;
    }
}
