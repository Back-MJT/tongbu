package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;

/**
 * 设备传感器数据
 * Data from wearable / fitness equipment IMU sensors.
 *
 * Maps to BLE broadcast payload from nRF52840 firmware (Eddystone TLM + custom frames).
 */
public class DeviceData
{
    private String deviceId;
    private String userId;
    private LocalDateTime timestamp;

    // IMU data (LSM6DSO or MPU6050)
    private double accelX;
    private double accelY;
    private double accelZ;
    private double gyroX;
    private double gyroY;
    private double gyroZ;

    // Derived metrics
    private Integer heartRate;          // bpm, nullable — may come from chest strap
    private Integer stepCount;
    private Double caloriesBurned;
    private Integer repCount;           // from firmware rep-counting algorithm

    // Signal quality
    private int rssi;                   // BLE signal strength
    private double batteryLevel;        // 0.0 – 1.0

    public DeviceData() {}

    /**
     * Calculate acceleration magnitude vector.
     * sqrt(ax^2 + ay^2 + az^2) — used for activity detection.
     */
    public double accelerationMagnitude()
    {
        return Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
    }

    // --- Getters & Setters ---

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getAccelX() { return accelX; }
    public void setAccelX(double accelX) { this.accelX = accelX; }

    public double getAccelY() { return accelY; }
    public void setAccelY(double accelY) { this.accelY = accelY; }

    public double getAccelZ() { return accelZ; }
    public void setAccelZ(double accelZ) { this.accelZ = accelZ; }

    public double getGyroX() { return gyroX; }
    public void setGyroX(double gyroX) { this.gyroX = gyroX; }

    public double getGyroY() { return gyroY; }
    public void setGyroY(double gyroY) { this.gyroY = gyroY; }

    public double getGyroZ() { return gyroZ; }
    public void setGyroZ(double gyroZ) { this.gyroZ = gyroZ; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }

    public Double getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(Double caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public Integer getRepCount() { return repCount; }
    public void setRepCount(Integer repCount) { this.repCount = repCount; }

    public int getRssi() { return rssi; }
    public void setRssi(int rssi) { this.rssi = rssi; }

    public double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }
}
