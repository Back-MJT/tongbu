package com.ruoyi.intervention.service.aline;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Minimal B Line payload reserved for future A Line analysis.
 */
public class AlineAnalysisRequest
{
    private Long tenantId;
    private String userId;
    private String sessionId;
    private String taskId;
    private String prescriptionId;
    private String equipmentCode;
    private String deviceCode;
    private String exerciseType;
    private Integer completedSets;
    private Integer totalReps;
    private Integer durationMin;
    private Double totalVolumeKg;
    private List<Map<String, Object>> sets = Collections.emptyList();
    private String source = "b_line";

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getExerciseType() { return exerciseType; }
    public void setExerciseType(String exerciseType) { this.exerciseType = exerciseType; }
    public Integer getCompletedSets() { return completedSets; }
    public void setCompletedSets(Integer completedSets) { this.completedSets = completedSets; }
    public Integer getTotalReps() { return totalReps; }
    public void setTotalReps(Integer totalReps) { this.totalReps = totalReps; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public Double getTotalVolumeKg() { return totalVolumeKg; }
    public void setTotalVolumeKg(Double totalVolumeKg) { this.totalVolumeKg = totalVolumeKg; }
    public List<Map<String, Object>> getSets() { return sets; }
    public void setSets(List<Map<String, Object>> sets) {
        this.sets = sets != null ? sets : Collections.emptyList();
    }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
