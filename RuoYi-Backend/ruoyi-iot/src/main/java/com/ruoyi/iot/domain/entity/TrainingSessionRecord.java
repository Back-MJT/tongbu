package com.ruoyi.iot.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 训练会话记录
 */
public class TrainingSessionRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private Long userId;
    private Long tenantId;
    private Long deviceId;
    private String equipmentCode;
    private String deviceCode;
    private String exerciseType;
    private Integer completedSets;
    private Integer totalReps;
    private BigDecimal totalVolumeKg;
    private Integer durationMinutes;
    private Integer avgHeartRate;
    private Integer caloriesBurned;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date sessionDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sessionTime;
    private Long prescriptionId;
    private String stage;
    private String delFlag;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
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
    public BigDecimal getTotalVolumeKg() { return totalVolumeKg; }
    public void setTotalVolumeKg(BigDecimal totalVolumeKg) { this.totalVolumeKg = totalVolumeKg; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Integer getAvgHeartRate() { return avgHeartRate; }
    public void setAvgHeartRate(Integer avgHeartRate) { this.avgHeartRate = avgHeartRate; }
    public Integer getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(Integer caloriesBurned) { this.caloriesBurned = caloriesBurned; }
    public Date getSessionDate() { return sessionDate; }
    public void setSessionDate(Date sessionDate) { this.sessionDate = sessionDate; }
    public Date getSessionTime() { return sessionTime; }
    public void setSessionTime(Date sessionTime) { this.sessionTime = sessionTime; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
