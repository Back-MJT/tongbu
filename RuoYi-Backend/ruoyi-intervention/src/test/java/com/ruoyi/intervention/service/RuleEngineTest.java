package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.enums.ExerciseGoal;
import com.ruoyi.intervention.domain.enums.ExerciseIntensity;
import com.ruoyi.intervention.domain.enums.ExerciseType;
import com.ruoyi.intervention.domain.enums.InterventionType;
import com.ruoyi.intervention.domain.enums.SleepGoal;
import com.ruoyi.intervention.domain.model.AdjustmentRule;
import com.ruoyi.intervention.domain.model.BaselineScores;
import com.ruoyi.intervention.domain.model.DailyExercise;
import com.ruoyi.intervention.domain.model.HealthProfile;
import com.ruoyi.intervention.domain.model.Prescription;
import com.ruoyi.intervention.domain.model.RiskFactors;
import com.ruoyi.intervention.domain.model.SleepRecommendation;
import com.ruoyi.intervention.domain.model.UserPreferences;

/**
 * JUnit 5 tests for RuleEngine.
 * Migrated from: intervention-engine/src/rules/engine.py
 *
 * Covers:
 *   - determineExerciseGoal() priority rules
 *   - determineSleepGoal() priority rules
 *   - applyAgeAdjustments() HR and intensity capping
 *   - generateExercisePrescription() template application
 *   - generateSleepPrescription() template application
 *   - generateInterventionPlan() multi-type generation
 */
class RuleEngineTest
{
    private RuleEngine engine;

    @BeforeEach
    void setUp()
    {
        engine = new RuleEngine();
    }

    // ========== HealthProfile helpers ==========

    private HealthProfile makeProfile(String userId, int age,
                                      BaselineScores scores, RiskFactors risks,
                                      UserPreferences prefs)
    {
        HealthProfile p = new HealthProfile(userId, age, "male");
        p.setBaselineScores(scores);
        p.setRiskFactors(risks);
        p.setPreferences(prefs);
        return p;
    }

    private BaselineScores scores(int cardio, int metabolic, int musculo, int sleep)
    {
        BaselineScores bs = new BaselineScores();
        bs.setCardiovascular(cardio);
        bs.setMetabolic(metabolic);
        bs.setMusculoskeletal(musculo);
        bs.setSleep(sleep);
        return bs;
    }

    // ========== determineExerciseGoal() ==========

    @Nested
    @DisplayName("TestDetermineExerciseGoal — Priority-ordered goal selection")
    class TestDetermineExerciseGoal
    {
        @Test
        @DisplayName("Risk sedentary+overweight → WEIGHT_LOSS")
        void test_risk_sedentary_overweight()
        {
            HealthProfile p = makeProfile("u1", 40,
                scores(70, 70, 70, 70),
                risk(true, false, false, true, false, false),
                null);
            assertEquals(ExerciseGoal.WEIGHT_LOSS, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("Risk hypertension → CARDIOVASCULAR")
        void test_risk_hypertension()
        {
            HealthProfile p = makeProfile("u2", 50,
                scores(70, 70, 70, 70),
                risk(false, false, false, false, true, false),
                null);
            assertEquals(ExerciseGoal.CARDIOVASCULAR, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("Risk cardiovascularRisk → CARDIOVASCULAR")
        void test_risk_cardiovascular()
        {
            HealthProfile p = makeProfile("u3", 45,
                scores(70, 70, 70, 70),
                risk(false, false, false, false, false, true),
                null);
            assertEquals(ExerciseGoal.CARDIOVASCULAR, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("cardiovascular score < 60 → CARDIOVASCULAR")
        void test_low_cardiovascular_score()
        {
            HealthProfile p = makeProfile("u4", 35,
                scores(55, 70, 70, 70),
                null, null);
            assertEquals(ExerciseGoal.CARDIOVASCULAR, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("musculoskeletal score < 60 → MUSCULOSKELETAL")
        void test_low_musculoskeletal_score()
        {
            HealthProfile p = makeProfile("u5", 30,
                scores(70, 70, 55, 70),
                null, null);
            assertEquals(ExerciseGoal.MUSCULOSKELETAL, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("metabolic score < 60 → WEIGHT_LOSS")
        void test_low_metabolic_score()
        {
            HealthProfile p = makeProfile("u6", 35,
                scores(70, 55, 70, 70),
                null, null);
            assertEquals(ExerciseGoal.WEIGHT_LOSS, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("All scores >= 60 → GENERAL_FITNESS")
        void test_all_good_scores()
        {
            HealthProfile p = makeProfile("u7", 30,
                scores(70, 70, 70, 70),
                null, null);
            assertEquals(ExerciseGoal.GENERAL_FITNESS, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("Null baseline scores → GENERAL_FITNESS")
        void test_null_baseline_scores()
        {
            HealthProfile p = makeProfile("u8", 30, null, null, null);
            assertEquals(ExerciseGoal.GENERAL_FITNESS, engine.determineExerciseGoal(p));
        }

        @Test
        @DisplayName("Null risk factors (scores primary) → uses score path")
        void test_null_risks()
        {
            HealthProfile p = makeProfile("u9", 40,
                scores(55, 70, 70, 70), null, null);
            assertEquals(ExerciseGoal.CARDIOVASCULAR, engine.determineExerciseGoal(p));
        }
    }

    // ========== determineSleepGoal() ==========

    @Nested
    @DisplayName("TestDetermineSleepGoal — Sleep goal selection")
    class TestDetermineSleepGoal
    {
        @Test
        @DisplayName("High stress → STRESS_SLEEP")
        void test_high_stress()
        {
            HealthProfile p = makeProfile("u1", 35,
                scores(70, 70, 70, 70),
                risk(false, true, false, false, false, false),
                null);
            assertEquals(SleepGoal.STRESS_SLEEP, engine.determineSleepGoal(p));
        }

        @Test
        @DisplayName("Sleep score < 50 → POOR_SLEEP")
        void test_low_sleep_score()
        {
            HealthProfile p = makeProfile("u2", 40,
                scores(70, 70, 70, 45),
                null, null);
            assertEquals(SleepGoal.POOR_SLEEP, engine.determineSleepGoal(p));
        }

        @Test
        @DisplayName("Null baseline scores → POOR_SLEEP")
        void test_null_scores()
        {
            HealthProfile p = makeProfile("u3", 30, null, null, null);
            assertEquals(SleepGoal.POOR_SLEEP, engine.determineSleepGoal(p));
        }

        @Test
        @DisplayName("Has sleep schedule preference → IRREGULAR_SLEEP")
        void test_irregular_sleep_schedule()
        {
            HealthProfile p = makeProfile("u4", 30,
                scores(70, 70, 70, 65),
                null, prefsWithSchedule());
            assertEquals(SleepGoal.IRREGULAR_SLEEP, engine.determineSleepGoal(p));
        }
    }

    // ========== applyAgeAdjustments() ==========

    @Nested
    @DisplayName("TestApplyAgeAdjustments — HR capping and intensity downgrade")
    class TestApplyAgeAdjustments
    {
        @Test
        @DisplayName("Age 25: no age adjustment (in vigorous bracket)")
        void test_young_no_change()
        {
            DailyExercise ex = new DailyExercise(ExerciseType.RUNNING,
                ExerciseIntensity.MODERATE, 30);
            ex.withHeartRateZone(140, 160);

            DailyExercise result = engine.applyAgeAdjustments(ex, 25);
            assertEquals(ExerciseIntensity.MODERATE, result.getIntensity());
            assertEquals(140, result.getTargetHrMin());
            assertEquals(160, result.getTargetHrMax());
        }

        @Test
        @DisplayName("Age 60: moderate HR cap, no intensity downgrade")
        void test_older_moderate_capping()
        {
            DailyExercise ex = new DailyExercise(ExerciseType.CYCLING,
                ExerciseIntensity.MODERATE, 40);
            ex.withHeartRateZone(120, 150);

            DailyExercise result = engine.applyAgeAdjustments(ex, 60);
            // 180 * 0.80 = 144 → hrMax capped to 144
            assertEquals(120, result.getTargetHrMin());
            assertTrue(result.getTargetHrMax() <= 144);
        }

        @Test
        @DisplayName("Age 55: vigorous → moderate downgrade (>50 rule)")
        void test_age_over_50_vigorous_downgrade()
        {
            DailyExercise ex = new DailyExercise(ExerciseType.HIIT,
                ExerciseIntensity.VIGOROUS, 20);
            ex.withHeartRateZone(130, 160);

            DailyExercise result = engine.applyAgeAdjustments(ex, 55);
            assertEquals(ExerciseIntensity.MODERATE, result.getIntensity());
        }

        @Test
        @DisplayName("Age 70: light intensity stays light")
        void test_age_70_light_stays_light()
        {
            DailyExercise ex = new DailyExercise(ExerciseType.WALKING,
                ExerciseIntensity.LIGHT, 30);
            ex.withHeartRateZone(90, 110);

            DailyExercise result = engine.applyAgeAdjustments(ex, 70);
            assertEquals(ExerciseIntensity.LIGHT, result.getIntensity());
        }

        @Test
        @DisplayName("Returns a copy, not the original")
        void test_returns_copy()
        {
            DailyExercise ex = new DailyExercise(ExerciseType.WALKING,
                ExerciseIntensity.MODERATE, 45);
            ex.withHeartRateZone(110, 140);

            DailyExercise result = engine.applyAgeAdjustments(ex, 65);
            assertNotSame(ex, result);
        }
    }

    // ========== generateExercisePrescription() ==========

    @Nested
    @DisplayName("TestGenerateExercisePrescription — Full prescription generation")
    class TestGenerateExercisePrescription
    {
        @Test
        @DisplayName("Prescription has 7-day plan")
        void test_seven_day_plan()
        {
            HealthProfile p = makeProfile("u1", 30, null, null, null);
            Prescription rx = engine.generateExercisePrescription(p);

            assertNotNull(rx);
            assertEquals(7, rx.getExercises().size());
        }

        @Test
        @DisplayName("Prescription ID starts with EX- prefix")
        void test_prescription_id_format()
        {
            Prescription rx = engine.generateExercisePrescription(
                makeProfile("u2", 30, null, null, null));
            assertTrue(rx.getPrescriptionId().startsWith("EX-"));
        }

        @Test
        @DisplayName("Intervention type is EXERCISE")
        void test_intervention_type_exercise()
        {
            Prescription rx = engine.generateExercisePrescription(
                makeProfile("u3", 30, null, null, null));
            assertEquals(InterventionType.EXERCISE.getCode(), rx.getInterventionType());
        }

        @Test
        @DisplayName("UserId is set from profile")
        void test_user_id_from_profile()
        {
            Prescription rx = engine.generateExercisePrescription(
                makeProfile("testUser", 30, null, null, null));
            assertEquals("testUser", rx.getUserId());
        }

        @Test
        @DisplayName("Adjustment rules are populated")
        void test_adjustment_rules_populated()
        {
            Prescription rx = engine.generateExercisePrescription(
                makeProfile("u4", 30, null, null, null));
            assertNotNull(rx.getAdjustmentRules());
            assertFalse(rx.getAdjustmentRules().isEmpty());
        }

        @Test
        @DisplayName("Generated by rule_engine")
        void test_generated_by_rule_engine()
        {
            Prescription rx = engine.generateExercisePrescription(
                makeProfile("u5", 30, null, null, null));
            assertEquals("rule_engine", rx.getGeneratedBy());
        }

        @Test
        @DisplayName("Overweight+sedentary uses WEIGHT_LOSS template")
        void test_weight_loss_template()
        {
            HealthProfile p = makeProfile("u6", 40,
                scores(70, 55, 70, 70),
                risk(true, false, false, true, false, false),
                null);
            Prescription rx = engine.generateExercisePrescription(p);
            // First day should be WALKING for weight_loss template
            assertEquals(ExerciseType.WALKING, rx.getExercises().get(0).getType());
        }

        @Test
        @DisplayName("Override goal parameter uses that template")
        void test_override_goal()
        {
            HealthProfile p = makeProfile("u7", 30, null, null, null);
            Prescription rx = engine.generateExercisePrescription(p, ExerciseGoal.CARDIOVASCULAR);
            assertEquals(ExerciseType.RUNNING, rx.getExercises().get(0).getType());
        }

        @Test
        @DisplayName("Null profile goal → determines from profile")
        void test_null_goal_defaults_to_determine()
        {
            HealthProfile p = makeProfile("u8", 35,
                scores(50, 70, 70, 70), null, null);
            Prescription rx = engine.generateExercisePrescription(p, null);
            // cardiovascular < 60 → CARDIOVASCULAR
            assertEquals(ExerciseType.RUNNING, rx.getExercises().get(0).getType());
        }
    }

    // ========== generateSleepPrescription() ==========

    @Nested
    @DisplayName("TestGenerateSleepPrescription — Sleep plan generation")
    class TestGenerateSleepPrescription
    {
        @Test
        @DisplayName("Sleep prescription has sleep plan")
        void test_has_sleep_plan()
        {
            HealthProfile p = makeProfile("u1", 30, null, null, null);
            Prescription rx = engine.generateSleepPrescription(p);
            assertNotNull(rx.getSleepPlan());
        }

        @Test
        @DisplayName("Sleep prescription ID starts with SL-")
        void test_sleep_id_format()
        {
            Prescription rx = engine.generateSleepPrescription(
                makeProfile("u2", 30, null, null, null));
            assertTrue(rx.getPrescriptionId().startsWith("SL-"));
        }

        @Test
        @DisplayName("Intervention type is SLEEP")
        void test_intervention_type_sleep()
        {
            Prescription rx = engine.generateSleepPrescription(
                makeProfile("u3", 30, null, null, null));
            assertEquals(InterventionType.SLEEP.getCode(), rx.getInterventionType());
        }

        @Test
        @DisplayName("High stress → STRESS_SLEEP goal has +0.5h for age > 50")
        void test_stress_sleep_duration_for_older()
        {
            HealthProfile p = makeProfile("u4", 60,
                scores(70, 70, 70, 40),
                risk(false, true, false, false, false, false),
                null);
            Prescription rx = engine.generateSleepPrescription(p);
            // STRESS_SLEEP template = 8.5h; age > 50 → +0.5h → 9.0h (capped)
            assertEquals(9.0, rx.getSleepPlan().getTargetDurationHours(), 0.1);
        }

        @Test
        @DisplayName("High stress → STRESS_SLEEP goal normal age")
        void test_stress_sleep_normal_age()
        {
            HealthProfile p = makeProfile("u5", 35,
                scores(70, 70, 70, 40),
                risk(false, true, false, false, false, false),
                null);
            Prescription rx = engine.generateSleepPrescription(p);
            assertEquals(8.5, rx.getSleepPlan().getTargetDurationHours(), 0.1);
        }

        @Test
        @DisplayName("Sleep hygiene advice is populated")
        void test_sleep_hygiene_populated()
        {
            Prescription rx = engine.generateSleepPrescription(
                makeProfile("u6", 30, null, null, null));
            assertNotNull(rx.getSleepPlan().getSleepHygieneAdvice());
            assertFalse(rx.getSleepPlan().getSleepHygieneAdvice().isEmpty());
        }

        @Test
        @DisplayName("Adjustment rules are populated")
        void test_sleep_adjustment_rules()
        {
            Prescription rx = engine.generateSleepPrescription(
                makeProfile("u7", 30, null, null, null));
            assertNotNull(rx.getAdjustmentRules());
            assertFalse(rx.getAdjustmentRules().isEmpty());
        }
    }

    // ========== generateInterventionPlan() ==========

    @Nested
    @DisplayName("TestGenerateInterventionPlan — Multi-type plan generation")
    class TestGenerateInterventionPlan
    {
        @Test
        @DisplayName("Default: returns exercise + sleep plans")
        void test_default_returns_both()
        {
            HealthProfile p = makeProfile("u1", 30, null, null, null);
            List<Prescription> plans = engine.generateInterventionPlan(p);
            assertEquals(2, plans.size());
        }

        @Test
        @DisplayName("Exercise-only filter")
        void test_exercise_only()
        {
            HealthProfile p = makeProfile("u2", 30, null, null, null);
            List<Prescription> plans = engine.generateInterventionPlan(p,
                java.util.Arrays.asList(InterventionType.EXERCISE.getCode()));
            assertEquals(1, plans.size());
            assertEquals(InterventionType.EXERCISE.getCode(), plans.get(0).getInterventionType());
        }

        @Test
        @DisplayName("Sleep-only filter")
        void test_sleep_only()
        {
            HealthProfile p = makeProfile("u3", 30, null, null, null);
            List<Prescription> plans = engine.generateInterventionPlan(p,
                java.util.Arrays.asList(InterventionType.SLEEP.getCode()));
            assertEquals(1, plans.size());
            assertEquals(InterventionType.SLEEP.getCode(), plans.get(0).getInterventionType());
        }

        @Test
        @DisplayName("Both exercise and sleep together")
        void test_both_together()
        {
            HealthProfile p = makeProfile("u4", 40, null, null, null);
            List<Prescription> plans = engine.generateInterventionPlan(p,
                java.util.Arrays.asList(
                    InterventionType.EXERCISE.getCode(),
                    InterventionType.SLEEP.getCode()));
            assertEquals(2, plans.size());
        }
    }

    // ========== Helper factories ==========

    private static RiskFactors risk(boolean sedentary, boolean highStress,
                                     boolean poorSleep, boolean overweight,
                                     boolean hypertension, boolean cardiovascularRisk)
    {
        RiskFactors r = new RiskFactors();
        r.setSedentary(sedentary);
        r.setHighStress(highStress);
        r.setPoorSleep(poorSleep);
        r.setOverweight(overweight);
        r.setHypertension(hypertension);
        r.setCardiovascularRisk(cardiovascularRisk);
        return r;
    }

    private static UserPreferences prefsWithSchedule()
    {
        UserPreferences p = new UserPreferences();
        Map<String, String> schedule = new java.util.HashMap<>();
        schedule.put("bedtime", "23:00");
        schedule.put("wake", "07:00");
        p.setSleepSchedule(schedule);
        return p;
    }
}
