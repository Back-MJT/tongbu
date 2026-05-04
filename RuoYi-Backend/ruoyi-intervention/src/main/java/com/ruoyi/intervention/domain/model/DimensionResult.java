package com.ruoyi.intervention.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单维度评分结果 — 用于表示某个健康维度的评分详情
 * Score result for one health dimension.
 *
 * Migrated from: intervention-engine/src/algorithms/health_score.py DimensionResult
 * Ticket: XIN-83
 */
public class DimensionResult
{
    /** 维度名称 / Dimension name */
    private String dimension;

    /** 维度评分 (0-100) / Dimension score */
    private Double score;

    /** 评分等级 (excellent/good/fair/poor) / Grade label */
    private String grade;

    /** 评分等级中文标签 / Grade label in Chinese */
    private String gradeLabelZh;

    /** 循证参考标识 / Evidence reference identifier */
    private String evidenceRef;

    /** 子指标评分映射 / Sub-indicator scores */
    private Map<String, Double> subIndicators = new LinkedHashMap<>();

    // ========== Getters and Setters ==========

    public String getDimension()
    {
        return dimension;
    }

    public void setDimension(String dimension)
    {
        this.dimension = dimension;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public String getGradeLabelZh()
    {
        return gradeLabelZh;
    }

    public void setGradeLabelZh(String gradeLabelZh)
    {
        this.gradeLabelZh = gradeLabelZh;
    }

    public String getEvidenceRef()
    {
        return evidenceRef;
    }

    public void setEvidenceRef(String evidenceRef)
    {
        this.evidenceRef = evidenceRef;
    }

    public Map<String, Double> getSubIndicators()
    {
        return subIndicators;
    }

    public void setSubIndicators(Map<String, Double> subIndicators)
    {
        this.subIndicators = subIndicators != null ? subIndicators : new LinkedHashMap<>();
    }
}
