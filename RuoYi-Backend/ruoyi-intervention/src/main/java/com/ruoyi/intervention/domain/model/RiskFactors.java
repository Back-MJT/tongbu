package com.ruoyi.intervention.domain.model;

/**
 * 风险因子 — ACSM风险分层
 * Risk factors aligned with ACSM 2021 Ch.2 risk stratification.
 *
 * Categories:
 *   Low: no risk factors, asymptomatic
 *   Moderate: >=1 cardiovascular risk factor
 *   High: signs/symptoms of cardiovascular disease
 */
public class RiskFactors
{
    private boolean sedentary = false;
    private boolean highStress = false;
    private boolean poorSleep = false;
    private boolean overweight = false;
    private boolean hypertension = false;
    private boolean diabetesRisk = false;
    private boolean cardiovascularRisk = false;  // Signs/symptoms per ACSM Ch.2
    private boolean smoking = false;
    private boolean familyHistoryCvd = false;     // First-degree relative <55yr male / <65yr female
    private boolean dyslipidemia = false;          // LDL > 130 mg/dL or on lipid-lowering medication

    public RiskFactors() {}

    // --- Getters & Setters ---

    public boolean isSedentary() { return sedentary; }
    public void setSedentary(boolean sedentary) { this.sedentary = sedentary; }

    public boolean isHighStress() { return highStress; }
    public void setHighStress(boolean highStress) { this.highStress = highStress; }

    public boolean isPoorSleep() { return poorSleep; }
    public void setPoorSleep(boolean poorSleep) { this.poorSleep = poorSleep; }

    public boolean isOverweight() { return overweight; }
    public void setOverweight(boolean overweight) { this.overweight = overweight; }

    public boolean isHypertension() { return hypertension; }
    public void setHypertension(boolean hypertension) { this.hypertension = hypertension; }

    public boolean isDiabetesRisk() { return diabetesRisk; }
    public void setDiabetesRisk(boolean diabetesRisk) { this.diabetesRisk = diabetesRisk; }

    public boolean isCardiovascularRisk() { return cardiovascularRisk; }
    public void setCardiovascularRisk(boolean cardiovascularRisk) { this.cardiovascularRisk = cardiovascularRisk; }

    public boolean isSmoking() { return smoking; }
    public void setSmoking(boolean smoking) { this.smoking = smoking; }

    public boolean isFamilyHistoryCvd() { return familyHistoryCvd; }
    public void setFamilyHistoryCvd(boolean familyHistoryCvd) { this.familyHistoryCvd = familyHistoryCvd; }

    public boolean isDyslipidemia() { return dyslipidemia; }
    public void setDyslipidemia(boolean dyslipidemia) { this.dyslipidemia = dyslipidemia; }

    /**
     * Count positive risk factors for ACSM risk stratification.
     */
    public int countRiskFactors()
    {
        int count = 0;
        if (sedentary) count++;
        if (overweight) count++;
        if (hypertension) count++;
        if (diabetesRisk) count++;
        if (cardiovascularRisk) count++;
        if (smoking) count++;
        if (familyHistoryCvd) count++;
        if (dyslipidemia) count++;
        if (highStress) count++;
        return count;
    }

    /**
     * Classify into ACSM risk category: low/moderate/high.
     * Reference: acsm_2021_ch2
     */
    public String acsmRiskCategory()
    {
        if (cardiovascularRisk || hypertension || diabetesRisk)
        {
            return "high";
        }
        if (countRiskFactors() >= 2)
        {
            return "moderate";
        }
        return "low";
    }
}
