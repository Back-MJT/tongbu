package com.ruoyi.iot.mapper;

import java.util.List;
import com.ruoyi.iot.domain.entity.DeviceLog;
import org.apache.ibatis.annotations.Param;

/**
 * IoT 设备日志 Mapper 接口
 *
 * @author ruoyi
 */
public interface DeviceLogMapper
{
    /**
     * 查询设备日志列表
     */
    public List<DeviceLog> selectDeviceLogList(DeviceLog deviceLog);

    /**
     * 查询设备最新N条日志
     */
    public List<DeviceLog> selectLatestByDeviceId(@Param("deviceId") Long deviceId,
                                                  @Param("tenantId") Long tenantId,
                                                  @Param("limit") int limit);

    /**
     * 查询设备最新一条日志
     */
    public DeviceLog selectLatestLogByDeviceId(DeviceLog deviceLog);

    /**
     * 新增设备日志
     */
    public int insertDeviceLog(DeviceLog deviceLog);

    /**
     * 删除设备日志
     */
    public int deleteDeviceLogById(DeviceLog deviceLog);

    /**
     * 批量删除日志
     */
    public int deleteDeviceLogByIds(@Param("logIds") Long[] logIds, @Param("tenantId") Long tenantId);

    /**
     * 清空设备日志
     */
    public int deleteByDeviceId(DeviceLog deviceLog);
}
