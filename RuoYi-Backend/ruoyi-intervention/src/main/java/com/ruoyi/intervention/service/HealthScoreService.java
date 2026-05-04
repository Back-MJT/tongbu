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

import com.ruoyi.intervention.domain.model.DimensionResult;
import com.ruoyi.intervention.domain.model.HealthScoreInput;
import com.ruoyi.intervention.domain.model.HealthScoreReport;

/**
 * 综合健康评分服务 — 基于四维度(心率、睡眠、运动、压力)的加权综合健康评分
 * Composite health score service — weighted scoring from four health dimensions.
 *
 * <p>Lightweight composite health score (0-100) from four dimensions:
 * <ol>
 *   <li>心率 (Heart rate) — resting HR as cardiovascular proxy</li>
 *   <li>睡眠 (Sleep) — duration, quality, efficiency</li>
 *   <li>运动 (Exercise) — weekly exercise minutes, step count</li>
 *   <li>压力 (Stress) — HRV-based stress proxy</li>
 * </ol>
 *
 * <p>Evidence references / 循证参考:
 * <ul>
 *   <li>acsm_2021_ch4 — Cardiovascular fitness &amp; resting HR thresholds / 心肺健康与静息心率阈值</li>
 *   <li>hirshkowitz_2015 — Sleep duration norms (7-9h adults) / 睡眠时长标准</li>
 *   <li>buysse_1989_psqi — PSQI scoring thresholds / PSQI评分阈值</li>
 *   <li>who_2020_pa — Physical activity guidelines (150-300 min/week) / 体力活动指南</li>
 *   <li>kim_2018_hrv_stress — HRV as stress biomarker / HRV压力生物标志物</li>
 *   <li>laborde_2017_hrv_biofeedback — HRV biofeedback norms / HRV生物反馈标准</li>
 * </ul>
 *
 * <p>Scoring grades / 评分等级:
 * <ul>
 *   <li>Excellent (优秀): &ge; 85</li>
 *   <li>Good (良好): 70-84</li>
 *   <li>Fair (一般): 50-69</li>
 *   <li>Needs Attention (需关注): &lt; 50</li>
 * </ul>
 *
 * Migrated from: intervention-engine/src/algorithms/health_score.py
 * Ticket: XIN-83
 */
@Service
public class HealthScoreService
{
    private static final Logger log = LoggerFactory.getLogger(HealthScoreService.class);

    // ========== 评分等级阈值 / Grade Thresholds ==========
    // Grade definitions — identical to Python GRADE_THRESHOLDS
    private static final double GRADE_EXCELLENT = 85.0;
    private static final double GRADE_GOOD = 70.0;
    private static final double GRADE_FAIR = 50.0;

    /** 评分等级中文标签 / Grade labels in Chinese */
    private static final Map<String, String> GRADE_LABELS_ZH = new LinkedHashMap<>();
    static
    {
        GRADE_LABELS_ZH.put("excellent", "优秀");
        GRADE_LABELS_ZH.put("good", "良好");
        GRADE_LABELS_ZH.put("fair", "一般");
        GRADE_LABELS_ZH.put("poor", "需关注");
    }

    // ========== 静息心率阈值 / Resting HR Thresholds ==========
    // Ref: acsm_2021_ch4 — athlete <60, good <70, average <80, poor >=80
    // 参考: acsm_2021_ch4 — 运动员<60, 良好<70, 一般<80, 较差>=80
    private static final double HR_EXCELLENT = 60.0;
    private static final double HR_GOOD = 70.0;
    private static final double HR_FAIR = 80.0;

    // ========== 睡眠时长阈值 / Sleep Duration Thresholds ==========
    // Ref: hirshkowitz_2015 — 7-9h optimal, 6-10h acceptable
    // 参考: hirshkowitz_2015 — 7-9小时最佳, 6-10小时可接受
    private static final double SLEEP_DUR_OPTIMAL_MIN = 7.0;
    private static final double SLEEP_DUR_OPTIMAL_MAX = 9.0;
    private static final double SLEEP_DUR_ACCEPTABLE_MIN = 6.0;
    private static final double SLEEP_DUR_ACCEPTABLE_MAX = 10.0;

    // ========== PSQI阈值 / PSQI Thresholds ==========
    // Ref: buysse_1989_psqi — <=5 good, 6-7 fair, >=8 poor
    // 参考: buysse_1989_psqi — <=5良好, 6-7一般, >=8较差
    private static final double PSQI_EXCELLENT = 3.0;
    private static final double PSQI_GOOD = 5.0;
    private static final double PSQI_FAIR = 8.0;

    // ========== 睡眠效率阈值 / Sleep Efficiency Thresholds ==========
    // Ref: buysse_1989_psqi — >=90% excellent, >=85% good, >=75% fair
    // 参考: buysse_1989_psqi — >=90%优秀, >=85%良好, >=75%一般
    private static final double SLEEP_EFF_EXCELLENT = 0.90;
    private static final double SLEEP_EFF_GOOD = 0.85;
    private static final double SLEEP_EFF_FAIR = 0.75;

    // ========== 运动时长阈值 / Exercise Minutes Thresholds ==========
    // Ref: who_2020_pa — 150-300 min/week moderate or 75-150 min/week vigorous
    // 参考: who_2020_pa — 每周150-300分钟中等强度或75-150分钟高强度
    private static final double EXERCISE_EXCELLENT_MIN = 300.0;
    private static final double EXERCISE_GOOD_MIN = 150.0;
    private static final double EXERCISE_FAIR_MIN = 60.0;

    // ========== 步数阈值 / Step Count Thresholds ==========
    // Ref: who_2020_pa — 10000 excellent, 7500 good, 5000 fair
    // 参考: who_2020_pa — 10000优秀, 7500良好, 5000一般
    private static final double STEP_EXCELLENT = 10000.0;
    private static final double STEP_GOOD = 7500.0;
    private static final double STEP_FAIR = 5000.0;

    // ========== HRV (RMSSD) 阈值 / HRV Thresholds ==========
    // Ref: kim_2018_hrv_stress — healthy adult RMSSD 40-100ms
    // Ref: laborde_2017_hrv_biofeedback — <20ms high stress, 20-40 moderate, >40 healthy
    private static final double HRV_EXCELLENT = 60.0;
    private static final double HRV_GOOD = 40.0;
    private static final double HRV_FAIR = 20.0;

    // ========== PSS-10 阈值 / PSS-10 Thresholds ==========
    // Ref: kim_2018_hrv_stress — 0-13 low, 14-26 moderate, 27-40 high
    // 参考: kim_2018_hrv_stress — 0-13低, 14-26中, 27-40高
    private static final double PSS_EXCELLENT = 13.0;
    private static final double PSS_GOOD = 20.0;
    private static final double PSS_FAIR = 27.0;

    // ========== 默认维度权重 / Default Dimension Weights ==========
    private static final Map<String, Double> DEFAULT_WEIGHTS = new LinkedHashMap<>();
    static
    {
        DEFAULT_WEIGHTS.put("heart_rate", 0.30);
        DEFAULT_WEIGHTS.put("sleep", 0.25);
        DEFAULT_WEIGHTS.put("exercise", 0.25);
        DEFAULT_WEIGHTS.put("stress", 0.20);
    }

    /** 所有循证参考 / All evidence references used by this module */
    private static final List<String> ALL_EVIDENCE_REFS = Arrays.asList(
        "acsm_2021_ch4",
        "hirshkowitz_2015",
        "buysse_1989_psqi",
        "who_2020_pa",
        "kim_2018_hrv_stress",
        "laborde_2017_hrv_biofeedback"
    );

    /** 配置版本 / Configuration version */
    private static final String CONFIG_VERSION = "1.0";

    // ========== Instance State ==========

    /** 归一化后的维度权重 / Normalized dimension weights */
    private final Map<String, Double> weights;

    /**
     * Package-private getter for testing.
     */
    Map<String, Double> getWeightsForTesting()
    {
        return weights;
    }

    // ========== Constructors ==========

    /**
     * 使用默认权重构造 / Construct with default weights.
     * Default: heart_rate 0.30, sleep 0.25, exercise 0.25, stress 0.20.
     */
    public HealthScoreService()
    {
        this(null);
    }

    /**
     * 使用自定义权重构造 / Construct with optional custom dimension weights.
     * Weights are normalized to sum to 1.0.
     *
     * @param customWeights custom weights per dimension, or null for defaults
     */
    public HealthScoreService(Map<String, Double> customWeights)
    {
        Map<String, Double> raw = (customWeights != null) ? customWeights : DEFAULT_WEIGHTS;
        double total = raw.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 1.0) < 1e-9)
        {
            this.weights = new LinkedHashMap<>(raw);
        }
        else
        {
            this.weights = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : raw.entrySet())
            {
                this.weights.put(entry.getKey(), entry.getValue() / total);
            }
        }
        log.debug("健康评分服务初始化，权重: {} / HealthScoreService initialized with weights: {}", this.weights);
    }

    // ========== Public API ==========

    /**
     * 计算综合健康评分 / Calculate composite health score from input data.
     *
     * @param userId 用户ID / user identifier
     * @param data   健康评分输入数据 / health score input data (all fields optional)
     * @return 健康评分报告 / HealthScoreReport with per-dimension scores and composite
     */
    public HealthScoreReport calculate(String userId, HealthScoreInput data)
    {
        log.info("计算用户{}的综合健康评分 / Calculating health score for user: {}", userId, userId);

        Map<String, DimensionResult> dimensionResults = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        // 心率维度 / Heart rate dimension
        DimensionResult hrResult = scoreHeartRate(data);
        if (hrResult != null)
        {
            dimensionResults.put("heart_rate", hrResult);
        }
        else
        {
            missing.add("heart_rate");
        }

        // 睡眠维度 / Sleep dimension
        DimensionResult sleepResult = scoreSleep(data);
        if (sleepResult != null)
        {
            dimensionResults.put("sleep", sleepResult);
        }
        else
        {
            missing.add("sleep");
        }

        // 运动维度 / Exercise dimension
        DimensionResult exerciseResult = scoreExercise(data);
        if (exerciseResult != null)
        {
            dimensionResults.put("exercise", exerciseResult);
        }
        else
        {
            missing.add("exercise");
        }

        // 压力维度 / Stress dimension
        DimensionResult stressResult = scoreStress(data);
        if (stressResult != null)
        {
            dimensionResults.put("stress", stressResult);
        }
        else
        {
            missing.add("stress");
        }

        // 计算加权综合分 / Compute weighted composite
        double composite = computeComposite(dimensionResults);
        String grade = assignGrade(composite);

        // 构建权重映射（保留2位小数） / Build weights map (rounded to 2 decimal places)
        Map<String, Double> roundedWeights = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : this.weights.entrySet())
        {
            roundedWeights.put(entry.getKey(), Math.round(entry.getValue() * 100.0) / 100.0);
        }

        HealthScoreReport report = new HealthScoreReport();
        report.setUserId(userId);
        report.setCalculatedAt(LocalDateTime.now());
        report.setCompositeScore(Math.round(composite * 10.0) / 10.0);
        report.setCompositeGrade(grade);
        report.setCompositeGradeLabelZh(GRADE_LABELS_ZH.get(grade));
        report.setDimensionResults(dimensionResults);
        report.setDimensionWeights(roundedWeights);
        report.setConfigVersion(CONFIG_VERSION);
        report.setConfigEvidenceRefs(new ArrayList<>(ALL_EVIDENCE_REFS));
        report.setMissingDimensions(missing);

        log.info("用户{}综合评分: {} ({}), 缺失维度: {} / User {} composite: {} ({}), missing: {}",
            userId, report.getCompositeScore(), grade, missing,
            userId, report.getCompositeScore(), grade, missing);

        return report;
    }

    // ========== 评分等级分配 / Grade Assignment ==========

    /**
     * 根据数值评分分配等级 / Assign grade label from numeric score.
     *
     * @param score 数值评分 (0-100) / numeric score
     * @return 等级字符串: 'excellent', 'good', 'fair', or 'poor'
     */
    static String assignGrade(double score)
    {
        if (score >= GRADE_EXCELLENT)
        {
            return "excellent";
        }
        else if (score >= GRADE_GOOD)
        {
            return "good";
        }
        else if (score >= GRADE_FAIR)
        {
            return "fair";
        }
        else
        {
            return "poor";
        }
    }

    // ========== 辅助评分方法 / Helper Scoring Methods ==========

    /**
     * 越低越好的指标评分 / Score a metric where lower values are better.
     * 使用阈值间线性插值 / Uses linear interpolation between thresholds.
     * 映射: excellent→100, good→75, fair→50, poor→25.
     *
     * @param value           指标值 / metric value
     * @param excellentThresh 优秀阈值 / excellent threshold
     * @param goodThresh      良好阈值 / good threshold
     * @param fairThresh      一般阈值 / fair threshold
     * @return 评分 (0-100) / score
     */
    static double scoreLowerIsBetter(double value, double excellentThresh,
                                     double goodThresh, double fairThresh)
    {
        if (value <= excellentThresh)
        {
            return 100.0;
        }
        else if (value <= goodThresh)
        {
            // 插值 100 -> 75 / Interpolate 100 -> 75
            double ratio = (goodThresh != excellentThresh)
                ? (value - excellentThresh) / (goodThresh - excellentThresh) : 0;
            return round1(100.0 - 25.0 * ratio);
        }
        else if (value <= fairThresh)
        {
            // 插值 75 -> 50 / Interpolate 75 -> 50
            double ratio = (fairThresh != goodThresh)
                ? (value - goodThresh) / (fairThresh - goodThresh) : 0;
            return round1(75.0 - 25.0 * ratio);
        }
        else
        {
            // 低于一般 — 插值 50 -> 0, 钳制 / Below fair — interpolate 50 -> 0, clamped
            return Math.max(0.0, round1(50.0 - (value - fairThresh) * 0.5));
        }
    }

    /**
     * 越高越好的指标评分 / Score a metric where higher values are better.
     * 映射: excellent→100, good→75, fair→50, below→25.
     *
     * @param value           指标值 / metric value
     * @param excellentThresh 优秀阈值 / excellent threshold
     * @param goodThresh      良好阈值 / good threshold
     * @param fairThresh      一般阈值 / fair threshold
     * @return 评分 (0-100) / score
     */
    static double scoreHigherIsBetter(double value, double excellentThresh,
                                      double goodThresh, double fairThresh)
    {
        if (value >= excellentThresh)
        {
            return 100.0;
        }
        else if (value >= goodThresh)
        {
            double ratio = (excellentThresh != goodThresh)
                ? (value - goodThresh) / (excellentThresh - goodThresh) : 0;
            return round1(75.0 + 25.0 * ratio);
        }
        else if (value >= fairThresh)
        {
            double ratio = (goodThresh != fairThresh)
                ? (value - fairThresh) / (goodThresh - fairThresh) : 0;
            return round1(50.0 + 25.0 * ratio);
        }
        else
        {
            // 低于一般 / Below fair
            return (fairThresh > 0) ? Math.max(0.0, round1(value / fairThresh * 50.0)) : 0.0;
        }
    }

    /**
     * 睡眠时长评分 / Score sleep duration based on optimal range.
     * 7-9h→100, 6-7h or 9-10h→70, &lt;6h or &gt;10h→按比例缩放.
     *
     * <p>Ref: hirshkowitz_2015
     * @param hours 睡眠时长(小时) / sleep duration in hours
     * @return 评分 (0-100)
     */
    static double scoreSleepDuration(double hours)
    {
        if (SLEEP_DUR_OPTIMAL_MIN <= hours && hours <= SLEEP_DUR_OPTIMAL_MAX)
        {
            return 100.0;
        }
        else if (SLEEP_DUR_ACCEPTABLE_MIN <= hours && hours < SLEEP_DUR_OPTIMAL_MIN)
        {
            double ratio = (hours - SLEEP_DUR_ACCEPTABLE_MIN) / (SLEEP_DUR_OPTIMAL_MIN - SLEEP_DUR_ACCEPTABLE_MIN);
            return round1(70.0 + 30.0 * ratio);
        }
        else if (SLEEP_DUR_OPTIMAL_MAX < hours && hours <= SLEEP_DUR_ACCEPTABLE_MAX)
        {
            double ratio = (SLEEP_DUR_ACCEPTABLE_MAX - hours) / (SLEEP_DUR_ACCEPTABLE_MAX - SLEEP_DUR_OPTIMAL_MAX);
            return round1(70.0 + 30.0 * ratio);
        }
        else if (hours < SLEEP_DUR_ACCEPTABLE_MIN)
        {
            return Math.max(0.0, round1(hours / SLEEP_DUR_ACCEPTABLE_MIN * 70.0));
        }
        else
        {
            // 高于可接受上限 / Above acceptable max
            double excess = hours - SLEEP_DUR_ACCEPTABLE_MAX;
            return Math.max(0.0, round1(70.0 - excess * 15.0));
        }
    }

    /**
     * 睡眠效率评分 / Score sleep efficiency (0.0-1.0).
     * &ge;90%→100, &ge;85%→80, &ge;75%→60, &lt;75%→按比例缩放.
     *
     * <p>Ref: buysse_1989_psqi
     * @param efficiency 睡眠效率 (0.0-1.0) / sleep efficiency
     * @return 评分 (0-100)
     */
    static double scoreSleepEfficiency(double efficiency)
    {
        if (efficiency >= SLEEP_EFF_EXCELLENT)
        {
            return 100.0;
        }
        else if (efficiency >= SLEEP_EFF_GOOD)
        {
            double ratio = (SLEEP_EFF_EXCELLENT != SLEEP_EFF_GOOD)
                ? (efficiency - SLEEP_EFF_GOOD) / (SLEEP_EFF_EXCELLENT - SLEEP_EFF_GOOD) : 0;
            return round1(80.0 + 20.0 * ratio);
        }
        else if (efficiency >= SLEEP_EFF_FAIR)
        {
            double ratio = (SLEEP_EFF_GOOD != SLEEP_EFF_FAIR)
                ? (efficiency - SLEEP_EFF_FAIR) / (SLEEP_EFF_GOOD - SLEEP_EFF_FAIR) : 0;
            return round1(60.0 + 20.0 * ratio);
        }
        else
        {
            return Math.max(0.0, round1(efficiency / SLEEP_EFF_FAIR * 60.0));
        }
    }

    /**
     * 每周运动时长评分 / Score weekly exercise duration.
     * &ge;300→100, &ge;150→75, &ge;60→50, &lt;60→按比例缩放.
     *
     * <p>Ref: who_2020_pa
     * @param weeklyMin 每周运动分钟数 / weekly exercise minutes
     * @return 评分 (0-100)
     */
    static double scoreExerciseMinutes(double weeklyMin)
    {
        return scoreHigherIsBetter(weeklyMin, EXERCISE_EXCELLENT_MIN, EXERCISE_GOOD_MIN, EXERCISE_FAIR_MIN);
    }

    /**
     * 每日步数评分 / Score daily step count.
     * &ge;10000→100, &ge;7500→75, &ge;5000→50, &lt;5000→按比例缩放.
     *
     * <p>Ref: who_2020_pa
     * @param steps 每日步数 / daily step count
     * @return 评分 (0-100)
     */
    static double scoreSteps(int steps)
    {
        return scoreHigherIsBetter((double) steps, STEP_EXCELLENT, STEP_GOOD, STEP_FAIR);
    }

    // ========== 维度评分器 / Dimension Scorers ==========

    /**
     * 心率维度评分 / Score heart rate dimension from resting HR.
     *
     * <p>Ref: acsm_2021_ch4
     */
    private DimensionResult scoreHeartRate(HealthScoreInput data)
    {
        if (data.getRestingHr() == null)
        {
            return null;
        }

        double score = scoreLowerIsBetter(
            data.getRestingHr().doubleValue(), HR_EXCELLENT, HR_GOOD, HR_FAIR
        );
        String grade = assignGrade(score);

        DimensionResult result = new DimensionResult();
        result.setDimension("heart_rate");
        result.setScore(score);
        result.setGrade(grade);
        result.setGradeLabelZh(GRADE_LABELS_ZH.get(grade));
        result.setEvidenceRef("acsm_2021_ch4");
        Map<String, Double> subIndicators = new LinkedHashMap<>();
        subIndicators.put("resting_hr", score);
        result.setSubIndicators(subIndicators);
        return result;
    }

    /**
     * 睡眠维度评分 / Score sleep dimension from duration, PSQI, and efficiency.
     *
     * <p>Refs: hirshkowitz_2015, buysse_1989_psqi
     */
    private DimensionResult scoreSleep(HealthScoreInput data)
    {
        Map<String, Double> subScores = new LinkedHashMap<>();

        if (data.getSleepDuration() != null)
        {
            subScores.put("sleep_duration", scoreSleepDuration(data.getSleepDuration()));
        }

        if (data.getPsqiGlobal() != null)
        {
            subScores.put("psqi_global", scoreLowerIsBetter(
                data.getPsqiGlobal().doubleValue(), PSQI_EXCELLENT, PSQI_GOOD, PSQI_FAIR
            ));
        }

        if (data.getSleepEfficiency() != null)
        {
            subScores.put("sleep_efficiency", scoreSleepEfficiency(data.getSleepEfficiency()));
        }

        if (subScores.isEmpty())
        {
            return null;
        }

        double total = subScores.values().stream().mapToDouble(Double::doubleValue).sum() / subScores.size();
        total = round1(total);
        String grade = assignGrade(total);

        // 子指标评分保留1位小数 / Round sub-indicator scores to 1 decimal
        Map<String, Double> roundedSubScores = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : subScores.entrySet())
        {
            roundedSubScores.put(entry.getKey(), round1(entry.getValue()));
        }

        DimensionResult result = new DimensionResult();
        result.setDimension("sleep");
        result.setScore(total);
        result.setGrade(grade);
        result.setGradeLabelZh(GRADE_LABELS_ZH.get(grade));
        result.setEvidenceRef("hirshkowitz_2015");
        result.setSubIndicators(roundedSubScores);
        return result;
    }

    /**
     * 运动维度评分 / Score exercise dimension from weekly minutes and daily steps.
     *
     * <p>Ref: who_2020_pa
     */
    private DimensionResult scoreExercise(HealthScoreInput data)
    {
        Map<String, Double> subScores = new LinkedHashMap<>();

        if (data.getWeeklyExerciseMinutes() != null)
        {
            subScores.put("weekly_exercise_minutes", scoreExerciseMinutes(data.getWeeklyExerciseMinutes()));
        }

        if (data.getDailySteps() != null)
        {
            subScores.put("daily_steps", scoreSteps(data.getDailySteps()));
        }

        if (subScores.isEmpty())
        {
            return null;
        }

        double total = subScores.values().stream().mapToDouble(Double::doubleValue).sum() / subScores.size();
        total = round1(total);
        String grade = assignGrade(total);

        Map<String, Double> roundedSubScores = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : subScores.entrySet())
        {
            roundedSubScores.put(entry.getKey(), round1(entry.getValue()));
        }

        DimensionResult result = new DimensionResult();
        result.setDimension("exercise");
        result.setScore(total);
        result.setGrade(grade);
        result.setGradeLabelZh(GRADE_LABELS_ZH.get(grade));
        result.setEvidenceRef("who_2020_pa");
        result.setSubIndicators(roundedSubScores);
        return result;
    }

    /**
     * 压力维度评分 / Score stress dimension from HRV (RMSSD) and perceived stress.
     *
     * <p>Refs: kim_2018_hrv_stress, laborde_2017_hrv_biofeedback
     */
    private DimensionResult scoreStress(HealthScoreInput data)
    {
        Map<String, Double> subScores = new LinkedHashMap<>();

        if (data.getHrvRmssd() != null)
        {
            subScores.put("hrv_rmssd", scoreHigherIsBetter(
                data.getHrvRmssd(), HRV_EXCELLENT, HRV_GOOD, HRV_FAIR
            ));
        }

        if (data.getPerceivedStressScore() != null)
        {
            subScores.put("perceived_stress_score", scoreLowerIsBetter(
                data.getPerceivedStressScore().doubleValue(), PSS_EXCELLENT, PSS_GOOD, PSS_FAIR
            ));
        }

        if (subScores.isEmpty())
        {
            return null;
        }

        double total = subScores.values().stream().mapToDouble(Double::doubleValue).sum() / subScores.size();
        total = round1(total);
        String grade = assignGrade(total);

        Map<String, Double> roundedSubScores = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : subScores.entrySet())
        {
            roundedSubScores.put(entry.getKey(), round1(entry.getValue()));
        }

        DimensionResult result = new DimensionResult();
        result.setDimension("stress");
        result.setScore(total);
        result.setGrade(grade);
        result.setGradeLabelZh(GRADE_LABELS_ZH.get(grade));
        result.setEvidenceRef("kim_2018_hrv_stress");
        result.setSubIndicators(roundedSubScores);
        return result;
    }

    // ========== 综合评分计算 / Composite Calculation ==========

    /**
     * 计算加权综合评分 / Compute weighted composite score from dimension results.
     * 使用配置的维度权重，无数据的维度被排除，剩余权重重新归一化。
     *
     * <p>Uses configured dimension weights. Dimensions with no data are excluded;
     * remaining weights are renormalized.
     */
    private double computeComposite(Map<String, DimensionResult> results)
    {
        if (results.isEmpty())
        {
            return 50.0; // 无数据时的中性默认值 / Neutral default when no data
        }

        // 收集有数据的维度的权重 / Gather weights for available dimensions
        Map<String, Double> relevantWeights = new LinkedHashMap<>();
        for (String dim : results.keySet())
        {
            if (this.weights.containsKey(dim))
            {
                relevantWeights.put(dim, this.weights.get(dim));
            }
        }

        double totalWeight = relevantWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0)
        {
            return 50.0;
        }

        // 重新归一化缺失维度的权重 / Renormalize for missing dimensions
        Map<String, Double> normWeights = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : relevantWeights.entrySet())
        {
            normWeights.put(entry.getKey(), entry.getValue() / totalWeight);
        }

        double composite = 0.0;
        for (String dim : results.keySet())
        {
            if (normWeights.containsKey(dim))
            {
                composite += results.get(dim).getScore() * normWeights.get(dim);
            }
        }

        return round1(composite);
    }

    // ========== 四舍五入工具 / Rounding Utility ==========

    /**
     * 保留1位小数四舍五入 / Round to 1 decimal place.
     * Matches Python round(x, 1) behavior exactly.
     */
    private static double round1(double value)
    {
        return Math.round(value * 10.0) / 10.0;
    }
}
