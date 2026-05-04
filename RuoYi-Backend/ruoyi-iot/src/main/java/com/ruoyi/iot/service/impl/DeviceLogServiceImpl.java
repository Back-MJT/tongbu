package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.iot.mapper.DeviceLogMapper;
import com.ruoyi.iot.domain.entity.DeviceLog;
import com.ruoyi.iot.service.IDeviceLogService;
import com.ruoyi.common.tenant.TenantContextHolder;

/**
 * IoT 设备日志 业务层处理
 *
 * @author ruoyi
 */
@Service
public class DeviceLogServiceImpl implements IDeviceLogService
{
    @Autowired
    private DeviceLogMapper deviceLogMapper;

    /**
     * 查询日志列表
     */
    @Override
    public List<DeviceLog> selectDeviceLogList(DeviceLog deviceLog)
    {
        deviceLog.setTenantId(TenantContextHolder.getTenantId());
        return deviceLogMapper.selectDeviceLogList(deviceLog);
    }

    /**
     * 查询设备最新N条日志
     */
    @Override
    public List<DeviceLog> selectLatestByDeviceId(Long deviceId, int limit)
    {
        return deviceLogMapper.selectLatestByDeviceId(deviceId, TenantContextHolder.getTenantId(), limit);
    }

    /**
     * 新增日志
     */
    @Override
    @Transactional
    public int insertDeviceLog(DeviceLog deviceLog)
    {
        deviceLog.setTenantId(TenantContextHolder.getTenantId());
        return deviceLogMapper.insertDeviceLog(deviceLog);
    }

    /**
     * 删除日志
     */
    @Override
    @Transactional
    public int deleteDeviceLogById(Long logId)
    {
        DeviceLog l = new DeviceLog();
        l.setLogId(logId);
        l.setTenantId(TenantContextHolder.getTenantId());
        return deviceLogMapper.deleteDeviceLogById(l);
    }

    /**
     * 批量删除日志
     */
    @Override
    @Transactional
    public int deleteDeviceLogByIds(Long[] logIds)
    {
        return deviceLogMapper.deleteDeviceLogByIds(logIds, TenantContextHolder.getTenantId());
    }

    /**
     * 清空设备日志
     */
    @Override
    @Transactional
    public int deleteByDeviceId(Long deviceId)
    {
        DeviceLog l = new DeviceLog();
        l.setDeviceId(deviceId);
        l.setTenantId(TenantContextHolder.getTenantId());
        return deviceLogMapper.deleteByDeviceId(l);
    }
}
