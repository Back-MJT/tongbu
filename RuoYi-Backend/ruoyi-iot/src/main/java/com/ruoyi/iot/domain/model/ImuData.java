package com.ruoyi.iot.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * IMU 数据模型（加速度 + 陀螺仪）
 * 对应 nRF52840 BLE广播数据解析后的格式
 *
 * @author ruoyi
 */
public class ImuData
{
    /** 设备编号（MAC地址） */
    private String deviceCode;

    /** 加速度 X */
    private Double accelX;

    /** 加速度 Y */
    private Double accelY;

    /** 加速度 Z */
    private Double accelZ;

    /** 陀螺仪 X */
    private Double gyroX;

    /** 陀螺仪 Y */
    private Double gyroY;

    /** 陀螺仪 Z */
    private Double gyroZ;

    /** 采样时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date sampleTime;

    /** 序列号（BLE广播序列） */
    private Integer sequence;

    /** 电池电量 (0-100) */
    private Integer batteryLevel;

    /** 运动类型 (idle/running/walking/strength) */
    private String motionType;

    /** 步数计数 */
    private Integer stepCount;

    public String getDeviceCode()
    {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode)
    {
        this.deviceCode = deviceCode;
    }

    public Double getAccelX()
    {
        return accelX;
    }

    public void setAccelX(Double accelX)
    {
        this.accelX = accelX;
    }

    public Double getAccelY()
    {
        return accelY;
    }

    public void setAccelY(Double accelY)
    {
        this.accelY = accelY;
    }

    public Double getAccelZ()
    {
        return accelZ;
    }

    public void setAccelZ(Double accelZ)
    {
        this.accelZ = accelZ;
    }

    public Double getGyroX()
    {
        return gyroX;
    }

    public void setGyroX(Double gyroX)
    {
        this.gyroX = gyroX;
    }

    public Double getGyroY()
    {
        return gyroY;
    }

    public void setGyroY(Double gyroY)
    {
        this.gyroY = gyroY;
    }

    public Double getGyroZ()
    {
        return gyroZ;
    }

    public void setGyroZ(Double gyroZ)
    {
        this.gyroZ = gyroZ;
    }

    public Date getSampleTime()
    {
        return sampleTime;
    }

    public void setSampleTime(Date sampleTime)
    {
        this.sampleTime = sampleTime;
    }

    public Integer getSequence()
    {
        return sequence;
    }

    public void setSequence(Integer sequence)
    {
        this.sequence = sequence;
    }

    public Integer getBatteryLevel()
    {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel)
    {
        this.batteryLevel = batteryLevel;
    }

    public String getMotionType()
    {
        return motionType;
    }

    public void setMotionType(String motionType)
    {
        this.motionType = motionType;
    }

    public Integer getStepCount()
    {
        return stepCount;
    }

    public void setStepCount(Integer stepCount)
    {
        this.stepCount = stepCount;
    }

    @Override
    public String toString()
    {
        return "ImuData{deviceCode='" + deviceCode + '\''
            + ", accelX=" + accelX + ", accelY=" + accelY + ", accelZ=" + accelZ
            + ", gyroX=" + gyroX + ", gyroY=" + gyroY + ", gyroZ=" + gyroZ
            + ", sampleTime=" + sampleTime + '}';
    }
}
