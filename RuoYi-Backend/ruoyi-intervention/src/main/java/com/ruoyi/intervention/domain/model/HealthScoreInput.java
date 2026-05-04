package com.ruoyi.intervention.domain.model;

/**
 * 健康评分输入数据模型 — 用于综合健康评分计算的所有输入字段
 * Health score input data model — all fields are optional for flexible scoring.
 *
 * Migrated from: intervention-engine/src/algorithms/health_score.py HealthScoreInput
 * Ticket: XIN-83
 */
public class HealthScoreInput
{
    // ========== 心率维度 / Heart Rate Dimension ==========

    /** 静息心率 (bpm) — Resting heart rate */
    private Integer restingHr;

    // ========== 睡眠维度 / Sleep Dimension ==========

    /** 平均每晚睡眠时长 (小时) — Average nightly sleep (hours) */
    private Double sleepDuration;

    /** PSQI全球评分 (0-21) — PSQI global score */
    private Integer psqiGlobal;

    /** 睡眠效率 (0.0-1.0) — Sleep efficiency */
    private Double sleepEfficiency;

    // ========== 运动维度 / Exercise Dimension ==========

    /** 每周运动时长 (分钟) — Weekly exercise duration (minutes) */
    private Double weeklyExerciseMinutes;

    /** 平均每日步数 — Average daily step count */
    private Integer dailySteps;

    // ========== 压力维度 / Stress Dimension ==========

    /** RMSSD心率变异性 (ms) — RMSSD heart rate variability (ms) */
    private Double hrvRmssd;

    /** PSS-10感知压力评分 (0-40) — PSS-10 perceived stress score */
    private Integer perceivedStressScore;

    // ========== Getters and Setters ==========

    public Integer getRestingHr()
    {
        return restingHr;
    }

    public void setRestingHr(Integer restingHr)
    {
        this.restingHr = restingHr;
    }

    public Double getSleepDuration()
    {
        return sleepDuration;
    }

    public void setSleepDuration(Double sleepDuration)
    {
        this.sleepDuration = sleepDuration;
    }

    public Integer getPsqiGlobal()
    {
        return psqiGlobal;
    }

    public void setPsqiGlobal(Integer psqiGlobal)
    {
        this.psqiGlobal = psqiGlobal;
    }

    public Double getSleepEfficiency()
    {
        return sleepEfficiency;
    }

    public void setSleepEfficiency(Double sleepEfficiency)
    {
        this.sleepEfficiency = sleepEfficiency;
    }

    public Double getWeeklyExerciseMinutes()
    {
        return weeklyExerciseMinutes;
    }

    public void setWeeklyExerciseMinutes(Double weeklyExerciseMinutes)
    {
        this.weeklyExerciseMinutes = weeklyExerciseMinutes;
    }

    public Integer getDailySteps()
    {
        return dailySteps;
    }

    public void setDailySteps(Integer dailySteps)
    {
        this.dailySteps = dailySteps;
    }

    public Double getHrvRmssd()
    {
        return hrvRmssd;
    }

    public void setHrvRmssd(Double hrvRmssd)
    {
        this.hrvRmssd = hrvRmssd;
    }

    public Integer getPerceivedStressScore()
    {
        return perceivedStressScore;
    }

    public void setPerceivedStressScore(Integer perceivedStressScore)
    {
        this.perceivedStressScore = perceivedStressScore;
    }
}
