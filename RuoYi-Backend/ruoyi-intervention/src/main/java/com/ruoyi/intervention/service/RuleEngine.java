package com.ruoyi.intervention.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ruoyi.intervention.domain.enums.ExerciseGoal;
import com.ruoyi.intervention.domain.enums.ExerciseIntensity;
import com.ruoyi.intervention.domain.enums.ExerciseType;
import com.ruoyi.intervention.domain.enums.InterventionType;
import com.ruoyi.intervention.domain.enums.SleepGoal;
import com.ruoyi.intervention.domain.model.AdjustmentRule;
import com.ruoyi.intervention.domain.model.DailyExercise;
import com.ruoyi.intervention.domain.model.HealthProfile;
import com.ruoyi.intervention.domain.model.Prescription;
import com.ruoyi.intervention.domain.model.RiskFactors;
import com.ruoyi.intervention.domain.model.SleepRecommendation;
import com.ruoyi.intervention.domain.model.BaselineScores;

/**
 * 规则引擎 — 基于循证规则生成个性化干预方案
 * Rule engine for generating personalized health intervention plans.
 *
 * Migrated from: intervention-engine/src/rules/engine.py
 * Evidence bases: ACSM 2021, WHO 2020 PA guidelines, Buysse 1989 (PSQI)
 */
@Service
public class RuleEngine
{
    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    // ========== Age bracket definitions ==========
    private static final List<AgeBracket> AGE_BRACKETS = Arrays.asList(
        new AgeBracket(18, 30, 0.90, "vigorous"),
        new AgeBracket(31, 50, 0.85, "moderate"),
        new AgeBracket(51, 65, 0.80, "moderate"),
        new AgeBracket(66, 100, 0.70, "light")
    );

    private static final double DEFAULT_MAX_HR_MULT = 0.85;

    // ========== Exercise Templates (7-day plans) ==========
    // Aligned with Python EXERCISE_TEMPLATES in engine.py lines 19-78

    private static final Map<ExerciseGoal, List<DailyExercise>> EXERCISE_TEMPLATES = new LinkedHashMap<>();
    static
    {
        // weight_loss: moderate base + 1 vigorous HIIT day
        EXERCISE_TEMPLATES.put(ExerciseGoal.WEIGHT_LOSS, Arrays.asList(
            day(1, ExerciseType.WALKING, ExerciseIntensity.MODERATE, 45, 110, 140),
            day(2, ExerciseType.CYCLING, ExerciseIntensity.MODERATE, 40, 110, 140),
            restDay(3),
            day(4, ExerciseType.SWIMMING, ExerciseIntensity.MODERATE, 35, 110, 140),
            day(5, ExerciseType.WALKING, ExerciseIntensity.MODERATE, 50, 110, 140),
            day(6, ExerciseType.HIIT, ExerciseIntensity.VIGOROUS, 25, 120, 150),
            restDay(7)
        ));

        // cardiovascular: higher intensity, longer durations
        EXERCISE_TEMPLATES.put(ExerciseGoal.CARDIOVASCULAR, Arrays.asList(
            day(1, ExerciseType.RUNNING, ExerciseIntensity.MODERATE, 35, 100, 150),
            day(2, ExerciseType.CYCLING, ExerciseIntensity.MODERATE, 45, 100, 150),
            day(3, ExerciseType.SWIMMING, ExerciseIntensity.MODERATE, 40, 100, 150),
            restDay(4),
            day(5, ExerciseType.RUNNING, ExerciseIntensity.VIGOROUS, 30, 120, 160),
            day(6, ExerciseType.HIIT, ExerciseIntensity.VIGOROUS, 25, 130, 170),
            day(7, ExerciseType.WALKING, ExerciseIntensity.LIGHT, 40, 100, 130)
        ));

        // musculoskeletal: strength + flexibility
        EXERCISE_TEMPLATES.put(ExerciseGoal.MUSCULOSKELETAL, Arrays.asList(
            day(1, ExerciseType.STRENGTH_TRAINING, ExerciseIntensity.MODERATE, 40, 80, 105),
            day(2, ExerciseType.STRETCHING, ExerciseIntensity.LIGHT, 30, 80, 100),
            day(3, ExerciseType.STRENGTH_TRAINING, ExerciseIntensity.MODERATE, 40, 80, 105),
            restDay(4),
            day(5, ExerciseType.STRENGTH_TRAINING, ExerciseIntensity.MODERATE, 40, 80, 105),
            day(6, ExerciseType.YOGA, ExerciseIntensity.LIGHT, 35, 80, 100),
            day(7, ExerciseType.WALKING, ExerciseIntensity.LIGHT, 30, 80, 100)
        ));

        // general_fitness: light-to-moderate balanced
        EXERCISE_TEMPLATES.put(ExerciseGoal.GENERAL_FITNESS, Arrays.asList(
            day(1, ExerciseType.WALKING, ExerciseIntensity.LIGHT, 30, 80, 110),
            day(2, ExerciseType.CYCLING, ExerciseIntensity.MODERATE, 30, 80, 110),
            restDay(3),
            day(4, ExerciseType.SWIMMING, ExerciseIntensity.MODERATE, 35, 80, 110),
            day(5, ExerciseType.WALKING, ExerciseIntensity.LIGHT, 40, 80, 110),
            day(6, ExerciseType.YOGA, ExerciseIntensity.LIGHT, 30, 80, 100),
            restDay(7)
        ));
    }

    // ========== Sleep Templates ==========
    private static final Map<SleepGoal, SleepTemplate> SLEEP_TEMPLATES = new LinkedHashMap<>();
    static
    {
        SLEEP_TEMPLATES.put(SleepGoal.POOR_SLEEP, new SleepTemplate(
            LocalTime.of(22, 0), LocalTime.of(6, 0), 8.0,
            Arrays.asList("睡前1小时停止使用电子设备", "进行15分钟轻度拉伸", "调暗灯光准备入睡", "上床放松呼吸"),
            envMap("18-20℃", "完全黑暗", "<30dB")
        ));

        SLEEP_TEMPLATES.put(SleepGoal.IRREGULAR_SLEEP, new SleepTemplate(
            LocalTime.of(22, 30), LocalTime.of(6, 30), 8.0,
            Arrays.asList("停止工作或学习", "进行放松活动", "调暗灯光", "按时上床"),
            envMap("19-21℃", "完全黑暗", "<35dB")
        ));

        SLEEP_TEMPLATES.put(SleepGoal.STRESS_SLEEP, new SleepTemplate(
            LocalTime.of(22, 0), LocalTime.of(6, 30), 8.5,
            Arrays.asList("轻度运动或散步", "冥想或深呼吸练习", "避免刺激性内容", "放松身心准备入睡"),
            envMap("18-20℃", "完全黑暗", "白噪音")
        ));
    }

    // ========== Public API ==========

    /**
     * Determine exercise goal from health profile.
     * Priority-ordered rules based on risk factors and baseline scores.
     *
     * Reference: engine.py determine_exercise_goal()
     */
    public ExerciseGoal determineExerciseGoal(HealthProfile profile)
    {
        BaselineScores scores = profile.getBaselineScores();
        if (scores == null)
        {
            return ExerciseGoal.GENERAL_FITNESS;
        }

        RiskFactors risks = profile.getRiskFactors();
        if (risks != null)
        {
            if (risks.isSedentary() && risks.isOverweight())
            {
                return ExerciseGoal.WEIGHT_LOSS;
            }
            if (risks.isHypertension() || risks.isCardiovascularRisk())
            {
                return ExerciseGoal.CARDIOVASCULAR;
            }
        }

        if (scores.getCardiovascular() < 60)
        {
            return ExerciseGoal.CARDIOVASCULAR;
        }
        if (scores.getMusculoskeletal() < 60)
        {
            return ExerciseGoal.MUSCULOSKELETAL;
        }
        if (scores.getMetabolic() < 60)
        {
            return ExerciseGoal.WEIGHT_LOSS;
        }

        return ExerciseGoal.GENERAL_FITNESS;
    }

    /**
     * Determine sleep goal from health profile.
     *
     * Reference: engine.py determine_sleep_goal()
     */
    public SleepGoal determineSleepGoal(HealthProfile profile)
    {
        BaselineScores scores = profile.getBaselineScores();
        if (scores == null)
        {
            return SleepGoal.POOR_SLEEP;
        }

        RiskFactors risks = profile.getRiskFactors();
        if (risks != null && risks.isHighStress())
        {
            return SleepGoal.STRESS_SLEEP;
        }

        if (scores.getSleep() < 50)
        {
            return SleepGoal.POOR_SLEEP;
        }

        if (profile.getPreferences() != null && profile.getPreferences().getSleepSchedule() != null)
        {
            return SleepGoal.IRREGULAR_SLEEP;
        }

        return SleepGoal.POOR_SLEEP;
    }

    /**
     * Apply age-based adjustments to an exercise.
     * Adjusts target heart rate and may downgrade intensity for older adults.
     *
     * Reference: engine.py apply_age_adjustments()
     *
     * @param exercise the exercise to adjust (a COPY is returned; original is not modified)
     * @param age the person's age
     * @return adjusted DailyExercise
     */
    public DailyExercise applyAgeAdjustments(DailyExercise exercise, int age)
    {
        // Defensive copy
        DailyExercise adjusted = copyExercise(exercise);

        double maxHrMult = DEFAULT_MAX_HR_MULT;
        String intensityCap = "vigorous";

        for (AgeBracket bracket : AGE_BRACKETS)
        {
            if (age >= bracket.minAge && age <= bracket.maxAge)
            {
                maxHrMult = bracket.maxHrMult;
                intensityCap = bracket.intensityCap;
                break;
            }
        }

        // Adjust target HR
        Integer hrMin = adjusted.getTargetHrMin();
        if (hrMin != null)
        {
            int newTarget = (int) (180 * maxHrMult);
            adjusted.setTargetHrMin(Math.min(hrMin, newTarget));
        }
        Integer hrMax = adjusted.getTargetHrMax();
        if (hrMax != null)
        {
            int newTarget = (int) (180 * maxHrMult);
            adjusted.setTargetHrMax(Math.min(hrMax, newTarget));
        }

        // Downgrade vigorous to moderate for age > 50
        if (age > 50 && adjusted.getIntensity() == ExerciseIntensity.VIGOROUS)
        {
            adjusted.setIntensity(ExerciseIntensity.MODERATE);
        }

        return adjusted;
    }

    /**
     * Generate a full exercise prescription for the given profile.
     *
     * Reference: engine.py generate_exercise_prescription()
     */
    public Prescription generateExercisePrescription(HealthProfile profile)
    {
        return generateExercisePrescription(profile, null);
    }

    public Prescription generateExercisePrescription(HealthProfile profile, ExerciseGoal goal)
    {
        if (goal == null)
        {
            goal = determineExerciseGoal(profile);
        }

        List<DailyExercise> template = EXERCISE_TEMPLATES.get(goal);
        if (template == null)
        {
            template = EXERCISE_TEMPLATES.get(ExerciseGoal.GENERAL_FITNESS);
        }

        // Deep copy and apply age adjustments
        List<DailyExercise> adjustedPlan = new ArrayList<>();
        for (DailyExercise ex : template)
        {
            adjustedPlan.add(applyAgeAdjustments(ex, profile.getAge()));
        }

        // Build adjustment rules
        List<AdjustmentRule> rules = Arrays.asList(
            new AdjustmentRule("resting_HR_increase > 10 bpm", "reduce_intensity_by_20%"),
            new AdjustmentRule("exercise_compliance < 50%", "reduce_duration_by_30%"),
            new AdjustmentRule("subjective_difficulty > 8", "lower_intensity_one_level")
        );

        // Generate prescription ID: EX-{YYYYMMDD}-{uuid6}
        String prescriptionId = generatePrescriptionId("EX");

        Prescription p = new Prescription();
        p.setPrescriptionId(prescriptionId);
        p.setUserId(profile.getUserId());
        p.setInterventionType(InterventionType.EXERCISE.getCode());
        p.setCreatedAt(LocalDateTime.now());
        p.setPlanDate(LocalDate.now());
        p.setExercises(adjustedPlan);
        p.setAdjustmentRules(rules);
        p.setDurationDays(7);
        p.setGeneratedBy("rule_engine");

        return p;
    }

    /**
     * Generate a full sleep prescription for the given profile.
     *
     * Reference: engine.py generate_sleep_prescription()
     */
    public Prescription generateSleepPrescription(HealthProfile profile)
    {
        return generateSleepPrescription(profile, null);
    }

    public Prescription generateSleepPrescription(HealthProfile profile, SleepGoal goal)
    {
        if (goal == null)
        {
            goal = determineSleepGoal(profile);
        }

        SleepTemplate tmpl = SLEEP_TEMPLATES.get(goal);
        if (tmpl == null)
        {
            tmpl = SLEEP_TEMPLATES.get(SleepGoal.POOR_SLEEP);
        }

        // Build sleep recommendation from template
        SleepRecommendation sleepRec = new SleepRecommendation();
        sleepRec.setTargetBedtime(tmpl.bedtime);
        sleepRec.setTargetWakeTime(tmpl.wakeTime);

        // Age adjustment: > 50 gets +0.5h, capped at 9h
        double duration = tmpl.sleepDurationHours;
        if (profile.getAge() > 50)
        {
            duration = Math.min(9.0, duration + 0.5);
        }
        sleepRec.setTargetDurationHours(duration);

        // Build sleep hygiene advice from routine steps
        sleepRec.setSleepHygieneAdvice(String.join("；", tmpl.preSleepRoutine) +
            "。环境建议: 温度" + tmpl.environment.get("temperature") +
            "，" + tmpl.environment.get("lighting") +
            "，噪音" + tmpl.environment.get("noise"));

        // Build adjustment rules
        List<AdjustmentRule> rules = Arrays.asList(
            new AdjustmentRule("sleep_duration < 6 hours", "extend_bedtime_by_30min"),
            new AdjustmentRule("sleep_quality_score < 40", "add_relaxation_routine")
        );

        String prescriptionId = generatePrescriptionId("SL");

        Prescription p = new Prescription();
        p.setPrescriptionId(prescriptionId);
        p.setUserId(profile.getUserId());
        p.setInterventionType(InterventionType.SLEEP.getCode());
        p.setCreatedAt(LocalDateTime.now());
        p.setPlanDate(LocalDate.now());
        p.setSleepPlan(sleepRec);
        p.setAdjustmentRules(rules);
        p.setDurationDays(7);
        p.setGeneratedBy("rule_engine");

        return p;
    }

    /**
     * Generate a comprehensive intervention plan covering multiple types.
     *
     * Reference: engine.py generate_intervention_plan()
     */
    public List<Prescription> generateInterventionPlan(HealthProfile profile)
    {
        return generateInterventionPlan(profile, null);
    }

    public List<Prescription> generateInterventionPlan(HealthProfile profile, List<String> interventionTypes)
    {
        if (interventionTypes == null || interventionTypes.isEmpty())
        {
            interventionTypes = Arrays.asList(
                InterventionType.EXERCISE.getCode(),
                InterventionType.SLEEP.getCode()
            );
        }

        List<Prescription> plans = new ArrayList<>();
        for (String type : interventionTypes)
        {
            if (InterventionType.EXERCISE.getCode().equals(type))
            {
                plans.add(generateExercisePrescription(profile));
            }
            else if (InterventionType.SLEEP.getCode().equals(type))
            {
                plans.add(generateSleepPrescription(profile));
            }
        }
        return plans;
    }

    // ========== Private Helpers ==========

    private String generatePrescriptionId(String prefix)
    {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid6 = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix + "-" + dateStr + "-" + uuid6;
    }

    /**
     * Deep copy a DailyExercise.
     */
    private DailyExercise copyExercise(DailyExercise src)
    {
        DailyExercise copy = new DailyExercise();
        copy.setType(src.getType());
        copy.setIntensity(src.getIntensity());
        copy.setDurationMinutes(src.getDurationMinutes());
        copy.setTargetHrMin(src.getTargetHrMin());
        copy.setTargetHrMax(src.getTargetHrMax());
        copy.setSets(src.getSets());
        copy.setRepsPerSet(src.getRepsPerSet());
        copy.setNotes(src.getNotes());
        return copy;
    }

    // Builder helpers for template definitions
    private static DailyExercise day(int day, ExerciseType type, ExerciseIntensity intensity,
                                      int duration, int hrMin, int hrMax)
    {
        DailyExercise ex = new DailyExercise(type, intensity, duration);
        ex.setNotes("Day " + day);
        return ex.withHeartRateZone(hrMin, hrMax);
    }

    private static DailyExercise restDay(int day)
    {
        DailyExercise ex = new DailyExercise(ExerciseType.STRETCHING, ExerciseIntensity.LIGHT, 15);
        ex.setNotes("Day " + day + " — 休息日，轻度活动");
        return ex;
    }

    private static Map<String, String> envMap(String temperature, String lighting, String noise)
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("temperature", temperature);
        map.put("lighting", lighting);
        map.put("noise", noise);
        return map;
    }

    // ========== Inner Data Classes ==========

    private static class AgeBracket
    {
        final int minAge;
        final int maxAge;
        final double maxHrMult;
        final String intensityCap;

        AgeBracket(int minAge, int maxAge, double maxHrMult, String intensityCap)
        {
            this.minAge = minAge;
            this.maxAge = maxAge;
            this.maxHrMult = maxHrMult;
            this.intensityCap = intensityCap;
        }
    }

    private static class SleepTemplate
    {
        final LocalTime bedtime;
        final LocalTime wakeTime;
        final double sleepDurationHours;
        final List<String> preSleepRoutine;
        final Map<String, String> environment;

        SleepTemplate(LocalTime bedtime, LocalTime wakeTime, double sleepDurationHours,
                      List<String> preSleepRoutine, Map<String, String> environment)
        {
            this.bedtime = bedtime;
            this.wakeTime = wakeTime;
            this.sleepDurationHours = sleepDurationHours;
            this.preSleepRoutine = preSleepRoutine;
            this.environment = environment;
        }
    }
}
