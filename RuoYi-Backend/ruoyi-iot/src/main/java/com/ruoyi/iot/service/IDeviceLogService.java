package com.ruoyi.iot.service;

import java.util.List;
import com.ruoyi.iot.domain.entity.DeviceLog;

/**
 * IoT 设备日志 Service接口
 *
 * @author ruoyi
 */
public interface IDeviceLogService
{
    /**
     * 查询日志列表
     */
    public List<DeviceLog> selectDeviceLogList(DeviceLog deviceLog);

    /**
     * 查询设备最新N条日志
     */
    public List<DeviceLog> selectLatestByDeviceId(Long deviceId, int limit);

    /**
     * 新增日志
     */
    public int insertDeviceLog(DeviceLog deviceLog);

    /**
     * 删除日志
     */
    public int deleteDeviceLogById(Long logId);

    /**
     * 批量删除日志
     */
    public int deleteDeviceLogByIds(Long[] logIds);

    /**
     * 清空设备日志
     */
    public int deleteByDeviceId(Long deviceId);
}
