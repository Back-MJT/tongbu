package com.ruoyi.iot.service;

import com.ruoyi.iot.domain.entity.ImuDataRecord;
import com.ruoyi.iot.domain.model.ImuData;

import java.util.Date;
import java.util.List;

/**
 * IMU时序数据 Service接口
 *
 * @author ruoyi
 */
public interface IImuDataService
{
    /**
     * 接收单条IMU数据（从MQTT Listener调用）
     * 数据会缓存在内存缓冲区，达到阈值或超时后批量写入PostgreSQL
     *
     * @param imuData   MQTT消息解析出的IMU数据
     * @param deviceId  设备数据库ID（可为空，会自动查询）
     */
    void onImuDataReceived(ImuData imuData, Long deviceId);

    /**
     * 强制刷新缓冲区（立即写入所有待持久化的数据）
     * 应用关闭时调用，避免数据丢失
     */
    void flushBuffer();

    /**
     * 按设备编号查询最近N条历史记录
     *
     * @param deviceCode 设备编号
     * @param limit      行数限制
     * @return IMU数据列表（按时间倒序）
     */
    List<ImuDataRecord> selectLatestByDeviceCode(String deviceCode, int limit);

    /**
     * 按设备编号查询指定时间范围内的历史数据
     *
     * @param deviceCode 设备编号
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return IMU数据列表（按时间正序）
     */
    List<ImuDataRecord> selectByDeviceCodeAndTimeRange(String deviceCode, Date beginTime, Date endTime);

    /**
     * 按设备编号统计指定时间范围内的数据点数
     *
     * @param deviceCode 设备编号
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 数据点数量
     */
    long countByDeviceCodeAndTimeRange(String deviceCode, Date beginTime, Date endTime);
}
