package com.ruoyi.iot.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * IoT 设备表 iot_device
 *
 * @author ruoyi
 */
public class IoTDevice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 设备编号（MAC 或 IMEI） */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型 (treadmill/elliptical/pilates_bed/strength_machine) */
    private String deviceType;

    /** 协议 (ble/mqtt/wifi) */
    private String protocol;

    /** 所属厂商ID */
    private Long manufacturerId;

    /** 租户ID（厂商账号） */
    private Long tenantId;

    /** 状态 (online/offline/error/maintenance) */
    private String status;

    /** 固件版本 */
    private String firmwareVersion;

    /** 蓝牙广播名称 */
    private String bluetoothName;

    /** BLE 服务 UUID */
    private String serviceUuid;

    /** BLE 通知特征 UUID */
    private String notifyCharUuid;

    /** 最后在线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenAt;

    /** 扩展字段 (JSON) */
    private String metadata;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 厂商名称（关联查询填充） */
    private String manufacturerName;

    public Long getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(Long deviceId)
    {
        this.deviceId = deviceId;
    }

    public String getDeviceCode()
    {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode)
    {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getDeviceType()
    {
        return deviceType;
    }

    public void setDeviceType(String deviceType)
    {
        this.deviceType = deviceType;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public Long getManufacturerId()
    {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId)
    {
        this.manufacturerId = manufacturerId;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getFirmwareVersion()
    {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion)
    {
        this.firmwareVersion = firmwareVersion;
    }

    public String getBluetoothName()
    {
        return bluetoothName;
    }

    public void setBluetoothName(String bluetoothName)
    {
        this.bluetoothName = bluetoothName;
    }

    public String getServiceUuid()
    {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid)
    {
        this.serviceUuid = serviceUuid;
    }

    public String getNotifyCharUuid()
    {
        return notifyCharUuid;
    }

    public void setNotifyCharUuid(String notifyCharUuid)
    {
        this.notifyCharUuid = notifyCharUuid;
    }

    public Date getLastSeenAt()
    {
        return lastSeenAt;
    }

    public void setLastSeenAt(Date lastSeenAt)
    {
        this.lastSeenAt = lastSeenAt;
    }

    public String getMetadata()
    {
        return metadata;
    }

    public void setMetadata(String metadata)
    {
        this.metadata = metadata;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getManufacturerName()
    {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName)
    {
        this.manufacturerName = manufacturerName;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
            .append("deviceId", getDeviceId())
            .append("deviceCode", getDeviceCode())
            .append("deviceName", getDeviceName())
            .append("deviceType", getDeviceType())
            .append("protocol", getProtocol())
            .append("manufacturerId", getManufacturerId())
            .append("tenantId", getTenantId())
            .append("status", getStatus())
            .append("firmwareVersion", getFirmwareVersion())
            .append("bluetoothName", getBluetoothName())
            .append("serviceUuid", getServiceUuid())
            .append("notifyCharUuid", getNotifyCharUuid())
            .append("lastSeenAt", getLastSeenAt())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
