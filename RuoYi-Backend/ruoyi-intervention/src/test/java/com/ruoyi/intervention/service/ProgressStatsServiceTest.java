package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.service.ProgressStatsService.DailyCompletionRecord;

/**
 * JUnit 5 tests for ProgressStatsService.
 *
 * Covers:
 *   - complianceRate: empty, no planned, full compliance, partial
 *   - complianceDetail: grade boundaries (A/B+/B/C+/C/D/F)
 *   - currentStreak: no records, active streak, broken streak, today/yesterday start
 *   - longestStreak: single, multi, with gaps
 *   - volumeTrend: weekly aggregation, missing weeks
 *   - intensitySlope: linear regression, <3 points, constant intensity, improving/declining
 *   - getFullStats: comprehensive output structure
 *   - recordCompletion / recordCompletions: data ingestion
 *   - DailyCompletionRecord: constructor, getters/setters
 *
 * Grade thresholds:
 *   A >= 0.9, B+ >= 0.8, B >= 0.7, C+ >= 0.6, C >= 0.5, D >= 0.3, F < 0.3
 *
 * Ticket: XIN-107 algorithm validation
 */
class ProgressStatsServiceTest
{
    private ProgressStatsService svc;

    @BeforeEach
    void setUp()
    {
        svc = new ProgressStatsService();
    }

    // ========== Fixtures ==========

    private static DailyCompletionRecord rec(LocalDate date, boolean planned, boolean completed,
                                              int durationMin, String activity, double intensity)
    {
        return new DailyCompletionRecord(date, planned, completed, durationMin, activity, intensity);
    }

    /** Build N consecutive daily records ending today (or yesterday if endYesterday=true). */
    private static List<DailyCompletionRecord> buildStreak(int days, boolean endYesterday)
    {
        List<DailyCompletionRecord> records = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days - 1);
        if (endYesterday) start = start.minusDays(1);

        for (int i = 0; i < days; i++)
        {
            LocalDate d = start.plusDays(i);
            records.add(rec(d, true, true, 30, "running", 50 + i));
        }
        return records;
    }

    /** Build planned records for the last N days with given completion rate. */
    private static List<DailyCompletionRecord> buildComplianceRecords(int totalDays, int completedDays)
    {
        List<DailyCompletionRecord> records = new ArrayList<>();
        for (int i = 0; i < totalDays; i++)
        {
            LocalDate d = LocalDate.now().minusDays(totalDays - 1 - i);
            boolean completed = i < completedDays;
            records.add(rec(d, true, completed, completed ? 30 : 0, "running", completed ? 60.0 : 0.0));
        }
        return records;
    }

    // ========== complianceRate ==========

    @Nested
    @DisplayName("complianceRate")
    class ComplianceRateTests
    {
        @Test
        @DisplayName("No records returns 0.0")
        void noRecords()
        {
            assertEquals(0.0, svc.complianceRate("UNKNOWN", 7), 0.001);
        }

        @Test
        @DisplayName("No planned sessions returns 0.0")
        void noPlanned()
        {
            svc.recordCompletion("U1", rec(LocalDate.now(), false, true, 30, "walking", 50));
            assertEquals(0.0, svc.complianceRate("U1", 7), 0.001);
        }

        @Test
        @DisplayName("100% compliance")
        void fullCompliance()
        {
            List<DailyCompletionRecord> records = buildComplianceRecords(5, 5);
            svc.recordCompletions("U1", records);
            assertEquals(1.0, svc.complianceRate("U1", 7), 0.001);
        }

        @Test
        @DisplayName("60% compliance (3 of 5 planned)")
        void partialCompliance()
        {
            List<DailyCompletionRecord> records = buildComplianceRecords(5, 3);
            svc.recordCompletions("U1", records);
            assertEquals(0.6, svc.complianceRate("U1", 7), 0.001);
        }

        @Test
        @DisplayName("Unplanned days don't affect rate")
        void unplannedIgnored()
        {
            LocalDate today = LocalDate.now();
            svc.recordCompletion("U1", rec(today.minusDays(2), true, true, 30, "run", 50));  // planned + completed
            svc.recordCompletion("U1", rec(today.minusDays(1), false, true, 30, "run", 50)); // unplanned (rest day)
            svc.recordCompletion("U1", rec(today, true, false, 0, "run", 0));                // planned + missed

            // 1 completed / 2 planned = 0.5
            assertEquals(0.5, svc.complianceRate("U1", 7), 0.001);
        }

        @Test
        @DisplayName("Only counts records within trailing window")
        void trailingWindow()
        {
            // Build 10 days: first 5 completed, last 5 missed
            List<DailyCompletionRecord> recs = new ArrayList<>();
            for (int i = 0; i < 10; i++)
            {
                LocalDate d = LocalDate.now().minusDays(9 - i);
                boolean completed = i < 5; // first 5 completed, last 5 missed
                recs.add(rec(d, true, completed, completed ? 30 : 0, "run", completed ? 60.0 : 0.0));
            }
            svc.recordCompletions("U1", recs);

            // trailing=4: cutoff = now()-4. Records with date >= now()-4:
            //   i=5(now()-4), i=6(now()-3), i=7(now()-2), i=8(now()-1), i=9(now())
            //   = 5 records, 0 completed = 0.0
            assertEquals(0.0, svc.complianceRate("U1", 4), 0.001);

            // trailing=8: cutoff = now()-8. Records with date >= now()-8:
            //   i=1(now()-8) .. i=9(now()) = 9 records
            //   Completed: i=1,2,3,4 = 4 of 9
            assertEquals(4.0/9.0, svc.complianceRate("U1", 8), 0.001);
        }
    }

    // ========== complianceDetail ==========

    @Nested
    @DisplayName("complianceDetail")
    class ComplianceDetailTests
    {
        @Test
        @DisplayName("Grade A (>=90%)")
        void gradeA()
        {
            // 9/10 = 0.9
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 9);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("A", detail.get("grade"));
            assertEquals(0.9, (double) detail.get("complianceRate"), 0.001);
            assertEquals(1L, detail.get("missedSessions"));
        }

        @Test
        @DisplayName("Grade B+ (>=80%)")
        void gradeBPlus()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 8);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("B+", detail.get("grade"));
        }

        @Test
        @DisplayName("Grade B (>=70%)")
        void gradeB()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 7);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("B", detail.get("grade"));
        }

        @Test
        @DisplayName("Grade C+ (>=60%)")
        void gradeCPlus()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 6);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("C+", detail.get("grade"));
        }

        @Test
        @DisplayName("Grade C (>=50%)")
        void gradeC()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 5);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("C", detail.get("grade"));
        }

        @Test
        @DisplayName("Grade D (>=30%)")
        void gradeD()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 3);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("D", detail.get("grade"));
        }

        @Test
        @DisplayName("Grade F (<30%)")
        void gradeF()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(10, 2);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 14);
            assertEquals("F", detail.get("grade"));
        }

        @Test
        @DisplayName("Detail structure contains all expected keys")
        void detailStructure()
        {
            svc.recordCompletions("U1", buildComplianceRecords(5, 4));
            Map<String, Object> detail = svc.complianceDetail("U1", 7);
            assertTrue(detail.containsKey("periodDays"));
            assertTrue(detail.containsKey("plannedSessions"));
            assertTrue(detail.containsKey("completedSessions"));
            assertTrue(detail.containsKey("missedSessions"));
            assertTrue(detail.containsKey("complianceRate"));
            assertTrue(detail.containsKey("grade"));
        }
    }

    // ========== currentStreak ==========

    @Nested
    @DisplayName("currentStreak")
    class CurrentStreakTests
    {
        @Test
        @DisplayName("No records returns 0")
        void noRecords()
        {
            assertEquals(0, svc.currentStreak("UNKNOWN"));
        }

        @Test
        @DisplayName("Streak ending today")
        void streakEndingToday()
        {
            List<DailyCompletionRecord> recs = buildStreak(5, false); // ends today
            svc.recordCompletions("U1", recs);
            assertEquals(5, svc.currentStreak("U1"));
        }

        @Test
        @DisplayName("Streak ending yesterday")
        void streakEndingYesterday()
        {
            List<DailyCompletionRecord> recs = buildStreak(3, true); // ends yesterday
            svc.recordCompletions("U1", recs);
            assertEquals(3, svc.currentStreak("U1"));
        }

        @Test
        @DisplayName("No recent training breaks streak")
        void brokenStreak()
        {
            // Last training was 3 days ago
            LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
            svc.recordCompletion("U1", rec(threeDaysAgo, true, true, 30, "run", 50));
            assertEquals(0, svc.currentStreak("U1"));
        }

        @Test
        @DisplayName("Uncompleted day breaks streak")
        void uncompletedDayBreaks()
        {
            LocalDate today = LocalDate.now();
            svc.recordCompletion("U1", rec(today.minusDays(1), true, true, 30, "run", 50));
            svc.recordCompletion("U1", rec(today, true, false, 0, "run", 0)); // today missed
            assertEquals(0, svc.currentStreak("U1"));
        }

        @Test
        @DisplayName("Single day streak (today only)")
        void singleDayStreak()
        {
            svc.recordCompletion("U1", rec(LocalDate.now(), true, true, 30, "run", 50));
            assertEquals(1, svc.currentStreak("U1"));
        }
    }

    // ========== longestStreak ==========

    @Nested
    @DisplayName("longestStreak")
    class LongestStreakTests
    {
        @Test
        @DisplayName("No records returns 0")
        void noRecords()
        {
            assertEquals(0, svc.longestStreak("UNKNOWN"));
        }

        @Test
        @DisplayName("Single completed day")
        void singleDay()
        {
            svc.recordCompletion("U1", rec(LocalDate.of(2026, 4, 10), true, true, 30, "run", 50));
            assertEquals(1, svc.longestStreak("U1"));
        }

        @Test
        @DisplayName("Consecutive completed days")
        void consecutive()
        {
            LocalDate base = LocalDate.of(2026, 4, 10);
            for (int i = 0; i < 5; i++)
            {
                svc.recordCompletion("U1", rec(base.plusDays(i), true, true, 30, "run", 50));
            }
            assertEquals(5, svc.longestStreak("U1"));
        }

        @Test
        @DisplayName("Gap in records — longest is the bigger segment")
        void gapInRecords()
        {
            LocalDate base = LocalDate.of(2026, 4, 1);
            // 3-day streak, gap, 5-day streak
            for (int i = 0; i < 3; i++)
                svc.recordCompletion("U1", rec(base.plusDays(i), true, true, 30, "run", 50));
            for (int i = 5; i < 10; i++)
                svc.recordCompletion("U1", rec(base.plusDays(i), true, true, 30, "run", 50));

            assertEquals(5, svc.longestStreak("U1"));
        }

        @Test
        @DisplayName("Uncompleted day resets streak counter")
        void uncompletedResets()
        {
            LocalDate base = LocalDate.of(2026, 4, 1);
            svc.recordCompletion("U1", rec(base, true, true, 30, "run", 50));
            svc.recordCompletion("U1", rec(base.plusDays(1), true, false, 0, "run", 0)); // missed
            svc.recordCompletion("U1", rec(base.plusDays(2), true, true, 30, "run", 50));

            assertEquals(1, svc.longestStreak("U1")); // max consecutive completed = 1
        }
    }

    // ========== volumeTrend ==========

    @Nested
    @DisplayName("volumeTrend")
    class VolumeTrendTests
    {
        @Test
        @DisplayName("Empty history returns zeroed weeks")
        void emptyHistory()
        {
            List<Map<String, Object>> trend = svc.volumeTrend("UNKNOWN", 4);
            assertEquals(4, trend.size());
            for (Map<String, Object> week : trend)
            {
                assertEquals(0, week.get("totalMinutes"));
                assertEquals(0L, week.get("completedDays"));
                assertEquals(0.0, (double) week.get("avgDailyMinutes"), 0.01);
            }
        }

        @Test
        @DisplayName("Single completed session in current week")
        void currentWeekSession()
        {
            LocalDate today = LocalDate.now();
            svc.recordCompletion("U1", rec(today, true, true, 45, "running", 65));

            List<Map<String, Object>> trend = svc.volumeTrend("U1", 2);
            assertEquals(2, trend.size());

            // Last week entry should have the session
            Map<String, Object> lastWeek = trend.get(1); // most recent
            assertEquals(45, lastWeek.get("totalMinutes"));
            assertEquals(1L, lastWeek.get("completedDays"));
            assertEquals(45.0, (double) lastWeek.get("avgDailyMinutes"), 0.01);
        }

        @Test
        @DisplayName("Multiple sessions in a week aggregate correctly")
        void multiSessionWeek()
        {
            LocalDate today = LocalDate.now();
            // Both sessions on today so they fall in the same week bucket
            svc.recordCompletion("U1", rec(today, true, true, 30, "run", 50));
            svc.recordCompletion("U1", rec(today, true, true, 60, "run", 70));

            List<Map<String, Object>> trend = svc.volumeTrend("U1", 2);
            assertEquals(2, trend.size());

            // Most recent week = trend.get(1) (w=0, today..today+6)
            Map<String, Object> week = trend.get(1);
            assertEquals(90, week.get("totalMinutes"));
            assertEquals(2L, week.get("completedDays"));
            assertEquals(45.0, (double) week.get("avgDailyMinutes"), 0.01);
        }
    }

    // ========== intensitySlope ==========

    @Nested
    @DisplayName("intensitySlope")
    class IntensitySlopeTests
    {
        @Test
        @DisplayName("Fewer than 3 completed points returns 0.0")
        void tooFewPoints()
        {
            LocalDate today = LocalDate.now();
            svc.recordCompletion("U1", rec(today.minusDays(1), true, true, 30, "run", 50));
            svc.recordCompletion("U1", rec(today, true, true, 30, "run", 60));
            assertEquals(0.0, svc.intensitySlope("U1", 7), 0.001);
        }

        @Test
        @DisplayName("Constant intensity gives slope ≈ 0")
        void constantIntensity()
        {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 5; i++)
            {
                svc.recordCompletion("U1", rec(today.minusDays(4 - i), true, true, 30, "run", 60.0));
            }
            assertEquals(0.0, svc.intensitySlope("U1", 7), 0.001);
        }

        @Test
        @DisplayName("Improving intensity gives positive slope")
        void improvingIntensity()
        {
            LocalDate today = LocalDate.now();
            double[] intensities = {40.0, 50.0, 60.0, 70.0, 80.0};
            for (int i = 0; i < 5; i++)
            {
                svc.recordCompletion("U1", rec(today.minusDays(4 - i), true, true, 30, "run", intensities[i]));
            }
            double slope = svc.intensitySlope("U1", 7);
            assertTrue(slope > 0, "Improving intensity should give positive slope, got " + slope);
            // Slope should be exactly 10.0 (linear regression of y = 40+10x)
            assertEquals(10.0, slope, 0.01);
        }

        @Test
        @DisplayName("Declining intensity gives negative slope")
        void decliningIntensity()
        {
            LocalDate today = LocalDate.now();
            double[] intensities = {80.0, 70.0, 60.0, 50.0, 40.0};
            for (int i = 0; i < 5; i++)
            {
                svc.recordCompletion("U1", rec(today.minusDays(4 - i), true, true, 30, "run", intensities[i]));
            }
            double slope = svc.intensitySlope("U1", 7);
            assertTrue(slope < 0, "Declining intensity should give negative slope, got " + slope);
            assertEquals(-10.0, slope, 0.01);
        }

        @Test
        @DisplayName("Uncompleted records are filtered out")
        void uncompletedFiltered()
        {
            LocalDate today = LocalDate.now();
            // Only 2 completed -> slope = 0
            svc.recordCompletion("U1", rec(today.minusDays(4), true, true, 30, "run", 40));
            svc.recordCompletion("U1", rec(today.minusDays(3), true, false, 0, "run", 0));
            svc.recordCompletion("U1", rec(today.minusDays(2), true, true, 30, "run", 50));
            assertEquals(0.0, svc.intensitySlope("U1", 7), 0.001);
        }
    }

    // ========== getFullStats ==========

    @Nested
    @DisplayName("getFullStats")
    class FullStatsTests
    {
        @Test
        @DisplayName("Empty user returns structure with zero/default values")
        void emptyUser()
        {
            Map<String, Object> stats = svc.getFullStats("NOBODY");

            assertTrue(stats.containsKey("compliance4Week"));
            assertTrue(stats.containsKey("compliance7Day"));
            assertTrue(stats.containsKey("currentStreak"));
            assertTrue(stats.containsKey("longestStreak"));
            assertTrue(stats.containsKey("volumeTrend8Weeks"));
            assertTrue(stats.containsKey("intensitySlope4Week"));
            assertTrue(stats.containsKey("overallTrend"));
            assertTrue(stats.containsKey("isOnTrack"));

            assertEquals(0.0, (double) stats.get("compliance7Day"), 0.001);
            assertEquals(0, stats.get("currentStreak"));
            assertEquals(0, stats.get("longestStreak"));
            assertFalse((boolean) stats.get("isOnTrack"));
        }

        @Test
        @DisplayName("Good user is on track: compliance >= 70% and not declining")
        void goodUserOnTrack()
        {
            LocalDate today = LocalDate.now();
            // 7 of 7 planned sessions completed
            for (int i = 0; i < 7; i++)
            {
                LocalDate d = today.minusDays(6 - i);
                svc.recordCompletion("U1", rec(d, true, true, 30, "run", 50 + i * 2));
            }

            Map<String, Object> stats = svc.getFullStats("U1");
            assertTrue((boolean) stats.get("isOnTrack"), "7/7 compliance should be on track");
            // Trend should be improving or stable (slope = +2)
            String trend = (String) stats.get("overallTrend");
            assertTrue("improving".equals(trend) || "stable".equals(trend),
                       "Expected improving or stable, got " + trend);
        }

        @Test
        @DisplayName("Poor compliance (<70%) is not on track")
        void poorComplianceNotOnTrack()
        {
            LocalDate today = LocalDate.now();
            // 3 of 7 planned (43% compliance)
            for (int i = 0; i < 7; i++)
            {
                boolean completed = i < 3;
                svc.recordCompletion("U1", rec(today.minusDays(6 - i), true, completed,
                                                 completed ? 30 : 0, "run", completed ? 60.0 : 0.0));
            }

            Map<String, Object> stats = svc.getFullStats("U1");
            assertFalse((boolean) stats.get("isOnTrack"), "43% compliance should NOT be on track");
        }

        @Test
        @DisplayName("volumeTrend8Weeks has 8 entries")
        void volumeTrendSize()
        {
            svc.recordCompletion("U1", rec(LocalDate.now(), true, true, 30, "run", 50));
            Map<String, Object> stats = svc.getFullStats("U1");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> vt = (List<Map<String, Object>>) stats.get("volumeTrend8Weeks");
            assertEquals(8, vt.size());
        }
    }

    // ========== recordCompletion / recordCompletions ==========

    @Nested
    @DisplayName("Data ingestion")
    class DataIngestionTests
    {
        @Test
        @DisplayName("recordCompletion adds single record")
        void singleRecord()
        {
            svc.recordCompletion("U1", rec(LocalDate.now(), true, true, 30, "run", 50));
            Map<String, Object> detail = svc.complianceDetail("U1", 7);
            assertEquals(1L, detail.get("plannedSessions"));
            assertEquals(1L, detail.get("completedSessions"));
        }

        @Test
        @DisplayName("recordCompletions adds batch")
        void batchRecord()
        {
            List<DailyCompletionRecord> recs = buildComplianceRecords(5, 5);
            svc.recordCompletions("U1", recs);
            Map<String, Object> detail = svc.complianceDetail("U1", 7);
            assertEquals(5L, detail.get("plannedSessions"));
        }

        @Test
        @DisplayName("Multiple users are tracked independently")
        void multiUser()
        {
            svc.recordCompletion("U1", rec(LocalDate.now(), true, true, 30, "run", 50));
            svc.recordCompletion("U2", rec(LocalDate.now(), true, false, 0, "run", 0));

            assertEquals(1.0, svc.complianceRate("U1", 7), 0.001);
            assertEquals(0.0, svc.complianceRate("U2", 7), 0.001);
        }
    }

    // ========== DailyCompletionRecord ==========

    @Nested
    @DisplayName("DailyCompletionRecord POJO")
    class RecordPojoTests
    {
        @Test
        @DisplayName("Constructor sets all fields")
        void constructorFields()
        {
            LocalDate d = LocalDate.of(2026, 4, 16);
            DailyCompletionRecord r = new DailyCompletionRecord(d, true, true, 45, "running", 75.5);

            assertEquals(d, r.getDate());
            assertTrue(r.isPlanned());
            assertTrue(r.isCompleted());
            assertEquals(45, r.getDurationMinutes());
            assertEquals("running", r.getActivityType());
            assertEquals(75.5, r.getIntensityScore(), 0.001);
        }

        @Test
        @DisplayName("Default constructor and setters work")
        void defaultConstructor()
        {
            DailyCompletionRecord r = new DailyCompletionRecord();
            assertNull(r.getDate());
            assertFalse(r.isPlanned());
            assertFalse(r.isCompleted());
            assertEquals(0, r.getDurationMinutes());
            assertNull(r.getActivityType());
            assertEquals(0.0, r.getIntensityScore(), 0.001);

            LocalDate d = LocalDate.of(2026, 5, 1);
            r.setDate(d);
            r.setPlanned(true);
            r.setCompleted(true);
            r.setDurationMinutes(60);
            r.setActivityType("walking");
            r.setIntensityScore(40.0);

            assertEquals(d, r.getDate());
            assertTrue(r.isPlanned());
            assertTrue(r.isCompleted());
            assertEquals(60, r.getDurationMinutes());
            assertEquals("walking", r.getActivityType());
            assertEquals(40.0, r.getIntensityScore(), 0.001);
        }
    }
}
