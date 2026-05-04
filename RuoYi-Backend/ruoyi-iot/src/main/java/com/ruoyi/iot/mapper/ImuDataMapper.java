package com.ruoyi.iot.mapper;

import com.ruoyi.iot.domain.entity.ImuDataRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * IMU时序数据 Mapper
 *
 * TimescaleDB hypertable: device_imu_data
 * 无主键，依靠 time + device_id 定位
 *
 * @author ruoyi
 */
public interface ImuDataMapper
{
    /**
     * 批量写入IMU数据（高频写入场景使用）
     * @param list 数据列表
     * @return 写入行数
     */
    int batchInsert(@Param("list") List<ImuDataRecord> list);

    /**
     * 按设备查询最近N条记录
     * @param deviceId 设备ID
     * @param limit 行数限制
     * @return IMU数据列表（按时间倒序）
     */
    List<ImuDataRecord> selectLatestByDeviceId(@Param("deviceId") Long deviceId, @Param("limit") int limit);

    /**
     * 按设备编号查询最近N条记录
     * @param deviceCode 设备编号
     * @param limit 行数限制
     * @return IMU数据列表（按时间倒序）
     */
    List<ImuDataRecord> selectLatestByDeviceCode(@Param("deviceCode") String deviceCode, @Param("limit") int limit);

    /**
     * 按设备编号查询指定时间范围内的数据
     * @param deviceCode 设备编号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return IMU数据列表（按时间正序）
     */
    List<ImuDataRecord> selectByDeviceCodeAndTimeRange(
        @Param("deviceCode") String deviceCode,
        @Param("beginTime") Date beginTime,
        @Param("endTime") Date endTime);

    /**
     * 按设备编号统计指定时间范围内的数据点数
     * @param deviceCode 设备编号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 数据点数量
     */
    long countByDeviceCodeAndTimeRange(
        @Param("deviceCode") String deviceCode,
        @Param("beginTime") Date beginTime,
        @Param("endTime") Date endTime);
}
