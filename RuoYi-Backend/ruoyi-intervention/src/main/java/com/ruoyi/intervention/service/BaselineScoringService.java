package com.ruoyi.intervention.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 基线健康评分 — 四维度量化评估
 * Baseline health scoring across 4 dimensions: cardiovascular, metabolic, sleep, musculoskeletal.
 *
 * Migrated from: intervention-engine/src/algorithms/baseline_scoring.py
 * Evidence: GB/T 10000-2023, ACSM 2021 Ch.4, Buysse 1989 (PSQI), WHO 2020
 */
@Service
public class BaselineScoringService
{
    private static final Logger log = LoggerFactory.getLogger(BaselineScoringService.class);

    // ========== Grade definitions ==========
    private static final double[][] GRADE_BOUNDS = {
        {85.0, 100.0},  // excellent
        {70.0, 84.9},   // good
        {50.0, 69.9},   // fair
        {0.0, 49.9}     // poor
    };
    private static final String[] GRADE_NAMES = {"excellent", "good", "fair", "poor"};
    private static final String[] GRADE_LABELS_ZH = {"优秀", "良好", "一般", "较差"};

    // ========== Default dimension weights ==========
    private static final Map<String, Double> DEFAULT_WEIGHTS = new LinkedHashMap<>();
    static
    {
        DEFAULT_WEIGHTS.put("cardiovascular", 0.30);
        DEFAULT_WEIGHTS.put("metabolic", 0.25);
        DEFAULT_WEIGHTS.put("sleep", 0.20);
        DEFAULT_WEIGHTS.put("musculoskeletal", 0.25);
    }

    // ========== Cardiovascular thresholds (lower is better) ==========
    // step_test_recovery_hr, resting_hr, systolic_bp
    private static final double[][] CARDIO_THRESHOLDS = {
        // excellent, good, fair (lower is better; poor = > fair)
        {110, 130, 150},  // step_test_recovery_hr (GB/T 10000-2023)
        {60, 70, 80},     // resting_hr (ACSM 2021 Ch.4)
        {110, 120, 140}   // systolic_bp (ACSM 2021 Ch.4)
    };

    // ========== Metabolic thresholds ==========
    // BMI scoring: range-based with optimal midpoint 21.5
    private static final double BMI_OPTIMAL_LOW = 18.5;
    private static final double BMI_OPTIMAL_HIGH = 23.9;
    private static final double BMI_OPTIMAL_MID = 21.5;
    private static final double BMI_MAX_DISTANCE = (23.9 - 18.5) / 2.0;  // 2.7

    // Waist circumference (lower is better, gender-specific)
    private static final double[][] WAIST_THRESHOLDS = {
        // male: excellent, good, fair
        {85.0, 90.0, 95.0},
        // female: excellent, good, fair
        {80.0, 85.0, 90.0}
    };

    // Body fat % (range-based, gender-specific)
    private static final double[][] BODY_FAT_EXCELLENT = {
        {6.0, 13.0},   // male
        {14.0, 20.0}   // female
    };
    private static final double[][] BODY_FAT_GOOD = {
        {14.0, 17.0},  // male
        {21.0, 24.0}   // female
    };

    // ========== Sleep thresholds ==========
    private static final double SLEEP_OPTIMAL_MIN = 7.0;
    private static final double SLEEP_OPTIMAL_MAX = 9.0;
    private static final double SLEEP_ACCEPTABLE_MIN = 6.0;
    private static final double SLEEP_ACCEPTABLE_MAX = 10.0;

    // PSQI (lower is better)
    private static final double PSQI_EXCELLENT = 3.0;
    private static final double PSQI_GOOD = 5.0;
    private static final double PSQI_FAIR = 8.0;

    // Sleep efficiency (0-1 scale, higher is better)
    private static final double SLEEP_EFF_EXCELLENT = 0.90;
    private static final double SLEEP_EFF_GOOD = 0.85;
    private static final double SLEEP_EFF_FAIR = 0.75;

    // ========== Musculoskeletal thresholds (higher is better, gender-specific) ==========
    // [male_excellent, male_good, male_fair], [female_excellent, female_good, female_fair]
    private static final double[][] GRIP_THRESHOLDS = {{50.0, 40.0, 30.0}, {33.0, 25.0, 18.0}};
    private static final double[][] SIT_REACH_THRESHOLDS = {{20.0, 10.0, 0.0}, {25.0, 15.0, 5.0}};
    private static final double[][] LOWER_LIMB_THRESHOLDS = {{250.0, 210.0, 170.0}, {190.0, 160.0, 130.0}};
    private static final double[][] CHAIR_STAND_THRESHOLDS = {{25.0, 20.0, 15.0}, {22.0, 17.0, 12.0}};

    // ========== Instance state ==========
    private final Map<String, Double> weights;

    public BaselineScoringService()
    {
        this(DEFAULT_WEIGHTS);
    }

    public BaselineScoringService(Map<String, Double> weights)
    {
        this.weights = (weights != null) ? weights : DEFAULT_WEIGHTS;
    }

    // ========== Input / Output types ==========

    /**
     * Raw baseline measurements (all optional).
     */
    public static class BaselineMeasurements
    {
        public Integer stepTestRecoveryHr;
        public Integer restingHr;
        public Integer systolicBp;
        public Double bmi;
        public Double waistCircumference;
        public Double bodyFatPct;
        public Double sleepDuration;       // hours
        public Integer psqiGlobal;         // 0-21
        public Double sleepEfficiency;     // 0-100 percentage (will be normalized to 0-1)
        public Double gripStrength;        // kg
        public Double sitAndReach;         // cm
        public Double lowerLimbPowerCm;    // standing long jump cm
        public Integer chairStandReps;     // 30s reps
    }

    /**
     * Single dimension score.
     */
    public static class DimensionScore
    {
        public String dimension;
        public double totalScore;          // 0-100
        public String grade;
        public String gradeLabelZh;
        public Map<String, Double> subIndicators = new LinkedHashMap<>();
        public String evidenceRef;

        public DimensionScore(String dimension, double totalScore)
        {
            this.dimension = dimension;
            this.totalScore = round1(totalScore);
            this.grade = assignGrade(this.totalScore);
            this.gradeLabelZh = gradeLabelZh(this.grade);
        }
    }

    /**
     * Full composite baseline report.
     */
    public static class CompositeBaselineReport
    {
        public String userId;
        public LocalDateTime assessedAt;
        public Map<String, DimensionScore> dimensionScores = new LinkedHashMap<>();
        public Double compositeScore;
        public String compositeGrade;
        public String compositeGradeLabelZh;
        public String configVersion = "1.0";
        public List<String> configEvidenceRefs = new ArrayList<>();
        public List<String> missingIndicators = new ArrayList<>();
    }

    // ========== Public API ==========

    /**
     * Run full baseline assessment across all 4 dimensions.
     *
     * Reference: baseline_scoring.py BaselineScorer.assess()
     */
    public CompositeBaselineReport assess(String userId, BaselineMeasurements m, String gender)
    {
        CompositeBaselineReport report = new CompositeBaselineReport();
        report.userId = userId;
        report.assessedAt = LocalDateTime.now();

        // Cardiovascular
        DimensionScore cardio = scoreCardiovascular(m);
        if (cardio != null)
        {
            report.dimensionScores.put("cardiovascular", cardio);
        }
        else
        {
            report.missingIndicators.add("cardiovascular");
        }

        // Metabolic
        DimensionScore metabolic = scoreMetabolic(m, gender);
        if (metabolic != null)
        {
            report.dimensionScores.put("metabolic", metabolic);
        }
        else
        {
            report.missingIndicators.add("metabolic");
        }

        // Sleep
        DimensionScore sleep = scoreSleep(m);
        if (sleep != null)
        {
            report.dimensionScores.put("sleep", sleep);
        }
        else
        {
            report.missingIndicators.add("sleep");
        }

        // Musculoskeletal
        DimensionScore musculo = scoreMusculoskeletal(m, gender);
        if (musculo != null)
        {
            report.dimensionScores.put("musculoskeletal", musculo);
        }
        else
        {
            report.missingIndicators.add("musculoskeletal");
        }

        // Evidence references
        report.configEvidenceRefs = Arrays.asList(
            "china_gbt_10000_2023",
            "acsm_2021_ch4",
            "buysse_1989_psqi",
            "hirshkowitz_2015",
            "who_2020_pa"
        );

        // Composite
        Double composite = computeComposite(report.dimensionScores);
        report.compositeScore = composite;
        if (composite != null)
        {
            report.compositeGrade = assignGrade(composite);
            report.compositeGradeLabelZh = gradeLabelZh(report.compositeGrade);
        }

        return report;
    }

    /**
     * Compare two reports and compute progress deltas.
     *
     * Reference: baseline_scoring.py BaselineScorer.score_progress()
     */
    public Map<String, Object> scoreProgress(CompositeBaselineReport before, CompositeBaselineReport after)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Double> dimensionDeltas = new LinkedHashMap<>();
        List<String> improved = new ArrayList<>();
        List<String> declined = new ArrayList<>();

        for (String dim : new String[]{"cardiovascular", "metabolic", "sleep", "musculoskeletal"})
        {
            DimensionScore beforeScore = before.dimensionScores.get(dim);
            DimensionScore afterScore = after.dimensionScores.get(dim);
            if (beforeScore != null && afterScore != null)
            {
                double delta = round1(afterScore.totalScore - beforeScore.totalScore);
                dimensionDeltas.put(dim, delta);
                if (delta > 0) improved.add(dim);
                else if (delta < 0) declined.add(dim);
            }
        }

        double compositeDelta = 0.0;
        if (before.compositeScore != null && after.compositeScore != null)
        {
            compositeDelta = round1(after.compositeScore - before.compositeScore);
        }

        result.put("dimension_deltas", dimensionDeltas);
        result.put("composite_delta", compositeDelta);
        result.put("improved_dimensions", improved);
        result.put("declined_dimensions", declined);
        result.put("before_composite", before.compositeScore);
        result.put("after_composite", after.compositeScore);

        return result;
    }

    // ========== Dimension Scorers ==========

    DimensionScore scoreCardiovascular(BaselineMeasurements m)
    {
        Map<String, Double> subs = new LinkedHashMap<>();
        boolean hasData = false;

        if (m.stepTestRecoveryHr != null)
        {
            subs.put("step_test_recovery_hr", scoreLowerIsBetter(m.stepTestRecoveryHr, CARDIO_THRESHOLDS[0]));
            hasData = true;
        }
        if (m.restingHr != null)
        {
            subs.put("resting_hr", scoreLowerIsBetter(m.restingHr, CARDIO_THRESHOLDS[1]));
            hasData = true;
        }
        if (m.systolicBp != null)
        {
            subs.put("systolic_bp", scoreLowerIsBetter(m.systolicBp, CARDIO_THRESHOLDS[2]));
            hasData = true;
        }

        if (!hasData) return null;

        DimensionScore ds = new DimensionScore("cardiovascular", average(subs.values()));
        ds.subIndicators = subs;
        ds.evidenceRef = "china_gbt_10000_2023;acsm_2021_ch4";
        return ds;
    }

    DimensionScore scoreMetabolic(BaselineMeasurements m, String gender)
    {
        Map<String, Double> subs = new LinkedHashMap<>();
        boolean hasData = false;

        if (m.bmi != null)
        {
            subs.put("bmi", scoreBmi(m.bmi));
            hasData = true;
        }
        if (m.waistCircumference != null)
        {
            subs.put("waist_circumference", scoreWaist(m.waistCircumference, gender));
            hasData = true;
        }
        if (m.bodyFatPct != null)
        {
            subs.put("body_fat_pct", scoreBodyFat(m.bodyFatPct, gender));
            hasData = true;
        }

        if (!hasData) return null;

        DimensionScore ds = new DimensionScore("metabolic", average(subs.values()));
        ds.subIndicators = subs;
        ds.evidenceRef = "acsm_2021_ch4;who_2020";
        return ds;
    }

    DimensionScore scoreSleep(BaselineMeasurements m)
    {
        Map<String, Double> subs = new LinkedHashMap<>();
        boolean hasData = false;

        if (m.sleepDuration != null)
        {
            subs.put("sleep_duration", scoreSleepDuration(m.sleepDuration));
            hasData = true;
        }
        if (m.psqiGlobal != null)
        {
            subs.put("psqi_global", scoreLowerIsBetter(m.psqiGlobal, new double[]{PSQI_EXCELLENT, PSQI_GOOD, PSQI_FAIR}));
            hasData = true;
        }
        if (m.sleepEfficiency != null)
        {
            // Normalize 0-100 percentage to 0-1 fraction for threshold comparison
            double effFraction = m.sleepEfficiency / 100.0;
            subs.put("sleep_efficiency", scoreHigherIsBetter(effFraction, new double[]{SLEEP_EFF_FAIR, SLEEP_EFF_GOOD, SLEEP_EFF_EXCELLENT}));
            hasData = true;
        }

        if (!hasData) return null;

        DimensionScore ds = new DimensionScore("sleep", average(subs.values()));
        ds.subIndicators = subs;
        ds.evidenceRef = "buysse_1989_psqi;hirshkowitz_2015";
        return ds;
    }

    DimensionScore scoreMusculoskeletal(BaselineMeasurements m, String gender)
    {
        Map<String, Double> subs = new LinkedHashMap<>();
        boolean hasData = false;
        int genderIdx = "female".equalsIgnoreCase(gender) ? 1 : 0;

        if (m.gripStrength != null)
        {
            subs.put("grip_strength", scoreHigherIsBetterGender(m.gripStrength, GRIP_THRESHOLDS[genderIdx]));
            hasData = true;
        }
        if (m.sitAndReach != null)
        {
            subs.put("sit_and_reach", scoreHigherIsBetterGender(m.sitAndReach, SIT_REACH_THRESHOLDS[genderIdx]));
            hasData = true;
        }
        if (m.lowerLimbPowerCm != null)
        {
            subs.put("lower_limb_power_cm", scoreHigherIsBetterGender(m.lowerLimbPowerCm, LOWER_LIMB_THRESHOLDS[genderIdx]));
            hasData = true;
        }
        if (m.chairStandReps != null)
        {
            subs.put("chair_stand_reps", scoreHigherIsBetterGender((double) m.chairStandReps, CHAIR_STAND_THRESHOLDS[genderIdx]));
            hasData = true;
        }

        if (!hasData) return null;

        DimensionScore ds = new DimensionScore("musculoskeletal", average(subs.values()));
        ds.subIndicators = subs;
        ds.evidenceRef = "china_gbt_10000_2023";
        return ds;
    }

    // ========== Scoring Primitives ==========

    /**
     * Lower raw value = better score.
     * Mapping: <= excellent → 100, <= good → [75,100), <= fair → [50,75), > fair → [0,50)
     *
     * Reference: baseline_scoring.py _score_lower_is_better()
     */
    static double scoreLowerIsBetter(double value, double[] thresholds)
    {
        // thresholds = [excellent, good, fair]
        double excellent = thresholds[0];
        double good = thresholds[1];
        double fair = thresholds[2];

        if (value <= excellent)
        {
            return 100.0;
        }
        else if (value <= good)
        {
            return 100.0 - (value - excellent) / (good - excellent) * 25.0;
        }
        else if (value <= fair)
        {
            return 75.0 - (value - good) / (fair - good) * 25.0;
        }
        else
        {
            double overshoot = Math.min(value - fair, fair);
            return Math.max(0.0, 50.0 - overshoot / fair * 50.0);
        }
    }

    /**
     * Higher raw value = better score.
     * thresholds = [fair, good, excellent] (ascending order)
     *
     * Reference: baseline_scoring.py _score_higher_is_better_pct()
     */
    static double scoreHigherIsBetter(double value, double[] thresholds)
    {
        // thresholds = [fair, good, excellent]
        double fair = thresholds[0];
        double good = thresholds[1];
        double excellent = thresholds[2];

        if (value >= excellent)
        {
            return 100.0;
        }
        else if (value >= good)
        {
            return 75.0 + (value - good) / (excellent - good) * 25.0;
        }
        else if (value >= fair)
        {
            return 50.0 + (value - fair) / (good - fair) * 25.0;
        }
        else
        {
            if (fair > 0)
            {
                return Math.max(0.0, value / fair * 50.0);
            }
            return 0.0;
        }
    }

    /**
     * Higher is better with gender-specific thresholds.
     * thresholds = [excellent, good, fair] (descending order)
     *
     * Reference: baseline_scoring.py _score_higher_is_better_gender()
     */
    static double scoreHigherIsBetterGender(double value, double[] thresholds)
    {
        // thresholds = [excellent, good, fair]
        double excellent = thresholds[0];
        double good = thresholds[1];
        double fair = thresholds[2];

        if (value >= excellent)
        {
            return 100.0;
        }
        else if (value >= good)
        {
            return 75.0 + (value - good) / (excellent - good) * 25.0;
        }
        else if (value >= fair)
        {
            return 50.0 + (value - fair) / (good - fair) * 25.0;
        }
        else
        {
            if (fair > 0)
            {
                return Math.max(0.0, value / fair * 50.0);
            }
            return 0.0;
        }
    }

    /**
     * BMI scoring: range-based with optimal midpoint.
     *
     * Reference: baseline_scoring.py _score_bmi()
     */
    static double scoreBmi(double bmi)
    {
        if (bmi >= BMI_OPTIMAL_LOW && bmi <= BMI_OPTIMAL_HIGH)
        {
            // Optimal range: 85-100
            double distance = Math.abs(bmi - BMI_OPTIMAL_MID);
            return 100.0 - (distance / BMI_MAX_DISTANCE) * 15.0;
        }
        else if (bmi < BMI_OPTIMAL_LOW)
        {
            // Underweight
            return Math.max(0.0, 70.0 - (BMI_OPTIMAL_LOW - bmi) * 10.0);
        }
        else
        {
            // Overweight/obese
            return Math.max(0.0, 70.0 - (bmi - BMI_OPTIMAL_HIGH) * 8.0);
        }
    }

    /**
     * Waist circumference scoring: lower is better, gender-specific.
     *
     * Reference: baseline_scoring.py _score_waist()
     */
    static double scoreWaist(double waist, String gender)
    {
        int genderIdx = "female".equalsIgnoreCase(gender) ? 1 : 0;
        double excellent = WAIST_THRESHOLDS[genderIdx][0];
        double good = WAIST_THRESHOLDS[genderIdx][1];
        double fair = WAIST_THRESHOLDS[genderIdx][2];

        if (waist <= excellent)
        {
            return 100.0;
        }
        else if (waist <= good)
        {
            return 75.0 + (good - waist) / (good - excellent) * 25.0;
        }
        else if (waist <= fair)
        {
            return 50.0 + (fair - waist) / (fair - good) * 25.0;
        }
        else
        {
            return Math.max(0.0, 50.0 - (waist - fair) / fair * 50.0);
        }
    }

    /**
     * Body fat percentage scoring: range-based, gender-specific.
     *
     * Reference: baseline_scoring.py _score_body_fat()
     */
    static double scoreBodyFat(double pct, String gender)
    {
        int genderIdx = "female".equalsIgnoreCase(gender) ? 1 : 0;
        double[] excRange = BODY_FAT_EXCELLENT[genderIdx];
        double[] goodRange = BODY_FAT_GOOD[genderIdx];

        if (pct >= excRange[0] && pct <= excRange[1])
        {
            return 100.0;
        }
        else if (pct >= goodRange[0] && pct <= goodRange[1])
        {
            double distance = Math.min(Math.abs(pct - excRange[0]), Math.abs(pct - excRange[1]));
            return 85.0 - distance * 3.0;
        }
        else if (pct < goodRange[0])
        {
            return Math.max(0.0, 70.0 - (goodRange[0] - pct) * 5.0);
        }
        else
        {
            return Math.max(0.0, 70.0 - (pct - goodRange[1]) * 5.0);
        }
    }

    /**
     * Sleep duration scoring: range-based with optimal and acceptable bounds.
     *
     * Reference: baseline_scoring.py _score_sleep_duration()
     */
    static double scoreSleepDuration(double hours)
    {
        if (hours >= SLEEP_OPTIMAL_MIN && hours <= SLEEP_OPTIMAL_MAX)
        {
            return 100.0;
        }
        else if (hours >= SLEEP_ACCEPTABLE_MIN && hours < SLEEP_OPTIMAL_MIN)
        {
            return 50.0 + (hours - SLEEP_ACCEPTABLE_MIN) / (SLEEP_OPTIMAL_MIN - SLEEP_ACCEPTABLE_MIN) * 50.0;
        }
        else if (hours > SLEEP_OPTIMAL_MAX && hours <= SLEEP_ACCEPTABLE_MAX)
        {
            return 50.0 + (SLEEP_ACCEPTABLE_MAX - hours) / (SLEEP_ACCEPTABLE_MAX - SLEEP_OPTIMAL_MAX) * 50.0;
        }
        else if (hours < SLEEP_ACCEPTABLE_MIN)
        {
            return Math.max(0.0, hours / SLEEP_ACCEPTABLE_MIN * 50.0);
        }
        else  // hours > SLEEP_ACCEPTABLE_MAX
        {
            return Math.max(0.0, 50.0 - (hours - SLEEP_ACCEPTABLE_MAX) * 10.0);
        }
    }

    // ========== Utility ==========

    static String assignGrade(double score)
    {
        for (int i = 0; i < GRADE_BOUNDS.length; i++)
        {
            if (score >= GRADE_BOUNDS[i][0] && score <= GRADE_BOUNDS[i][1])
            {
                return GRADE_NAMES[i];
            }
        }
        return "poor";
    }

    static String gradeLabelZh(String grade)
    {
        switch (grade)
        {
            case "excellent": return "优秀";
            case "good": return "良好";
            case "fair": return "一般";
            default: return "较差";
        }
    }

    private Double computeComposite(Map<String, DimensionScore> dimensionScores)
    {
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : weights.entrySet())
        {
            DimensionScore ds = dimensionScores.get(entry.getKey());
            if (ds != null)
            {
                weightedSum += ds.totalScore * entry.getValue();
                totalWeight += entry.getValue();
            }
        }

        if (totalWeight == 0) return null;
        return round1(weightedSum / totalWeight);
    }

    private static double average(Iterable<Double> values)
    {
        double sum = 0;
        int count = 0;
        for (Double v : values)
        {
            sum += v;
            count++;
        }
        return (count == 0) ? 0.0 : round1(sum / count);
    }

    private static double round1(double value)
    {
        return Math.round(value * 10.0) / 10.0;
    }
}
