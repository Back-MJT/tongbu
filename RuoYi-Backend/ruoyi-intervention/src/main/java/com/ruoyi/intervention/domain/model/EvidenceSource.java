package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.EvidenceLevel;

/**
 * Published evidence source referenced by algorithm rules.
 * Provides full traceability: "Why was this recommendation generated?"
 */
public class EvidenceSource
{
    /** Unique identifier, e.g. "acsm_2021_ch26" */
    private String sourceId;
    private String title;
    private String authors;
    private Integer year;
    /** guideline | meta_analysis | rct | consensus | textbook */
    private String sourceType;
    private EvidenceLevel level;
    private String url;
    private String description;

    public EvidenceSource() {}

    public EvidenceSource(String sourceId, String title, String authors,
                          Integer year, String sourceType, EvidenceLevel level,
                          String url, String description)
    {
        this.sourceId = sourceId;
        this.title = title;
        this.authors = authors;
        this.year = year;
        this.sourceType = sourceType;
        this.level = level;
        this.url = url;
        this.description = description;
    }

    // --- Getters and Setters ---
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public EvidenceLevel getLevel() { return level; }
    public void setLevel(EvidenceLevel level) { this.level = level; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString()
    {
        return "EvidenceSource{" + sourceId + ": " + title + " (" + year + ") [" + level + "]}";
    }
}
