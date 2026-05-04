package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.model.DeviceData;

/**
 * JUnit 5 tests for ImuDataAggregationService.
 *
 * Covers:
 *   - aggregateSession: full data, empty/null input, single point
 *   - aggregateDaily: multi-session, empty input, readiness score calculation
 *   - calculateReadinessScore: resting HR, yesterday steps, days since last, frequency
 *   - Activity classification: sedentary/walking/running/vigorous thresholds
 *   - Intensity classification: HR-based and acceleration fallback
 *   - Buffer management: addRawData, getRawDataInRange, getRawDataForUserOnDate, clearOldData
 *   - Edge cases: null fields, boundary values, cumulative step counts
 *
 * Thresholds per ImuDataAggregationService source:
 *   - Sedentary: avgAccelMagnitude < 1.3g
 *   - Walking: 1.3g <= avg < 2.0g
 *   - Running: 2.0g <= avg < 3.5g
 *   - Vigorous: avg >= 3.5g
 *
 * Intensity (HR-based):
 *   - Light: avgHR < 100
 *   - Moderate: 100 <= avgHR < 130
 *   - Vigorous: 130 <= avgHR < 160
 *   - Very vigorous: avgHR >= 160
 *
 * Ticket: XIN-107 algorithm validation
 */
class ImuDataAggregationServiceTest
{
    private ImuDataAggregationService svc;

    @BeforeEach
    void setUp()
    {
        svc = new ImuDataAggregationService();
    }

    // ========== Fixtures ==========

    private static DeviceData makePoint(String deviceId, String userId,
                                         LocalDateTime ts, double ax, double ay, double az,
                                         Integer hr, Integer steps, Double calories, Integer reps)
    {
        DeviceData d = new DeviceData();
        d.setDeviceId(deviceId);
        d.setUserId(userId);
        d.setTimestamp(ts);
        d.setAccelX(ax);
        d.setAccelY(ay);
        d.setAccelZ(az);
        d.setHeartRate(hr);
        d.setStepCount(steps);
        d.setCaloriesBurned(calories);
        d.setRepCount(reps);
        return d;
    }

    /** Create a list of N data points at 1-second intervals starting from baseTime. */
    private static List<DeviceData> makeSessionData(LocalDateTime baseTime, int count,
                                                     double accelMag, Integer hr,
                                                     int startSteps, int endSteps,
                                                     double caloriesPerPoint, int repsPerPoint)
    {
        List<DeviceData> points = new ArrayList<>();
        // Distribute accelMag evenly across X axis (Y=Z=0) for simplicity
        for (int i = 0; i < count; i++)
        {
            // Linearly interpolate steps
            int steps = startSteps + (endSteps - startSteps) * i / Math.max(1, count - 1);
            points.add(makePoint(
                "DEV-001", "USER-A",
                baseTime.plusSeconds(i),
                accelMag, 0.0, 0.0,
                hr, steps,
                caloriesPerPoint, repsPerPoint
            ));
        }
        return points;
    }

    // ========== aggregateSession ==========

    @Nested
    @DisplayName("aggregateSession")
    class AggregateSessionTests
    {
        @Test
        @DisplayName("Null input returns empty summary")
        void nullInput()
        {
            Map<String, Object> result = svc.aggregateSession(null);
            assertEquals(0, result.get("dataPointCount"));
            assertEquals(0L, result.get("durationMinutes"));
            assertEquals("none", result.get("activityType"));
        }

        @Test
        @DisplayName("Empty list returns empty summary")
        void emptyInput()
        {
            Map<String, Object> result = svc.aggregateSession(Collections.emptyList());
            assertEquals(0, result.get("dataPointCount"));
        }

        @Test
        @DisplayName("Single data point — zero duration, all stats from one sample")
        void singlePoint()
        {
            List<DeviceData> data = List.of(
                makePoint("D1", "U1", LocalDateTime.of(2026, 4, 16, 10, 0, 0),
                          1.5, 0, 0, 75, 100, 5.0, 10)
            );
            Map<String, Object> result = svc.aggregateSession(data);

            assertEquals(1, result.get("dataPointCount"));
            assertEquals(0L, result.get("durationMinutes"));
            // accelMag = sqrt(1.5^2) = 1.5
            assertEquals(1.5, (double) result.get("accelAvg"), 0.01);
            assertEquals(1.5, (double) result.get("accelMin"), 0.01);
            assertEquals(1.5, (double) result.get("accelMax"), 0.01);
            // Single point: no step delta (both first and last have 100)
            assertEquals(0, result.get("steps"));
            assertEquals(5.0, (double) result.get("caloriesBurned"), 0.01);
            assertEquals(10, result.get("repCount"));
        }

        @Test
        @DisplayName("Multi-point session with duration and step delta")
        void multiPointSession()
        {
            LocalDateTime base = LocalDateTime.of(2026, 4, 16, 10, 0, 0);
            // 10 points over 9 seconds, accel magnitude ~2.0 (walking)
            List<DeviceData> data = makeSessionData(base, 10, 2.0, 110, 0, 500, 2.5, 3);

            Map<String, Object> result = svc.aggregateSession(data);

            assertEquals(10, result.get("dataPointCount"));
            assertEquals(0L, result.get("durationMinutes")); // < 1 minute
            // Steps: last(500) - first(0) = 500
            assertEquals(500, result.get("steps"));
            // Calories: 10 * 2.5 = 25.0
            assertEquals(25.0, (double) result.get("caloriesBurned"), 0.01);
            // Reps: 10 * 3 = 30
            assertEquals(30, result.get("repCount"));
            // Activity: avgAccel = 2.0, which is at walking/running boundary
            // 2.0 >= WALKING_THRESHOLD -> "running"
            assertEquals("running", result.get("activityType"));
            // HR avg=110, 100<=110<130 -> moderate
            assertEquals("moderate", result.get("intensity"));
            // HR stats
            assertEquals(110, result.get("heartRateAvg"));
        }

        @Test
        @DisplayName("Session with 5-minute duration")
        void fiveMinuteSession()
        {
            LocalDateTime base = LocalDateTime.of(2026, 4, 16, 10, 0, 0);
            LocalDateTime end = base.plusMinutes(5);
            // Start and end point
            List<DeviceData> data = List.of(
                makePoint("D1", "U1", base, 1.0, 0, 0, 65, 0, 1.0, 0),
                makePoint("D1", "U1", end, 1.0, 0, 0, 85, 300, 3.0, 15)
            );

            Map<String, Object> result = svc.aggregateSession(data);
            assertEquals(5L, result.get("durationMinutes"));
            assertEquals(300, result.get("steps"));
            // Heart rate: min=65, max=85, avg=75
            assertEquals(65, result.get("heartRateMin"));
            assertEquals(85, result.get("heartRateMax"));
            assertEquals(75, result.get("heartRateAvg"));
        }

        @Test
        @DisplayName("Device info preserved in summary")
        void deviceInfoPreserved()
        {
            List<DeviceData> data = List.of(
                makePoint("DEV-X", "USER-Y", LocalDateTime.now(), 1.0, 0, 0, null, null, null, null)
            );
            Map<String, Object> result = svc.aggregateSession(data);
            assertEquals("DEV-X", result.get("deviceId"));
            assertEquals("USER-Y", result.get("userId"));
        }

        @Test
        @DisplayName("Points with null/zero heart rate are excluded from HR stats")
        void nullHeartRateHandling()
        {
            LocalDateTime base = LocalDateTime.of(2026, 4, 16, 10, 0, 0);
            List<DeviceData> data = List.of(
                makePoint("D1", "U1", base, 1.0, 0, 0, null, null, null, null),
                makePoint("D1", "U1", base.plusSeconds(1), 1.0, 0, 0, 0, null, null, null),  // HR=0 excluded
                makePoint("D1", "U1", base.plusSeconds(2), 1.0, 0, 0, 90, null, null, null)
            );
            Map<String, Object> result = svc.aggregateSession(data);
            // Only 1 valid HR point (90) — min=max=avg=90
            assertEquals(90, result.get("heartRateAvg"));
            assertEquals(90, result.get("heartRateMin"));
            assertEquals(90, result.get("heartRateMax"));
        }

        @Test
        @DisplayName("Step count falls back to last point when first is null")
        void stepCountFallback()
        {
            LocalDateTime base = LocalDateTime.of(2026, 4, 16, 10, 0, 0);
            List<DeviceData> data = List.of(
                makePoint("D1", "U1", base, 1.0, 0, 0, null, null, null, null),
                makePoint("D1", "U1", base.plusSeconds(1), 1.0, 0, 0, null, 200, null, null)
            );
            Map<String, Object> result = svc.aggregateSession(data);
            // First stepCount is null, last is 200 -> fallback to last=200
            assertEquals(200, result.get("steps"));
        }
    }

    // ========== Activity Classification ==========

    @Nested
    @DisplayName("Activity classification via aggregateSession")
    class ActivityClassificationTests
    {
        private Map<String, Object> classify(double accelMag)
        {
            List<DeviceData> data = List.of(
                makePoint("D1", "U1", LocalDateTime.now(), accelMag, 0, 0, null, null, null, null)
            );
            return svc.aggregateSession(data);
        }

        @Test
        @DisplayName("Sedentary: accel < 1.3g")
        void sedentary()
        {
            assertEquals("sedentary", classify(1.0).get("activityType"));
            assertEquals("sedentary", classify(1.29).get("activityType"));
        }

        @Test
        @DisplayName("Walking: 1.3g <= accel < 2.0g")
        void walking()
        {
            assertEquals("walking", classify(1.3).get("activityType"));
            assertEquals("walking", classify(1.5).get("activityType"));
            assertEquals("walking", classify(1.99).get("activityType"));
        }

        @Test
        @DisplayName("Running: 2.0g <= accel < 3.5g")
        void running()
        {
            assertEquals("running", classify(2.0).get("activityType"));
            assertEquals("running", classify(2.75).get("activityType"));
            assertEquals("running", classify(3.49).get("activityType"));
        }

        @Test
        @DisplayName("Vigorous: accel >= 3.5g")
        void vigorous()
        {
            assertEquals("vigorous", classify(3.5).get("activityType"));
            assertEquals("vigorous", classify(5.0).get("activityType"));
        }
    }

    // ========== Intensity Classification ==========

    @Nested
    @DisplayName("Intensity classification via aggregateSession")
    class IntensityClassificationTests
    {
        private Map<String, Object> classifyIntensity(double accelMag, Integer hr)
        {
            List<DeviceData> data = List.of(
                makePoint("D1", "U1", LocalDateTime.now(), accelMag, 0, 0, hr, null, null, null)
            );
            return svc.aggregateSession(data);
        }

        @Test
        @DisplayName("HR-based: light (<100 bpm)")
        void lightHR()
        {
            assertEquals("light", classifyIntensity(2.0, 80).get("intensity"));
        }

        @Test
        @DisplayName("HR-based: moderate (100-129 bpm)")
        void moderateHR()
        {
            assertEquals("moderate", classifyIntensity(2.0, 120).get("intensity"));
        }

        @Test
        @DisplayName("HR-based: vigorous (130-159 bpm)")
        void vigorousHR()
        {
            assertEquals("vigorous", classifyIntensity(2.0, 145).get("intensity"));
        }

        @Test
        @DisplayName("HR-based: very vigorous (>=160 bpm)")
        void veryVigorousHR()
        {
            assertEquals("very_vigorous", classifyIntensity(2.0, 170).get("intensity"));
        }

        @Test
        @DisplayName("Acceleration fallback when no HR")
        void accelFallback()
        {
            assertEquals("light", classifyIntensity(1.0, null).get("intensity"));
            assertEquals("moderate", classifyIntensity(2.0, null).get("intensity"));
            assertEquals("vigorous", classifyIntensity(3.0, null).get("intensity"));
            assertEquals("very_vigorous", classifyIntensity(4.0, null).get("intensity"));
        }
    }

    // ========== aggregateDaily ==========

    @Nested
    @DisplayName("aggregateDaily")
    class AggregateDailyTests
    {
        @Test
        @DisplayName("Null input returns zero summary")
        void nullInput()
        {
            Map<String, Object> result = svc.aggregateDaily(null);
            assertEquals(0, result.get("totalSessions"));
            assertEquals(0L, result.get("totalDurationMinutes"));
            assertFalse((boolean) result.get("completedPlan"));
        }

        @Test
        @DisplayName("Empty list returns zero summary")
        void emptyInput()
        {
            Map<String, Object> result = svc.aggregateDaily(Collections.emptyList());
            assertEquals(0, result.get("totalSessions"));
        }

        @Test
        @DisplayName("Single session aggregates correctly")
        void singleSession()
        {
            List<Map<String, Object>> sessions = new ArrayList<>();
            Map<String, Object> s1 = new java.util.LinkedHashMap<>();
            s1.put("durationMinutes", 30L);
            s1.put("steps", 3000);
            s1.put("caloriesBurned", 150.0);
            s1.put("heartRateAvg", 120);
            s1.put("accelAvg", 2.5);
            s1.put("activityType", "running");
            sessions.add(s1);

            Map<String, Object> result = svc.aggregateDaily(sessions);
            assertEquals(1, result.get("totalSessions"));
            assertEquals(30L, result.get("totalDurationMinutes"));
            assertEquals(3000, result.get("totalSteps"));
            assertEquals(150.0, (double) result.get("totalCalories"), 0.01);
            assertEquals(120, result.get("avgHeartRate"));
            assertEquals("running", result.get("dominantActivity"));
            assertTrue((boolean) result.get("completedPlan"));
        }

        @Test
        @DisplayName("Multiple sessions — dominant activity is highest avg accel")
        void dominantActivitySelection()
        {
            List<Map<String, Object>> sessions = new ArrayList<>();

            Map<String, Object> s1 = new java.util.LinkedHashMap<>();
            s1.put("durationMinutes", 10L);
            s1.put("steps", 500);
            s1.put("caloriesBurned", 30.0);
            s1.put("accelAvg", 1.2);
            s1.put("activityType", "sedentary");
            sessions.add(s1);

            Map<String, Object> s2 = new java.util.LinkedHashMap<>();
            s2.put("durationMinutes", 25L);
            s2.put("steps", 2000);
            s2.put("caloriesBurned", 120.0);
            s2.put("accelAvg", 3.0);
            s2.put("activityType", "running");
            sessions.add(s2);

            Map<String, Object> result = svc.aggregateDaily(sessions);
            assertEquals("running", result.get("dominantActivity"));
            assertEquals(35L, result.get("totalDurationMinutes"));
            assertEquals(2500, result.get("totalSteps"));
        }

        @Test
        @DisplayName("Readiness score: 30+ min = good range (80+)")
        void readinessGoodRange()
        {
            List<Map<String, Object>> sessions = new ArrayList<>();
            Map<String, Object> s1 = new java.util.LinkedHashMap<>();
            s1.put("durationMinutes", 30L);
            s1.put("steps", 500);
            s1.put("caloriesBurned", 50.0);
            s1.put("accelAvg", 2.0);
            s1.put("activityType", "walking");
            sessions.add(s1);

            Map<String, Object> result = svc.aggregateDaily(sessions);
            double readiness = (double) result.get("readinessScore");
            assertTrue(readiness >= 80.0, "30min session should give readiness >= 80, got " + readiness);
        }

        @Test
        @DisplayName("Readiness score: <15min = low range")
        void readinessLowRange()
        {
            List<Map<String, Object>> sessions = new ArrayList<>();
            Map<String, Object> s1 = new java.util.LinkedHashMap<>();
            s1.put("durationMinutes", 5L);
            s1.put("steps", 100);
            s1.put("caloriesBurned", 10.0);
            s1.put("accelAvg", 1.0);
            s1.put("activityType", "sedentary");
            sessions.add(s1);

            Map<String, Object> result = svc.aggregateDaily(sessions);
            double readiness = (double) result.get("readinessScore");
            assertTrue(readiness < 50.0, "5min session should give readiness < 50, got " + readiness);
        }
    }

    // ========== calculateReadinessScore ==========

    @Nested
    @DisplayName("calculateReadinessScore")
    class ReadinessScoreTests
    {
        @Test
        @DisplayName("Baseline score with no modifiers = 70")
        void baselineScore()
        {
            double score = svc.calculateReadinessScore(null, null, 2, 0);
            assertEquals(70.0, score, 0.01);
        }

        @Test
        @DisplayName("Low resting HR (athlete) boosts score by +15")
        void lowRestingHR()
        {
            double score = svc.calculateReadinessScore(55, null, 2, 0);
            assertEquals(85.0, score, 0.01);
        }

        @Test
        @DisplayName("Good resting HR boosts by +10")
        void goodRestingHR()
        {
            double score = svc.calculateReadinessScore(65, null, 2, 0);
            assertEquals(80.0, score, 0.01);
        }

        @Test
        @DisplayName("Elevated resting HR penalizes by -10")
        void elevatedRestingHR()
        {
            double score = svc.calculateReadinessScore(90, null, 2, 0);
            assertEquals(60.0, score, 0.01);
        }

        @Test
        @DisplayName("Normal resting HR (70-79) = no change")
        void normalRestingHR()
        {
            double score = svc.calculateReadinessScore(75, null, 2, 0);
            assertEquals(70.0, score, 0.01);
        }

        @Test
        @DisplayName("Rested yesterday (<3000 steps) boosts by +10")
        void restedYesterday()
        {
            double score = svc.calculateReadinessScore(null, 2000, 2, 0);
            assertEquals(80.0, score, 0.01);
        }

        @Test
        @DisplayName("Active yesterday (>10000 steps) boosts by +5")
        void activeYesterday()
        {
            double score = svc.calculateReadinessScore(null, 12000, 2, 0);
            assertEquals(75.0, score, 0.01);
        }

        @Test
        @DisplayName("Trained today (daysSinceLast=0) penalizes -15")
        void trainedToday()
        {
            double score = svc.calculateReadinessScore(null, null, 0, 0);
            assertEquals(55.0, score, 0.01);
        }

        @Test
        @DisplayName("1 day rest = ideal (+5)")
        void idealRest()
        {
            double score = svc.calculateReadinessScore(null, null, 1, 0);
            assertEquals(75.0, score, 0.01);
        }

        @Test
        @DisplayName("Long break (>7 days) penalizes -15")
        void longBreak()
        {
            double score = svc.calculateReadinessScore(null, null, 10, 0);
            assertEquals(55.0, score, 0.01);
        }

        @Test
        @DisplayName("High weekly frequency (>=3) boosts +5")
        void highFrequency()
        {
            double score = svc.calculateReadinessScore(null, null, 2, 3.5);
            assertEquals(75.0, score, 0.01);
        }

        @Test
        @DisplayName("Score clamped to 0-100 range")
        void clampedRange()
        {
            // Worst case: elevated HR + trained today + long break + no frequency
            double lowScore = svc.calculateReadinessScore(90, 12000, 0, 0);
            assertTrue(lowScore >= 0.0, "Score should be >= 0, got " + lowScore);

            // Best case: athlete HR + rested + ideal rest + high frequency
            double highScore = svc.calculateReadinessScore(50, 2000, 1, 4.0);
            assertTrue(highScore <= 100.0, "Score should be <= 100, got " + highScore);
        }

        @Test
        @DisplayName("Combined modifiers — athlete + rested + ideal rest + high freq")
        void combinedOptimal()
        {
            double score = svc.calculateReadinessScore(55, 2000, 1, 3.5);
            // 70 + 15(HR<60) + 10(rested) + 5(1day rest) + 5(high freq) = 105 -> clamped to 100
            assertEquals(100.0, score, 0.01);
        }
    }

    // ========== Buffer Management ==========

    @Nested
    @DisplayName("Buffer management")
    class BufferTests
    {
        @Test
        @DisplayName("addRawData and getRawDataInRange")
        void addAndGetRange()
        {
            LocalDateTime t1 = LocalDateTime.of(2026, 4, 16, 10, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 4, 16, 11, 0, 0);
            LocalDateTime t3 = LocalDateTime.of(2026, 4, 16, 12, 0, 0);

            svc.addRawData(makePoint("D1", "U1", t1, 1.0, 0, 0, null, null, null, null));
            svc.addRawData(makePoint("D1", "U1", t2, 1.0, 0, 0, null, null, null, null));
            svc.addRawData(makePoint("D1", "U1", t3, 1.0, 0, 0, null, null, null, null));

            // Range: 10:00 - 11:30 should get t1 and t2
            List<DeviceData> result = svc.getRawDataInRange(
                LocalDateTime.of(2026, 4, 16, 10, 0, 0),
                LocalDateTime.of(2026, 4, 16, 11, 30, 0)
            );
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("addRawData null is ignored")
        void nullDataIgnored()
        {
            svc.addRawData(null);
            // Should not throw; buffer remains empty
            List<DeviceData> result = svc.getRawDataInRange(
                LocalDateTime.MIN, LocalDateTime.MAX
            );
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("getRawDataForUserOnDate filters by user and date")
        void userDateFilter()
        {
            LocalDate date = LocalDate.of(2026, 4, 16);

            svc.addRawData(makePoint("D1", "U1", date.atTime(10, 0), 1.0, 0, 0, null, null, null, null));
            svc.addRawData(makePoint("D1", "U2", date.atTime(10, 30), 1.0, 0, 0, null, null, null, null));
            svc.addRawData(makePoint("D1", "U1", date.plusDays(1).atTime(10, 0), 1.0, 0, 0, null, null, null, null));

            List<DeviceData> u1Today = svc.getRawDataForUserOnDate("U1", date);
            assertEquals(1, u1Today.size());
            assertEquals("U1", u1Today.get(0).getUserId());
        }

        @Test
        @DisplayName("clearOldData removes entries older than threshold")
        void clearOldData()
        {
            // Add old data (2 days ago) and recent data (today)
            svc.addRawData(makePoint("D1", "U1", LocalDateTime.now().minusDays(3), 1.0, 0, 0, null, null, null, null));
            svc.addRawData(makePoint("D1", "U1", LocalDateTime.now().minusDays(1), 1.0, 0, 0, null, null, null, null));
            svc.addRawData(makePoint("D1", "U1", LocalDateTime.now(), 1.0, 0, 0, null, null, null, null));

            int cleared = svc.clearOldData(2);  // Remove data older than 2 days
            assertEquals(1, cleared);
        }

        @Test
        @DisplayName("clearOldData with no old data returns 0")
        void clearNothing()
        {
            svc.addRawData(makePoint("D1", "U1", LocalDateTime.now(), 1.0, 0, 0, null, null, null, null));
            int cleared = svc.clearOldData(30);
            assertEquals(0, cleared);
        }
    }
}
