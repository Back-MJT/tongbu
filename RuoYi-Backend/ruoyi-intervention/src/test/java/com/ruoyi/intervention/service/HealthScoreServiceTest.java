package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.model.DimensionResult;
import com.ruoyi.intervention.domain.model.HealthScoreInput;
import com.ruoyi.intervention.domain.model.HealthScoreReport;

/**
 * JUnit 5 tests for HealthScoreService.
 * Migrated from: intervention-engine/tests/test_health_score.py
 *
 * Covers:
 *   - calculate() with full, partial, and empty inputs
 *   - All 4 dimension scorers: heart_rate, sleep, exercise, stress
 *   - Score grade boundaries: excellent >=85, good 70-84, fair 50-69, poor <50
 *   - Configurable dimension weights (renormalization on missing dimensions)
 *   - All evidence_ref fields populated in dimension results
 *   - Missing dimension handling and reporting
 *   - Static helper functions: assignGrade, scoreLowerIsBetter, scoreHigherIsBetter,
 *     scoreSleepDuration, scoreSleepEfficiency, scoreExerciseMinutes, scoreSteps
 *   - Edge cases: boundary values, zero/missing input
 *
 * All numerical assertions match Python test expectations within ±0.01.
 */
class HealthScoreServiceTest
{
    private HealthScoreService calc;

    @BeforeEach
    void setUp()
    {
        calc = new HealthScoreService();
    }

    // ========== Fixtures ==========

    private static HealthScoreInput fullHealthyInput()
    {
        HealthScoreInput input = new HealthScoreInput();
        input.setRestingHr(62);
        input.setSleepDuration(7.5);
        input.setPsqiGlobal(3);
        input.setSleepEfficiency(0.92);
        input.setWeeklyExerciseMinutes(210.0);
        input.setDailySteps(9000);
        input.setHrvRmssd(55.0);
        input.setPerceivedStressScore(8);
        return input;
    }

    private static HealthScoreInput poorInput()
    {
        HealthScoreInput input = new HealthScoreInput();
        input.setRestingHr(85);
        input.setSleepDuration(5.0);
        input.setPsqiGlobal(12);
        input.setSleepEfficiency(0.65);
        input.setWeeklyExerciseMinutes(30.0);
        input.setDailySteps(3000);
        input.setHrvRmssd(12.0);
        input.setPerceivedStressScore(30);
        return input;
    }

    private static HealthScoreInput singleDimensionInput()
    {
        HealthScoreInput input = new HealthScoreInput();
        input.setRestingHr(68);
        return input;
    }

    // ========== Helper Function Tests — assignGrade ==========

    @Nested
    @DisplayName("TestAssignGrade — Grade boundary assignments")
    class TestAssignGrade
    {
        @Test
        @DisplayName("Scores >= 85 are excellent")
        void test_excellent_threshold()
        {
            assertEquals("excellent", HealthScoreService.assignGrade(85.0));
            assertEquals("excellent", HealthScoreService.assignGrade(90.0));
            assertEquals("excellent", HealthScoreService.assignGrade(100.0));
        }

        @Test
        @DisplayName("Scores 70-84.9 are good")
        void test_good_threshold()
        {
            assertEquals("good", HealthScoreService.assignGrade(70.0));
            assertEquals("good", HealthScoreService.assignGrade(80.0));
            assertEquals("good", HealthScoreService.assignGrade(84.9));
        }

        @Test
        @DisplayName("Scores 50-69.9 are fair")
        void test_fair_threshold()
        {
            assertEquals("fair", HealthScoreService.assignGrade(50.0));
            assertEquals("fair", HealthScoreService.assignGrade(60.0));
            assertEquals("fair", HealthScoreService.assignGrade(69.9));
        }

        @Test
        @DisplayName("Scores < 50 are poor")
        void test_poor_threshold()
        {
            assertEquals("poor", HealthScoreService.assignGrade(49.9));
            assertEquals("poor", HealthScoreService.assignGrade(25.0));
            assertEquals("poor", HealthScoreService.assignGrade(0.0));
        }
    }

    // ========== Helper Function Tests — scoreLowerIsBetter ==========

    @Nested
    @DisplayName("TestScoreLowerIsBetter — Lower values = better score (resting HR)")
    class TestScoreLowerIsBetter
    {
        @Test
        @DisplayName("Value at excellent threshold returns 100")
        void test_excellent_at_threshold()
        {
            // HR_EXCELLENT = 60.0, HR_GOOD = 70.0, HR_FAIR = 80.0
            double score = HealthScoreService.scoreLowerIsBetter(60.0, 60.0, 70.0, 80.0);
            assertEquals(100.0, score);
        }

        @Test
        @DisplayName("Value below excellent threshold returns 100")
        void test_below_excellent()
        {
            double score = HealthScoreService.scoreLowerIsBetter(55.0, 60.0, 70.0, 80.0);
            assertEquals(100.0, score);
        }

        @Test
        @DisplayName("Value in good range returns 75-100")
        void test_good_range()
        {
            double score = HealthScoreService.scoreLowerIsBetter(65.0, 60.0, 70.0, 80.0);
            assertTrue(score >= 75.0 && score <= 100.0, "score " + score + " expected in [75,100]");
        }

        @Test
        @DisplayName("Value at good threshold returns 75")
        void test_at_good_threshold()
        {
            double score = HealthScoreService.scoreLowerIsBetter(70.0, 60.0, 70.0, 80.0);
            assertEquals(75.0, score);
        }

        @Test
        @DisplayName("Value in fair range returns 50-75")
        void test_fair_range()
        {
            double score = HealthScoreService.scoreLowerIsBetter(75.0, 60.0, 70.0, 80.0);
            assertTrue(score >= 50.0 && score <= 75.0, "score " + score + " expected in [50,75]");
        }

        @Test
        @DisplayName("Value above fair threshold returns 0-50")
        void test_above_fair()
        {
            double score = HealthScoreService.scoreLowerIsBetter(90.0, 60.0, 70.0, 80.0);
            assertTrue(score >= 0.0 && score < 50.0, "score " + score + " expected in [0,50)");
        }
    }

    // ========== Helper Function Tests — scoreHigherIsBetter ==========

    @Nested
    @DisplayName("TestScoreHigherIsBetter — Higher values = better score (steps, exercise)")
    class TestScoreHigherIsBetter
    {
        @Test
        @DisplayName("Value at excellent threshold returns 100")
        void test_excellent_at_threshold()
        {
            // excellent=10000, good=7500, fair=5000
            double score = HealthScoreService.scoreHigherIsBetter(10000.0, 10000.0, 7500.0, 5000.0);
            assertEquals(100.0, score);
        }

        @Test
        @DisplayName("Value above excellent threshold returns 100")
        void test_above_excellent()
        {
            double score = HealthScoreService.scoreHigherIsBetter(12000.0, 10000.0, 7500.0, 5000.0);
            assertEquals(100.0, score);
        }

        @Test
        @DisplayName("Value in good range returns 75-100")
        void test_good_range()
        {
            double score = HealthScoreService.scoreHigherIsBetter(8000.0, 10000.0, 7500.0, 5000.0);
            assertTrue(score >= 75.0 && score <= 100.0, "score " + score + " expected in [75,100]");
        }

        @Test
        @DisplayName("Value in fair range returns 50-75")
        void test_fair_range()
        {
            double score = HealthScoreService.scoreHigherIsBetter(6000.0, 10000.0, 7500.0, 5000.0);
            assertTrue(score >= 50.0 && score <= 75.0, "score " + score + " expected in [50,75]");
        }

        @Test
        @DisplayName("Value below fair threshold returns 0-50")
        void test_below_fair()
        {
            double score = HealthScoreService.scoreHigherIsBetter(2000.0, 10000.0, 7500.0, 5000.0);
            assertTrue(score >= 0.0 && score < 50.0, "score " + score + " expected in [0,50)");
        }
    }

    // ========== Helper Function Tests — scoreSleepDuration ==========

    @Nested
    @DisplayName("TestScoreSleepDuration — Sleep duration scoring")
    class TestScoreSleepDuration
    {
        @Test
        @DisplayName("Optimal range 7-9h returns 100")
        void test_optimal_range()
        {
            assertEquals(100.0, HealthScoreService.scoreSleepDuration(7.5));
            assertEquals(100.0, HealthScoreService.scoreSleepDuration(8.0));
            assertEquals(100.0, HealthScoreService.scoreSleepDuration(9.0));
        }

        @Test
        @DisplayName("Acceptable under optimal (6-7h) returns 70-100")
        void test_acceptable_under_optimal()
        {
            double score = HealthScoreService.scoreSleepDuration(6.5);
            assertTrue(score > 70.0 && score < 100.0, "score " + score + " expected in (70,100)");
        }

        @Test
        @DisplayName("Acceptable over optimal (9-10h) returns 70-100")
        void test_acceptable_over_optimal()
        {
            double score = HealthScoreService.scoreSleepDuration(9.5);
            assertTrue(score > 70.0 && score < 100.0, "score " + score + " expected in (70,100)");
        }

        @Test
        @DisplayName("At acceptable minimum (6h) returns 70")
        void test_at_acceptable_min_boundary()
        {
            assertEquals(70.0, HealthScoreService.scoreSleepDuration(6.0));
        }

        @Test
        @DisplayName("Below acceptable (<6h) returns 0-70")
        void test_below_acceptable()
        {
            double score = HealthScoreService.scoreSleepDuration(5.0);
            assertTrue(score > 0.0 && score < 70.0, "score " + score + " expected in (0,70)");
        }

        @Test
        @DisplayName("Above acceptable (>10h) returns <70")
        void test_above_acceptable()
        {
            double score = HealthScoreService.scoreSleepDuration(11.0);
            assertTrue(score < 70.0, "score " + score + " expected < 70");
        }
    }

    // ========== Helper Function Tests — scoreSleepEfficiency ==========

    @Nested
    @DisplayName("TestScoreSleepEfficiency — Sleep efficiency scoring")
    class TestScoreSleepEfficiency
    {
        @Test
        @DisplayName(">=90% returns 100")
        void test_excellent()
        {
            assertEquals(100.0, HealthScoreService.scoreSleepEfficiency(0.92));
        }

        @Test
        @DisplayName(">=85% <90% returns 80-100")
        void test_good()
        {
            double score = HealthScoreService.scoreSleepEfficiency(0.87);
            assertTrue(score >= 80.0 && score < 100.0, "score " + score + " expected in [80,100)");
        }

        @Test
        @DisplayName(">=75% <85% returns 60-80")
        void test_fair()
        {
            double score = HealthScoreService.scoreSleepEfficiency(0.78);
            assertTrue(score >= 60.0 && score < 80.0, "score " + score + " expected in [60,80)");
        }

        @Test
        @DisplayName("<75% returns 0-60")
        void test_poor()
        {
            double score = HealthScoreService.scoreSleepEfficiency(0.60);
            assertTrue(score > 0.0 && score < 60.0, "score " + score + " expected in (0,60)");
        }
    }

    // ========== Helper Function Tests — scoreExerciseMinutes ==========

    @Nested
    @DisplayName("TestScoreExerciseMinutes — Weekly exercise minutes scoring (WHO 2020)")
    class TestScoreExerciseMinutes
    {
        @Test
        @DisplayName(">=300 min returns 100")
        void test_excellent_above_who_upper()
        {
            assertEquals(100.0, HealthScoreService.scoreExerciseMinutes(350.0));
        }

        @Test
        @DisplayName(">=150 min returns 75")
        void test_good_at_who_minimum()
        {
            assertEquals(75.0, HealthScoreService.scoreExerciseMinutes(150.0));
        }

        @Test
        @DisplayName(">=60 min <150 returns 50-75")
        void test_fair_range()
        {
            double score = HealthScoreService.scoreExerciseMinutes(80.0);
            assertTrue(score > 50.0 && score < 75.0, "score " + score + " expected in (50,75)");
        }

        @Test
        @DisplayName("<60 min returns 0-50")
        void test_below_fair()
        {
            double score = HealthScoreService.scoreExerciseMinutes(30.0);
            assertTrue(score > 0.0 && score < 50.0, "score " + score + " expected in (0,50)");
        }
    }

    // ========== Helper Function Tests — scoreSteps ==========

    @Nested
    @DisplayName("TestScoreSteps — Daily step count scoring")
    class TestScoreSteps
    {
        @Test
        @DisplayName("10000 steps returns 100")
        void test_excellent_10000()
        {
            assertEquals(100.0, HealthScoreService.scoreSteps(10000));
        }

        @Test
        @DisplayName("8000 steps returns 75-100")
        void test_good_7500()
        {
            double score = HealthScoreService.scoreSteps(8000);
            assertTrue(score >= 75.0 && score <= 100.0, "score " + score + " expected in [75,100]");
        }

        @Test
        @DisplayName("6000 steps returns 50-75")
        void test_fair_5000()
        {
            double score = HealthScoreService.scoreSteps(6000);
            assertTrue(score >= 50.0 && score <= 75.0, "score " + score + " expected in [50,75]");
        }

        @Test
        @DisplayName("2000 steps returns 0-50")
        void test_below_fair()
        {
            double score = HealthScoreService.scoreSteps(2000);
            assertTrue(score >= 0.0 && score < 50.0, "score " + score + " expected in [0,50)");
        }
    }

    // ========== calculate() — Full Input ==========

    @Nested
    @DisplayName("TestCalculateFullInput — Complete healthy input across all dimensions")
    class TestCalculateFullInput
    {
        @Test
        @DisplayName("All 4 dimensions are present in dimension results")
        void test_all_dimensions_present()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertEquals(4, report.getDimensionResults().size());
            assertTrue(report.getDimensionResults().containsKey("heart_rate"));
            assertTrue(report.getDimensionResults().containsKey("sleep"));
            assertTrue(report.getDimensionResults().containsKey("exercise"));
            assertTrue(report.getDimensionResults().containsKey("stress"));
        }

        @Test
        @DisplayName("No missing dimensions reported")
        void test_no_missing_dimensions()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertTrue(report.getMissingDimensions().isEmpty());
        }

        @Test
        @DisplayName("Healthy input yields excellent composite score")
        void test_full_healthy_yields_excellent_composite()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertEquals("excellent", report.getCompositeGrade());
            assertTrue(report.getCompositeScore() >= 85.0);
        }

        @Test
        @DisplayName("Every dimension result has a non-empty evidence reference")
        void test_all_evidence_refs_populated()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            for (DimensionResult dim : report.getDimensionResults().values())
            {
                assertNotNull(dim.getEvidenceRef());
                assertFalse(dim.getEvidenceRef().isEmpty());
            }
        }

        @Test
        @DisplayName("All grade labels are in Chinese")
        void test_grade_labels_zh_present()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertNotNull(report.getCompositeGradeLabelZh());
            assertFalse(report.getCompositeGradeLabelZh().isEmpty());
            for (DimensionResult dim : report.getDimensionResults().values())
            {
                assertNotNull(dim.getGradeLabelZh());
                assertFalse(dim.getGradeLabelZh().isEmpty());
            }
        }

        @Test
        @DisplayName("User ID is preserved in the report")
        void test_user_id_preserved()
        {
            HealthScoreReport report = calc.calculate("test_user_abc123", fullHealthyInput());
            assertEquals("test_user_abc123", report.getUserId());
        }

        @Test
        @DisplayName("Calculated timestamp is present")
        void test_calculated_at_is_present()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertNotNull(report.getCalculatedAt());
        }

        @Test
        @DisplayName("Dimension weights are rounded to 2 decimal places")
        void test_dimension_weights_rounded_to_2_decimals()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            for (Double weight : report.getDimensionWeights().values())
            {
                // 0.3 should round to 0.3, 0.25 to 0.25, etc.
                assertEquals(weight, Math.round(weight * 100.0) / 100.0);
            }
        }

        @Test
        @DisplayName("Config version is 1.0")
        void test_config_version()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertEquals("1.0", report.getConfigVersion());
        }

        @Test
        @DisplayName("Config evidence refs list is populated")
        void test_config_evidence_refs_populated()
        {
            HealthScoreReport report = calc.calculate("user001", fullHealthyInput());
            assertNotNull(report.getConfigEvidenceRefs());
            assertTrue(report.getConfigEvidenceRefs().size() >= 4);
        }
    }

    // ========== calculate() — Poor Input ==========

    @Nested
    @DisplayName("TestCalculatePoorInput — All dimensions with poor values")
    class TestCalculatePoorInput
    {
        @Test
        @DisplayName("Poor input yields poor composite grade")
        void test_poor_input_yields_poor_composite()
        {
            HealthScoreReport report = calc.calculate("user002", poorInput());
            assertEquals("poor", report.getCompositeGrade());
            assertTrue(report.getCompositeScore() < 50.0);
        }

        @Test
        @DisplayName("Poor input has no missing dimensions")
        void test_poor_input_has_no_missing_dimensions()
        {
            HealthScoreReport report = calc.calculate("user002", poorInput());
            assertTrue(report.getMissingDimensions().isEmpty());
        }
    }

    // ========== calculate() — Single Dimension ==========

    @Nested
    @DisplayName("TestCalculateSingleDimension — Only one dimension present")
    class TestCalculateSingleDimension
    {
        @Test
        @DisplayName("Single dimension is present in results")
        void test_single_dimension_present()
        {
            HealthScoreReport report = calc.calculate("user003", singleDimensionInput());
            assertEquals(1, report.getDimensionResults().size());
            assertTrue(report.getDimensionResults().containsKey("heart_rate"));
        }

        @Test
        @DisplayName("Dimension weights in report reflect original configuration")
        void test_dimension_weights_reflect_original_config()
        {
            HealthScoreReport report = calc.calculate("user003", singleDimensionInput());
            assertTrue(report.getDimensionWeights().containsKey("heart_rate"));
            // Default heart_rate weight = 0.30
            assertEquals(0.30, report.getDimensionWeights().get("heart_rate"));
        }

        @Test
        @DisplayName("68 bpm resting HR yields a good heart_rate score (75-100)")
        void test_single_dimension_score_reflects_dimension()
        {
            HealthScoreReport report = calc.calculate("user003", singleDimensionInput());
            DimensionResult hrResult = report.getDimensionResults().get("heart_rate");
            assertNotNull(hrResult);
            // 68 bpm is in good range
            assertTrue(hrResult.getScore() >= 70.0 && hrResult.getScore() <= 100.0,
                "score " + hrResult.getScore() + " expected in [70,100]");
        }
    }

    // ========== calculate() — Missing Dimensions ==========

    @Nested
    @DisplayName("TestCalculateMissingDimensions — Partial input with some dimensions absent")
    class TestCalculateMissingDimensions
    {
        @Test
        @DisplayName("Missing dimensions are correctly identified")
        void test_missing_dimensions_are_reported()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(65);
            input.setSleepDuration(7.5);
            // exercise and stress missing

            HealthScoreReport report = calc.calculate("user004", input);
            assertTrue(report.getMissingDimensions().contains("exercise"));
            assertTrue(report.getMissingDimensions().contains("stress"));
            assertFalse(report.getMissingDimensions().contains("heart_rate"));
            assertFalse(report.getMissingDimensions().contains("sleep"));
        }

        @Test
        @DisplayName("Single dimension: composite score equals dimension score (renormalized weight)")
        void test_missing_dimension_weight_renormalization()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(65);

            HealthScoreReport report = calc.calculate("user005", input);
            Double hrScore = report.getDimensionResults().get("heart_rate").getScore();
            assertEquals(hrScore, report.getCompositeScore());
        }

        @Test
        @DisplayName("No data returns neutral 50 score")
        void test_no_data_returns_neutral_50()
        {
            HealthScoreReport report = calc.calculate("user006", new HealthScoreInput());
            assertEquals(50.0, report.getCompositeScore());
            assertEquals(0, report.getDimensionResults().size());
            assertEquals(4, report.getMissingDimensions().size());
        }
    }

    // ========== calculate() — Weight Configuration ==========

    @Nested
    @DisplayName("TestCalculateWeights — Configurable dimension weights")
    class TestCalculateWeights
    {
        @Test
        @DisplayName("Default weights sum to 1.0")
        void test_default_weights_sum_to_one()
        {
            double sum = calc.getWeightsForTesting().values().stream()
                .mapToDouble(Double::doubleValue).sum();
            assertEquals(1.0, sum, 0.001);
        }

        @Test
        @DisplayName("Custom weights are normalized")
        void test_custom_weights_normalized()
        {
            java.util.Map<String, Double> customWeights = new java.util.LinkedHashMap<>();
            customWeights.put("heart_rate", 0.40);
            customWeights.put("sleep", 0.20);
            customWeights.put("exercise", 0.20);
            customWeights.put("stress", 0.20);

            HealthScoreService customCalc = new HealthScoreService(customWeights);
            double sum = customCalc.getWeightsForTesting().values().stream()
                .mapToDouble(Double::doubleValue).sum();
            assertEquals(1.0, sum, 0.001);
            assertEquals(0.40, customCalc.getWeightsForTesting().get("heart_rate"));
        }

        @Test
        @DisplayName("Unnormalized weights are renormalized to sum to 1.0")
        void test_unnormalized_weights_are_normalized()
        {
            java.util.Map<String, Double> unnormWeights = new java.util.LinkedHashMap<>();
            unnormWeights.put("heart_rate", 2.0);
            unnormWeights.put("sleep", 1.0);

            HealthScoreService customCalc = new HealthScoreService(unnormWeights);
            double sum = customCalc.getWeightsForTesting().values().stream()
                .mapToDouble(Double::doubleValue).sum();
            assertEquals(1.0, sum, 0.001);
        }

        @Test
        @DisplayName("Custom weights affect composite score differently than defaults")
        void test_custom_weights_affect_composite()
        {
            java.util.Map<String, Double> customWeights = new java.util.LinkedHashMap<>();
            customWeights.put("heart_rate", 0.40);
            customWeights.put("sleep", 0.20);
            customWeights.put("exercise", 0.20);
            customWeights.put("stress", 0.20);

            HealthScoreService customCalc = new HealthScoreService(customWeights);

            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(65);               // good HR score
            input.setSleepDuration(8.0);          // optimal = 100
            input.setWeeklyExerciseMinutes(200.0); // excellent = 100
            input.setHrvRmssd(55.0);             // excellent = 100

            HealthScoreReport defaultReport = calc.calculate("user007", input);
            HealthScoreReport customReport = customCalc.calculate("user008", input);

            // Custom gives 40% to heart_rate vs default 30%, so composite differs
            assertNotEquals(defaultReport.getCompositeScore(), customReport.getCompositeScore());
        }
    }

    // ========== calculate() — Grade Boundaries ==========

    @Nested
    @DisplayName("TestCalculateGradeBoundaries — Composite score grade boundaries")
    class TestCalculateGradeBoundaries
    {
        @Test
        @DisplayName("All excellent scores yield excellent composite")
        void test_excellent_boundary_85()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(55);                   // excellent
            input.setSleepDuration(8.0);              // optimal = 100
            input.setPsqiGlobal(2);                   // excellent
            input.setSleepEfficiency(0.95);           // excellent
            input.setWeeklyExerciseMinutes(300.0);     // excellent
            input.setDailySteps(12000);               // excellent
            input.setHrvRmssd(80.0);                 // excellent
            input.setPerceivedStressScore(5);         // excellent

            HealthScoreReport report = calc.calculate("user009", input);
            assertEquals("excellent", report.getCompositeGrade());
        }

        @Test
        @DisplayName("Moderate scores yield good composite")
        void test_good_boundary_70()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(72);                   // good
            input.setSleepDuration(7.0);              // good
            input.setPsqiGlobal(6);                   // fair
            input.setSleepEfficiency(0.85);           // good
            input.setWeeklyExerciseMinutes(160.0);    // good
            input.setDailySteps(7800);               // good
            input.setHrvRmssd(42.0);                 // good
            input.setPerceivedStressScore(15);        // good

            HealthScoreReport report = calc.calculate("user010", input);
            assertEquals("good", report.getCompositeGrade());
        }

        @Test
        @DisplayName("Below-average scores yield fair composite")
        void test_fair_boundary_50()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(80);                   // fair
            input.setSleepDuration(6.0);              // acceptable min = 70
            input.setPsqiGlobal(9);                   // fair
            input.setSleepEfficiency(0.75);           // fair
            input.setWeeklyExerciseMinutes(80.0);     // fair
            input.setDailySteps(4500);               // fair
            input.setHrvRmssd(25.0);                 // fair
            input.setPerceivedStressScore(22);        // fair

            HealthScoreReport report = calc.calculate("user011", input);
            assertEquals("fair", report.getCompositeGrade());
        }
    }

    // ========== calculate() — Evidence References ==========

    @Nested
    @DisplayName("TestCalculateEvidenceRefs — Evidence reference compliance")
    class TestCalculateEvidenceRefs
    {
        private static final String[] ALL_EVIDENCE_REFS = {
            "acsm_2021_ch4", "hirshkowitz_2015", "buysse_1989_psqi",
            "who_2020_pa", "kim_2018_hrv_stress", "laborde_2017_hrv_biofeedback"
        };

        @Test
        @DisplayName("All dimension results have valid evidence references")
        void test_all_dimension_results_have_evidence_ref()
        {
            HealthScoreReport report = calc.calculate("user012", fullHealthyInput());
            assertEquals(4, report.getDimensionResults().size());
            for (DimensionResult dim : report.getDimensionResults().values())
            {
                boolean found = false;
                for (String ref : ALL_EVIDENCE_REFS)
                {
                    if (ref.equals(dim.getEvidenceRef()))
                    {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "evidence_ref '" + dim.getEvidenceRef() + "' not in ALL_EVIDENCE_REFS");
            }
        }

        @Test
        @DisplayName("Heart rate dimension uses acsm_2021_ch4 reference")
        void test_hr_dimension_uses_acsm_ref()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(65);
            HealthScoreReport report = calc.calculate("user014", input);
            assertEquals("acsm_2021_ch4",
                report.getDimensionResults().get("heart_rate").getEvidenceRef());
        }

        @Test
        @DisplayName("Sleep dimension uses hirshkowitz_2015 reference")
        void test_sleep_dimension_uses_hirshkowitz_ref()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setSleepDuration(7.5);
            HealthScoreReport report = calc.calculate("user015", input);
            assertEquals("hirshkowitz_2015",
                report.getDimensionResults().get("sleep").getEvidenceRef());
        }

        @Test
        @DisplayName("Exercise dimension uses who_2020_pa reference")
        void test_exercise_dimension_uses_who_ref()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setWeeklyExerciseMinutes(200.0);
            HealthScoreReport report = calc.calculate("user016", input);
            assertEquals("who_2020_pa",
                report.getDimensionResults().get("exercise").getEvidenceRef());
        }

        @Test
        @DisplayName("Stress dimension uses kim_2018_hrv_stress reference")
        void test_stress_dimension_uses_kim_ref()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setHrvRmssd(50.0);
            HealthScoreReport report = calc.calculate("user017", input);
            assertEquals("kim_2018_hrv_stress",
                report.getDimensionResults().get("stress").getEvidenceRef());
        }
    }

    // ========== calculate() — Edge Cases ==========

    @Nested
    @DisplayName("TestCalculateEdgeCases — Edge cases and boundary values")
    class TestCalculateEdgeCases
    {
        @Test
        @DisplayName("Zero exercise minutes yields a score < 50")
        void test_zero_exercise_minutes()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setWeeklyExerciseMinutes(0.0);
            HealthScoreReport report = calc.calculate("user018", input);
            assertTrue(report.getDimensionResults().containsKey("exercise"));
            double score = report.getDimensionResults().get("exercise").getScore();
            assertTrue(score >= 0.0 && score < 50.0, "score " + score + " expected in [0,50)");
        }

        @Test
        @DisplayName("Zero steps yields a score < 50")
        void test_zero_steps()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setDailySteps(0);
            HealthScoreReport report = calc.calculate("user019", input);
            double score = report.getDimensionResults().get("exercise").getScore();
            assertTrue(score < 50.0, "score " + score + " expected < 50");
        }

        @Test
        @DisplayName("PSQI global = 21 (worst) yields poor sleep grade")
        void test_max_psqi_poor_sleep()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setPsqiGlobal(21);
            HealthScoreReport report = calc.calculate("user020", input);
            assertEquals("poor",
                report.getDimensionResults().get("sleep").getGrade());
            assertTrue(report.getDimensionResults().get("sleep").getScore() < 50.0);
        }

        @Test
        @DisplayName("Very high resting HR (120) yields a valid score >= 0")
        void test_max_resting_hr()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(120);
            HealthScoreReport report = calc.calculate("user021", input);
            assertTrue(report.getDimensionResults().get("heart_rate").getScore() >= 0.0);
        }

        @Test
        @DisplayName("Very low HRV (5ms) yields a score < 50")
        void test_very_low_hrv()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setHrvRmssd(5.0);
            HealthScoreReport report = calc.calculate("user022", input);
            double score = report.getDimensionResults().get("stress").getScore();
            assertTrue(score >= 0.0 && score < 50.0, "score " + score + " expected in [0,50)");
        }

        @Test
        @DisplayName("Composite score is rounded to 1 decimal place")
        void test_composite_rounded_to_1_decimal()
        {
            HealthScoreReport report = calc.calculate("user023", fullHealthyInput());
            double score = report.getCompositeScore();
            // Check it has at most 1 decimal place
            assertEquals(score, Math.round(score * 10.0) / 10.0);
        }

        @Test
        @DisplayName("Heart rate sub-indicator score matches dimension score when only resting HR present")
        void test_heart_rate_sub_indicator_matches()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setRestingHr(62);
            HealthScoreReport report = calc.calculate("user024", input);
            DimensionResult hr = report.getDimensionResults().get("heart_rate");
            Double subScore = hr.getSubIndicators().get("resting_hr");
            assertNotNull(subScore);
            assertEquals(hr.getScore(), subScore);
        }

        @Test
        @DisplayName("Sleep sub-indicators are present when sleep fields are provided")
        void test_sleep_sub_indicators_present()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setSleepDuration(7.5);
            input.setPsqiGlobal(3);
            input.setSleepEfficiency(0.92);
            HealthScoreReport report = calc.calculate("user025", input);
            DimensionResult sleep = report.getDimensionResults().get("sleep");
            assertTrue(sleep.getSubIndicators().containsKey("sleep_duration"));
            assertTrue(sleep.getSubIndicators().containsKey("psqi_global"));
            assertTrue(sleep.getSubIndicators().containsKey("sleep_efficiency"));
        }

        @Test
        @DisplayName("Exercise sub-indicators are present when exercise fields are provided")
        void test_exercise_sub_indicators_present()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setWeeklyExerciseMinutes(200.0);
            input.setDailySteps(9000);
            HealthScoreReport report = calc.calculate("user026", input);
            DimensionResult exercise = report.getDimensionResults().get("exercise");
            assertTrue(exercise.getSubIndicators().containsKey("weekly_exercise_minutes"));
            assertTrue(exercise.getSubIndicators().containsKey("daily_steps"));
        }

        @Test
        @DisplayName("Stress sub-indicators are present when stress fields are provided")
        void test_stress_sub_indicators_present()
        {
            HealthScoreInput input = new HealthScoreInput();
            input.setHrvRmssd(55.0);
            input.setPerceivedStressScore(8);
            HealthScoreReport report = calc.calculate("user027", input);
            DimensionResult stress = report.getDimensionResults().get("stress");
            assertTrue(stress.getSubIndicators().containsKey("hrv_rmssd"));
            assertTrue(stress.getSubIndicators().containsKey("perceived_stress_score"));
        }
    }
}
