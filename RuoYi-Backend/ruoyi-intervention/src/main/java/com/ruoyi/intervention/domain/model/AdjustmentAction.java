package com.ruoyi.intervention.domain.model;

import com.ruoyi.intervention.domain.enums.AdjustmentDimension;
import com.ruoyi.intervention.domain.enums.AdjustmentDirection;

import java.time.LocalDateTime;

/**
 * A concrete action to adjust a single parameter of an intervention plan.
 *
 * Reference: ACSM 2021 Ch.4; Nahum-Shani 2018 JITAI design
 */
public class AdjustmentAction {
    private String actionId;
    private String userId;
    private AdjustmentDimension dimension;
    private AdjustmentDirection direction;
    private String parameterName;
    private Object oldValue;
    private Object newValue;
    private double changeFraction;
    private double magnitude;
    private boolean safetyBoundsRespected;
    private String triggeredBy;
    private String rationale;
    private String evidenceRef;
    private LocalDateTime timestamp;

    public AdjustmentAction() {
        this.timestamp = LocalDateTime.now();
    }

    public AdjustmentAction(String actionId, String userId, AdjustmentDimension dimension,
            AdjustmentDirection direction, String parameterName,
            Object oldValue, Object newValue, double changeFraction,
            double magnitude, boolean safetyBoundsRespected,
            String triggeredBy, String rationale, String evidenceRef) {
        this();
        this.actionId = actionId;
        this.userId = userId;
        this.dimension = dimension;
        this.direction = direction;
        this.parameterName = parameterName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeFraction = changeFraction;
        this.magnitude = magnitude;
        this.safetyBoundsRespected = safetyBoundsRespected;
        this.triggeredBy = triggeredBy;
        this.rationale = rationale;
        this.evidenceRef = evidenceRef;
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public AdjustmentDimension getDimension() { return dimension; }
    public void setDimension(AdjustmentDimension dimension) { this.dimension = dimension; }
    public AdjustmentDirection getDirection() { return direction; }
    public void setDirection(AdjustmentDirection direction) { this.direction = direction; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
    public double getChangeFraction() { return changeFraction; }
    public void setChangeFraction(double changeFraction) { this.changeFraction = changeFraction; }
    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }
    public boolean isSafetyBoundsRespected() { return safetyBoundsRespected; }
    public void setSafetyBoundsRespected(boolean safetyBoundsRespected) { this.safetyBoundsRespected = safetyBoundsRespected; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
