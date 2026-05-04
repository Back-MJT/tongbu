package com.ruoyi.iot.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * IoT 设备事件日志表 iot_device_log
 *
 * @author ruoyi
 */
public class DeviceLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 设备ID */
    private Long deviceId;

    /** 事件类型 (online/offline/error/ota/heartbeat) */
    private String eventType;

    /** 事件详情 (JSON) */
    private String eventData;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(Long deviceId)
    {
        this.deviceId = deviceId;
    }

    public String getEventType()
    {
        return eventType;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public String getEventData()
    {
        return eventData;
    }

    public void setEventData(String eventData)
    {
        this.eventData = eventData;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
            .append("logId", getLogId())
            .append("deviceId", getDeviceId())
            .append("eventType", getEventType())
            .append("createTime", getCreateTime())
            .toString();
    }
}
