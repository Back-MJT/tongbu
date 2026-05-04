package com.ruoyi.iot.domain.model;

/**
 * 小程序扫码后返回的器械解析结果
 */
public class EquipmentResolveResult
{
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private String location;
    private Long deviceId;
    private String deviceCode;
    private String bluetoothName;
    private String serviceUuid;
    private String notifyCharUuid;
    private CountingConfig countingConfig;

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

    public CountingConfig getCountingConfig()
    {
        return countingConfig;
    }

    public void setCountingConfig(CountingConfig countingConfig)
    {
        this.countingConfig = countingConfig;
    }

    public static class CountingConfig
    {
        private String mainAxis;
        private Double upThreshold;
        private Double downThreshold;
        private Integer minIntervalMs;
        private Double minRange;
        private Integer smoothingWindow;

        public String getMainAxis()
        {
            return mainAxis;
        }

        public void setMainAxis(String mainAxis)
        {
            this.mainAxis = mainAxis;
        }

        public Double getUpThreshold()
        {
            return upThreshold;
        }

        public void setUpThreshold(Double upThreshold)
        {
            this.upThreshold = upThreshold;
        }

        public Double getDownThreshold()
        {
            return downThreshold;
        }

        public void setDownThreshold(Double downThreshold)
        {
            this.downThreshold = downThreshold;
        }

        public Integer getMinIntervalMs()
        {
            return minIntervalMs;
        }

        public void setMinIntervalMs(Integer minIntervalMs)
        {
            this.minIntervalMs = minIntervalMs;
        }

        public Double getMinRange()
        {
            return minRange;
        }

        public void setMinRange(Double minRange)
        {
            this.minRange = minRange;
        }

        public Integer getSmoothingWindow()
        {
            return smoothingWindow;
        }

        public void setSmoothingWindow(Integer smoothingWindow)
        {
            this.smoothingWindow = smoothingWindow;
        }
    }
}
