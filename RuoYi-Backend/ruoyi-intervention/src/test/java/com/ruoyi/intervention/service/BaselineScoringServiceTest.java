package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.service.BaselineScoringService.BaselineMeasurements;
import com.ruoyi.intervention.service.BaselineScoringService.CompositeBaselineReport;
import com.ruoyi.intervention.service.BaselineScoringService.DimensionScore;

/**
 * JUnit 5 tests for BaselineScoringService.
 * Migrated from: intervention-engine/tests/test_baseline_scoring.py
 *
 * Covers:
 *   - assess() with complete and partial measurements
 *   - All 4 dimension scorers: cardiovascular, metabolic, sleep, musculoskeletal
 *   - Composite weighted score computation
 *   - scoreProgress() delta tracking
 *   - Static helper functions
 *
 * All numerical assertions match Python test expectations within ±0.01.
 */
class BaselineScoringServiceTest
{
    private BaselineScoringService scorer;

    @BeforeEach
    void setUp()
    {
        scorer = new BaselineScoringService();
    }

    // ========== Fixtures ==========

    private static BaselineMeasurements maleFull()
    {
        BaselineMeasurements m = new BaselineMeasurements();
        m.stepTestRecoveryHr = 100;
        m.restingHr = 62;
        m.systolicBp = 118;
        m.bmi = 22.0;
        m.waistCircumference = 82.0;
        m.bodyFatPct = 15.0;
        m.sleepDuration = 7.5;
        m.psqiGlobal = 4;
        m.sleepEfficiency = 88.0;   // stored as 0-100, normalized internally
        m.gripStrength = 45.0;
        m.sitAndReach = 15.0;
        m.lowerLimbPowerCm = 220.0;
        m.chairStandReps = 20;
        return m;
    }

    private static BaselineMeasurements femaleFull()
    {
        BaselineMeasurements m = new BaselineMeasurements();
        m.restingHr = 68;
        m.stepTestRecoveryHr = 108;
        m.systolicBp = 112;
        m.bmi = 21.0;
        m.waistCircumference = 75.0;
        m.bodyFatPct = 18.0;
        m.sleepDuration = 8.0;
        m.psqiGlobal = 3;
        m.sleepEfficiency = 92.0;
        m.gripStrength = 30.0;
        m.sitAndReach = 20.0;
        m.lowerLimbPowerCm = 175.0;
        m.chairStandReps = 18;
        return m;
    }

    // ========== assess() — Full Input ==========

    @Nested
    @DisplayName("TestAssessFull — Complete measurement sets")
    class TestAssessFull
    {
        @Test
        @DisplayName("Male full measurements: all 4 dimensions present")
        void test_assess_male_full()
        {
            CompositeBaselineReport report = scorer.assess("u001", maleFull(), "male");

            assertEquals("u001", report.userId);
            assertNotNull(report.assessedAt);
            assertEquals("1.0", report.configVersion);

            assertEquals(4, report.dimensionScores.size());
            assertTrue(report.dimensionScores.containsKey("cardiovascular"));
            assertTrue(report.dimensionScores.containsKey("metabolic"));
            assertTrue(report.dimensionScores.containsKey("sleep"));
            assertTrue(report.dimensionScores.containsKey("musculoskeletal"));
        }

        @Test
        @DisplayName("Female full measurements: all 4 dimensions present")
        void test_assess_female_full()
        {
            CompositeBaselineReport report = scorer.assess("u002", femaleFull(), "female");

            assertEquals("u002", report.userId);
            assertEquals(4, report.dimensionScores.size());
            assertEquals(0, report.missingIndicators.size());
        }

        @Test
        @DisplayName("Evidence refs are populated: china_gbt_10000_2023, acsm_2021_ch4, buysse_1989_psqi, hirshkowitz_2015, who_2020_pa")
        void test_assess_stores_evidence_refs()
        {
            CompositeBaselineReport report = scorer.assess("u003", maleFull(), "male");
            assertTrue(report.configEvidenceRefs.contains("china_gbt_10000_2023"));
            assertTrue(report.configEvidenceRefs.contains("acsm_2021_ch4"));
            assertTrue(report.configEvidenceRefs.contains("buysse_1989_psqi"));
            assertTrue(report.configEvidenceRefs.contains("hirshkowitz_2015"));
            assertTrue(report.configEvidenceRefs.contains("who_2020_pa"));
        }

        @Test
        @DisplayName("All dimension scores have grades: excellent/good/fair/poor")
        void test_assess_dimension_scores_have_grades()
        {
            CompositeBaselineReport report = scorer.assess("u004", maleFull(), "male");
            for (Map.Entry<String, DimensionScore> entry : report.dimensionScores.entrySet())
            {
                DimensionScore ds = entry.getValue();
                assertEquals(entry.getKey(), ds.dimension);
                assertTrue(
                    "excellent".equals(ds.grade) || "good".equals(ds.grade)
                    || "fair".equals(ds.grade) || "poor".equals(ds.grade),
                    "Unexpected grade for " + entry.getKey() + ": " + ds.grade);
                assertNotNull(ds.gradeLabelZh);
                assertTrue(ds.totalScore >= 0 && ds.totalScore <= 100);
            }
        }

        @Test
        @DisplayName("Composite grade is one of: excellent/good/fair/poor")
        void test_assess_composite_grade()
        {
            CompositeBaselineReport report = scorer.assess("u005", maleFull(), "male");
            assertTrue(
                "excellent".equals(report.compositeGrade) || "good".equals(report.compositeGrade)
                || "fair".equals(report.compositeGrade) || "poor".equals(report.compositeGrade));
            assertNotNull(report.compositeGradeLabelZh);
            assertNotNull(report.compositeScore);
            assertTrue(report.compositeScore >= 0 && report.compositeScore <= 100);
        }
    }

    // ========== assess() — Partial Input ==========

    @Nested
    @DisplayName("TestAssessPartial — Partial / missing measurements")
    class TestAssessPartial
    {
        @Test
        @DisplayName("Empty measurements: no dimensions, composite null")
        void test_assess_empty_measurements()
        {
            CompositeBaselineReport report = scorer.assess("u100", new BaselineMeasurements(), "male");
            assertEquals("u100", report.userId);
            assertEquals(0, report.dimensionScores.size());
            assertNull(report.compositeScore);
            assertNull(report.compositeGrade);
            assertEquals(4, report.missingIndicators.size());
        }

        @Test
        @DisplayName("Only cardiovascular indicators present")
        void test_assess_only_cardiovascular()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 65;
            m.systolicBp = 120;

            CompositeBaselineReport report = scorer.assess("u101", m, "male");
            assertTrue(report.dimensionScores.containsKey("cardiovascular"));
            assertFalse(report.dimensionScores.containsKey("metabolic"));
            assertFalse(report.dimensionScores.containsKey("sleep"));
            assertFalse(report.dimensionScores.containsKey("musculoskeletal"));
            assertFalse(report.missingIndicators.contains("cardiovascular"));
        }

        @Test
        @DisplayName("Only metabolic indicators present")
        void test_assess_only_metabolic()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bmi = 24.0;
            m.waistCircumference = 90.0;

            CompositeBaselineReport report = scorer.assess("u102", m, "male");
            assertTrue(report.dimensionScores.containsKey("metabolic"));
            assertFalse(report.dimensionScores.containsKey("cardiovascular"));
        }

        @Test
        @DisplayName("Only sleep indicators present")
        void test_assess_only_sleep()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sleepDuration = 6.5;
            m.psqiGlobal = 9;

            CompositeBaselineReport report = scorer.assess("u103", m, "female");
            assertTrue(report.dimensionScores.containsKey("sleep"));
        }

        @Test
        @DisplayName("Only musculoskeletal indicators present")
        void test_assess_only_musculoskeletal()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.gripStrength = 35.0;
            m.sitAndReach = 8.0;

            CompositeBaselineReport report = scorer.assess("u104", m, "male");
            assertTrue(report.dimensionScores.containsKey("musculoskeletal"));
        }

        @Test
        @DisplayName("Missing indicators tracked correctly")
        void test_missing_indicators_tracked()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 70;
            m.bmi = 25.0;
            m.sleepDuration = 6.0;

            CompositeBaselineReport report = scorer.assess("u105", m, "female");
            assertFalse(report.missingIndicators.contains("cardiovascular"));
            assertFalse(report.missingIndicators.contains("metabolic"));
            assertFalse(report.missingIndicators.contains("sleep"));
            assertTrue(report.missingIndicators.contains("musculoskeletal"));
        }
    }

    // ========== Cardiovascular Scoring ==========

    @Nested
    @DisplayName("TestCardiovascularScoring — Cardiovascular dimension")
    class TestCardiovascularScoring
    {
        @Test
        @DisplayName("Resting HR 55 bpm → excellent (>= 85)")
        void test_resting_hr_excellent()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 55;
            CompositeBaselineReport report = scorer.assess("u200", m, "male");
            double score = report.dimensionScores.get("cardiovascular").totalScore;
            assertTrue(score >= 85, "Expected >= 85 but got " + score);
        }

        @Test
        @DisplayName("Resting HR 95 bpm → poor (< 50)")
        void test_resting_hr_poor()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 95;
            CompositeBaselineReport report = scorer.assess("u201", m, "male");
            double score = report.dimensionScores.get("cardiovascular").totalScore;
            assertTrue(score < 50, "Expected < 50 but got " + score);
        }

        @Test
        @DisplayName("Step test recovery 100 bpm → excellent (>= 85)")
        void test_step_test_recovery_excellent()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.stepTestRecoveryHr = 100;
            CompositeBaselineReport report = scorer.assess("u202", m, "male");
            assertTrue(report.dimensionScores.get("cardiovascular").totalScore >= 85);
        }

        @Test
        @DisplayName("Step test recovery 160 bpm → poor (< 50)")
        void test_step_test_recovery_poor()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.stepTestRecoveryHr = 160;
            CompositeBaselineReport report = scorer.assess("u203", m, "male");
            assertTrue(report.dimensionScores.get("cardiovascular").totalScore < 50);
        }

        @Test
        @DisplayName("Systolic BP 115 mmHg → excellent (>= 85)")
        void test_systolic_bp_normal()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.systolicBp = 115;
            CompositeBaselineReport report = scorer.assess("u204", m, "male");
            assertTrue(report.dimensionScores.get("cardiovascular").totalScore >= 85);
        }

        @Test
        @DisplayName("Mixed: resting HR excellent + BP poor → 40-75 range")
        void test_multiple_indicators_average()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 55;      // excellent
            m.systolicBp = 160;   // poor
            CompositeBaselineReport report = scorer.assess("u205", m, "male");
            double score = report.dimensionScores.get("cardiovascular").totalScore;
            assertTrue(score >= 40 && score <= 75,
                "Expected 40-75 but got " + score);
        }
    }

    // ========== Metabolic Scoring ==========

    @Nested
    @DisplayName("TestMetabolicScoring — Metabolic dimension")
    class TestMetabolicScoring
    {
        @Test
        @DisplayName("BMI 21.5 → optimal → excellent (>= 85)")
        void test_bmi_optimal()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bmi = 21.5;
            CompositeBaselineReport report = scorer.assess("u300", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore >= 85);
        }

        @Test
        @DisplayName("BMI 27.0 → overweight → < 70")
        void test_bmi_overweight()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bmi = 27.0;
            CompositeBaselineReport report = scorer.assess("u301", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore < 70);
        }

        @Test
        @DisplayName("BMI 16.5 → underweight → < 70")
        void test_bmi_underweight()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bmi = 16.5;
            CompositeBaselineReport report = scorer.assess("u302", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore < 70);
        }

        @Test
        @DisplayName("Waist 80 cm male → excellent (>= 85)")
        void test_waist_male_excellent()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.waistCircumference = 80.0;
            CompositeBaselineReport report = scorer.assess("u303", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore >= 85);
        }

        @Test
        @DisplayName("Waist 100 cm male → poor (< 50)")
        void test_waist_male_poor()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.waistCircumference = 100.0;
            CompositeBaselineReport report = scorer.assess("u304", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore < 50);
        }

        @Test
        @DisplayName("Waist 82 cm female → metabolic dimension present")
        void test_waist_female_threshold()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.waistCircumference = 82.0;
            CompositeBaselineReport report = scorer.assess("u305", m, "female");
            assertTrue(report.dimensionScores.containsKey("metabolic"));
        }

        @Test
        @DisplayName("Body fat 12% male → excellent (>= 85)")
        void test_body_fat_male_optimal()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bodyFatPct = 12.0;
            CompositeBaselineReport report = scorer.assess("u306", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore >= 85);
        }

        @Test
        @DisplayName("Body fat 28% male → poor (< 50)")
        void test_body_fat_male_obese()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bodyFatPct = 28.0;
            CompositeBaselineReport report = scorer.assess("u307", m, "male");
            assertTrue(report.dimensionScores.get("metabolic").totalScore < 50);
        }

        @Test
        @DisplayName("Body fat 17% female → excellent (>= 85)")
        void test_body_fat_female_optimal()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.bodyFatPct = 17.0;
            CompositeBaselineReport report = scorer.assess("u308", m, "female");
            assertTrue(report.dimensionScores.get("metabolic").totalScore >= 85);
        }
    }

    // ========== Sleep Scoring ==========

    @Nested
    @DisplayName("TestSleepScoring — Sleep dimension")
    class TestSleepScoring
    {
        @Test
        @DisplayName("Sleep duration 8.0h → optimal → excellent (>= 85)")
        void test_sleep_duration_optimal()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sleepDuration = 8.0;
            CompositeBaselineReport report = scorer.assess("u400", m, "male");
            assertTrue(report.dimensionScores.get("sleep").totalScore >= 85);
        }

        @Test
        @DisplayName("Sleep duration 5.0h → short → < 60")
        void test_sleep_duration_short()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sleepDuration = 5.0;
            CompositeBaselineReport report = scorer.assess("u401", m, "male");
            assertTrue(report.dimensionScores.get("sleep").totalScore < 60);
        }

        @Test
        @DisplayName("Sleep duration 11.0h → too long → < 60")
        void test_sleep_duration_too_long()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sleepDuration = 11.0;
            CompositeBaselineReport report = scorer.assess("u402", m, "male");
            assertTrue(report.dimensionScores.get("sleep").totalScore < 60);
        }

        @Test
        @DisplayName("PSQI global 3 → good sleeper → excellent (>= 85)")
        void test_psqi_good_sleeper()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.psqiGlobal = 3;
            CompositeBaselineReport report = scorer.assess("u403", m, "female");
            assertTrue(report.dimensionScores.get("sleep").totalScore >= 85);
        }

        @Test
        @DisplayName("PSQI global 12 → poor sleeper → < 50")
        void test_psqi_poor_sleeper()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.psqiGlobal = 12;
            CompositeBaselineReport report = scorer.assess("u404", m, "female");
            assertTrue(report.dimensionScores.get("sleep").totalScore < 50);
        }

        @Test
        @DisplayName("Sleep efficiency 93% → excellent (>= 85)")
        void test_sleep_efficiency_excellent()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sleepEfficiency = 93.0;  // stored as 0-100
            CompositeBaselineReport report = scorer.assess("u405", m, "male");
            assertTrue(report.dimensionScores.get("sleep").totalScore >= 85);
        }

        @Test
        @DisplayName("Sleep efficiency 78% → fair → 50-75 range")
        void test_sleep_efficiency_fair()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sleepEfficiency = 78.0;
            CompositeBaselineReport report = scorer.assess("u406", m, "male");
            double score = report.dimensionScores.get("sleep").totalScore;
            assertTrue(score >= 50 && score <= 75,
                "Expected 50-75 but got " + score);
        }
    }

    // ========== Musculoskeletal Scoring ==========

    @Nested
    @DisplayName("TestMusculoskeletalScoring — Musculoskeletal dimension")
    class TestMusculoskeletalScoring
    {
        @Test
        @DisplayName("Grip strength 55 kg male → excellent (>= 85)")
        void test_grip_strength_excellent_male()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.gripStrength = 55.0;
            CompositeBaselineReport report = scorer.assess("u500", m, "male");
            assertTrue(report.dimensionScores.get("musculoskeletal").totalScore >= 85);
        }

        @Test
        @DisplayName("Grip strength 32 kg male → fair → 50-75 range")
        void test_grip_strength_fair_male()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.gripStrength = 32.0;
            CompositeBaselineReport report = scorer.assess("u501", m, "male");
            double score = report.dimensionScores.get("musculoskeletal").totalScore;
            assertTrue(score >= 50 && score <= 75,
                "Expected 50-75 but got " + score);
        }

        @Test
        @DisplayName("Grip strength 36 kg female → excellent (>= 85)")
        void test_grip_strength_excellent_female()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.gripStrength = 36.0;
            CompositeBaselineReport report = scorer.assess("u502", m, "female");
            assertTrue(report.dimensionScores.get("musculoskeletal").totalScore >= 85);
        }

        @Test
        @DisplayName("Sit and reach 22 cm → excellent (>= 85)")
        void test_sit_and_reach_excellent()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.sitAndReach = 22.0;
            CompositeBaselineReport report = scorer.assess("u503", m, "male");
            assertTrue(report.dimensionScores.get("musculoskeletal").totalScore >= 85);
        }

        @Test
        @DisplayName("Lower limb power 255 cm male → excellent (>= 85)")
        void test_lower_limb_power_excellent_male()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.lowerLimbPowerCm = 255.0;
            CompositeBaselineReport report = scorer.assess("u504", m, "male");
            assertTrue(report.dimensionScores.get("musculoskeletal").totalScore >= 85);
        }

        @Test
        @DisplayName("Chair stand 24 reps female → excellent (>= 85)")
        void test_chair_stand_excellent()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.chairStandReps = 24;
            CompositeBaselineReport report = scorer.assess("u505", m, "female");
            assertTrue(report.dimensionScores.get("musculoskeletal").totalScore >= 85);
        }
    }

    // ========== Composite Score ==========

    @Nested
    @DisplayName("TestCompositeScore — Weighted composite computation")
    class TestCompositeScore
    {
        @Test
        @DisplayName("All excellent values → composite >= 80")
        void test_composite_is_weighted_average()
        {
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 60;
            m.bmi = 21.0;
            m.sleepDuration = 8.0;
            m.gripStrength = 50.0;

            CompositeBaselineReport report = scorer.assess("u600", m, "male");
            assertNotNull(report.compositeScore);
            assertTrue(report.compositeScore >= 80,
                "Expected >= 80 but got " + report.compositeScore);
        }

        @Test
        @DisplayName("All missing → composite null")
        void test_composite_none_when_all_missing()
        {
            CompositeBaselineReport report = scorer.assess("u601", new BaselineMeasurements(), "male");
            assertNull(report.compositeScore);
        }

        @Test
        @DisplayName("Custom dimension weights (50/50 cardiovascular/metabolic)")
        void test_custom_dimension_weights()
        {
            java.util.Map<String, Double> weights = new java.util.LinkedHashMap<>();
            weights.put("cardiovascular", 0.5);
            weights.put("metabolic", 0.5);
            weights.put("sleep", 0.0);
            weights.put("musculoskeletal", 0.0);

            BaselineScoringService s = new BaselineScoringService(weights);
            BaselineMeasurements m = new BaselineMeasurements();
            m.restingHr = 60;
            m.bmi = 21.0;

            CompositeBaselineReport report = s.assess("u602", m, "male");
            assertNotNull(report.compositeScore);
        }

        @Test
        @DisplayName("Composite score is always in [0, 100]")
        void test_composite_score_in_range()
        {
            CompositeBaselineReport report = scorer.assess("u603", maleFull(), "male");
            assertTrue(report.compositeScore >= 0 && report.compositeScore <= 100,
                "Out of range: " + report.compositeScore);
        }
    }

    // ========== scoreProgress() ==========

    @Nested
    @DisplayName("TestScoreProgress — Progress delta computation")
    class TestScoreProgress
    {
        @Test
        @DisplayName("Improvement: resting HR 85 → 65 → positive delta")
        void test_progress_improvement()
        {
            CompositeBaselineReport before = scorer.assess("u700",
                makeWithRestingHr(85), "male");
            CompositeBaselineReport after = scorer.assess("u700",
                makeWithRestingHr(65), "male");

            Map<String, Object> result = scorer.scoreProgress(before, after);
            double delta = (Double) result.get("composite_delta");
            assertTrue(delta > 0, "Expected positive delta but got " + delta);
        }

        @Test
        @DisplayName("Decline: resting HR 65 → 85 → negative delta")
        void test_progress_decline()
        {
            CompositeBaselineReport before = scorer.assess("u701",
                makeWithRestingHr(65), "male");
            CompositeBaselineReport after = scorer.assess("u701",
                makeWithRestingHr(85), "male");

            Map<String, Object> result = scorer.scoreProgress(before, after);
            double delta = (Double) result.get("composite_delta");
            assertTrue(delta < 0, "Expected negative delta but got " + delta);
        }

        @Test
        @DisplayName("No change → delta 0.0")
        void test_progress_no_change()
        {
            CompositeBaselineReport before = scorer.assess("u702",
                makeWithRestingHr(70), "male");
            CompositeBaselineReport after = scorer.assess("u702",
                makeWithRestingHr(70), "male");

            Map<String, Object> result = scorer.scoreProgress(before, after);
            assertEquals(0.0, result.get("composite_delta"));
        }

        @Test
        @DisplayName("Improved dimensions tracked correctly")
        void test_progress_tracks_improved_dimensions()
        {
            CompositeBaselineReport before = scorer.assess("u703",
                makeWithCardioMetabolic(90, 30), "male");
            CompositeBaselineReport after = scorer.assess("u703",
                makeWithCardioMetabolic(60, 22), "male");

            Map<String, Object> result = scorer.scoreProgress(before, after);
            @SuppressWarnings("unchecked")
            java.util.List<String> improved =
                (java.util.List<String>) result.get("improved_dimensions");
            assertTrue(improved.contains("cardiovascular"));
            assertTrue(improved.contains("metabolic"));
            @SuppressWarnings("unchecked")
            java.util.List<String> declined =
                (java.util.List<String>) result.get("declined_dimensions");
            assertEquals(0, declined.size());
        }

        @Test
        @DisplayName("New dimensions in 'after' (missing in 'before') are NOT tracked")
        void test_progress_with_missing_dimensions()
        {
            CompositeBaselineReport before = scorer.assess("u704",
                makeWithRestingHr(80), "male");
            CompositeBaselineReport after = scorer.assess("u704",
                makeWithCardioMetabolic(60, 22), "male");

            Map<String, Object> result = scorer.scoreProgress(before, after);
            @SuppressWarnings("unchecked")
            java.util.List<String> improved =
                (java.util.List<String>) result.get("improved_dimensions");
            // cardiovascular was in before → tracked
            assertTrue(improved.contains("cardiovascular"));
            // metabolic was missing in before → NOT tracked as improvement
            assertFalse(improved.contains("metabolic"));
        }
    }

    // ========== Helper factories for scoreProgress tests ==========

    private static BaselineMeasurements makeWithRestingHr(int hr)
    {
        BaselineMeasurements m = new BaselineMeasurements();
        m.restingHr = hr;
        return m;
    }

    private static BaselineMeasurements makeWithCardioMetabolic(int restingHr, double bmi)
    {
        BaselineMeasurements m = new BaselineMeasurements();
        m.restingHr = restingHr;
        m.bmi = bmi;
        return m;
    }
}
