package com.ruoyi.iot.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * IMU时序数据记录 对应 device_imu_data 表 (TimescaleDB hypertable)
 *
 * TimescaleDB: 无主键，以 time + device_id 为时间维度
 * 字段名映射: accelX → accel_x, gyroX → gyro_x (下划线风格)
 *
 * @author ruoyi
 */
public class ImuDataRecord
{
    /** 时间戳 (TimescaleDB time dimension) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private Date time;

    /** 设备ID (关联 iot_device.device_id) */
    private Long deviceId;

    /** 设备编号 (MAC地址，方便查询) */
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

    /** BLE广播序列号 */
    private Integer sequence;

    /** 电池电量 0-100 */
    private Integer batteryLevel;

    /** 运动类型 (idle/running/walking/strength) */
    private String motionType;

    /** 步数计数 */
    private Integer stepCount;

    // ---- getters / setters ----

    public Date getTime()
    {
        return time;
    }

    public void setTime(Date time)
    {
        this.time = time;
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
        return "ImuDataRecord{time=" + time
            + ", deviceId=" + deviceId
            + ", deviceCode='" + deviceCode + '\''
            + ", accelX=" + accelX + ", accelY=" + accelY + ", accelZ=" + accelZ
            + ", gyroX=" + gyroX + ", gyroY=" + gyroY + ", gyroZ=" + gyroZ
            + '}';
    }
}
