package com.ruoyi.iot.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 训练组明细
 */
public class TrainingSetDetail
{
    private Long setId;
    private Long sessionId;
    private Long userId;
    private Long tenantId;
    private String equipmentCode;
    private String deviceCode;
    private Integer setNo;
    private Integer reps;
    private Integer durationSec;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endedAt;

    public Long getSetId() { return setId; }
    public void setSetId(Long setId) { this.setId = setId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public Integer getSetNo() { return setNo; }
    public void setSetNo(Integer setNo) { this.setNo = setNo; }
    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getEndedAt() { return endedAt; }
    public void setEndedAt(Date endedAt) { this.endedAt = endedAt; }
}
