package com.ruoyi.iot.domain.entity;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 器械主数据
 */
public class Equipment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private String location;
    private String qrContent;
    private Long tenantId;
    private String status;
    private String delFlag;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String bluetoothName;

    public Long getEquipmentId()
    {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId)
    {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentCode()
    {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode)
    {
        this.equipmentCode = equipmentCode;
    }

    public String getEquipmentName()
    {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName)
    {
        this.equipmentName = equipmentName;
    }

    public String getEquipmentType()
    {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType)
    {
        this.equipmentType = equipmentType;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getQrContent()
    {
        return qrContent;
    }

    public void setQrContent(String qrContent)
    {
        this.qrContent = qrContent;
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

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

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

    public String getBluetoothName()
    {
        return bluetoothName;
    }

    public void setBluetoothName(String bluetoothName)
    {
        this.bluetoothName = bluetoothName;
    }
}
