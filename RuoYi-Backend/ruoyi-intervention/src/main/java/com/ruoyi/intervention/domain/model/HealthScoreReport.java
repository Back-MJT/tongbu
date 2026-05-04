package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合健康评分报告 — 包含各维度评分、综合评分和循证参考
 * Composite health score report with per-dimension results, composite score,
 * grades, evidence references, and missing dimension info.
 *
 * Migrated from: intervention-engine/src/algorithms/health_score.py HealthScoreReport
 * Ticket: XIN-83
 */
public class HealthScoreReport
{
    /** 用户ID / User identifier */
    private String userId;

    /** 计算时间 / Calculation timestamp */
    private LocalDateTime calculatedAt;

    /** 综合评分 (0-100) / Composite score */
    private Double compositeScore;

    /** 综合等级 (excellent/good/fair/poor) / Composite grade */
    private String compositeGrade;

    /** 综合等级中文标签 / Composite grade label in Chinese */
    private String compositeGradeLabelZh;

    /** 各维度评分结果 / Per-dimension score results */
    private Map<String, DimensionResult> dimensionResults = new LinkedHashMap<>();

    /** 维度权重配置 / Dimension weights used in calculation */
    private Map<String, Double> dimensionWeights = new LinkedHashMap<>();

    /** 配置版本 / Configuration version */
    private String configVersion = "1.0";

    /** 循证参考列表 / Evidence references used */
    private List<String> configEvidenceRefs = new ArrayList<>();

    /** 缺失维度列表 / Dimensions with no input data */
    private List<String> missingDimensions = new ArrayList<>();

    // ========== Getters and Setters ==========

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public LocalDateTime getCalculatedAt()
    {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt)
    {
        this.calculatedAt = calculatedAt;
    }

    public Double getCompositeScore()
    {
        return compositeScore;
    }

    public void setCompositeScore(Double compositeScore)
    {
        this.compositeScore = compositeScore;
    }

    public String getCompositeGrade()
    {
        return compositeGrade;
    }

    public void setCompositeGrade(String compositeGrade)
    {
        this.compositeGrade = compositeGrade;
    }

    public String getCompositeGradeLabelZh()
    {
        return compositeGradeLabelZh;
    }

    public void setCompositeGradeLabelZh(String compositeGradeLabelZh)
    {
        this.compositeGradeLabelZh = compositeGradeLabelZh;
    }

    public Map<String, DimensionResult> getDimensionResults()
    {
        return dimensionResults;
    }

    public void setDimensionResults(Map<String, DimensionResult> dimensionResults)
    {
        this.dimensionResults = dimensionResults != null ? dimensionResults : new LinkedHashMap<>();
    }

    public Map<String, Double> getDimensionWeights()
    {
        return dimensionWeights;
    }

    public void setDimensionWeights(Map<String, Double> dimensionWeights)
    {
        this.dimensionWeights = dimensionWeights != null ? dimensionWeights : new LinkedHashMap<>();
    }

    public String getConfigVersion()
    {
        return configVersion;
    }

    public void setConfigVersion(String configVersion)
    {
        this.configVersion = configVersion;
    }

    public List<String> getConfigEvidenceRefs()
    {
        return configEvidenceRefs;
    }

    public void setConfigEvidenceRefs(List<String> configEvidenceRefs)
    {
        this.configEvidenceRefs = configEvidenceRefs != null ? configEvidenceRefs : new ArrayList<>();
    }

    public List<String> getMissingDimensions()
    {
        return missingDimensions;
    }

    public void setMissingDimensions(List<String> missingDimensions)
    {
        this.missingDimensions = missingDimensions != null ? missingDimensions : new ArrayList<>();
    }
}
