package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.iot.mapper.IoTDeviceMapper;
import com.ruoyi.iot.domain.entity.IoTDevice;
import com.ruoyi.iot.service.IIoTDeviceService;
import com.ruoyi.common.tenant.TenantContextHolder;

/**
 * IoT 设备 业务层处理
 *
 * @author ruoyi
 */
@Service
public class IoTDeviceServiceImpl implements IIoTDeviceService
{
    @Autowired
    private IoTDeviceMapper deviceMapper;

    /**
     * 查询设备列表
     */
    @Override
    public List<IoTDevice> selectDeviceList(IoTDevice iotDevice)
    {
        iotDevice.setTenantId(TenantContextHolder.getTenantId());
        return deviceMapper.selectDeviceList(iotDevice);
    }

    /**
     * 查询设备详情
     */
    @Override
    public IoTDevice selectDeviceById(Long deviceId)
    {
        IoTDevice d = new IoTDevice();
        d.setDeviceId(deviceId);
        d.setTenantId(TenantContextHolder.getTenantId());
        return deviceMapper.selectDeviceById(d);
    }

    /**
     * 根据设备编号查询
     */
    @Override
    public IoTDevice selectDeviceByCode(String deviceCode)
    {
        IoTDevice d = new IoTDevice();
        d.setDeviceCode(deviceCode);
        d.setTenantId(TenantContextHolder.getTenantId());
        return deviceMapper.selectDeviceByCode(d);
    }

    /**
     * 新增设备
     */
    @Override
    @Transactional
    public int insertDevice(IoTDevice iotDevice)
    {
        iotDevice.setTenantId(TenantContextHolder.getTenantId());
        validateDeviceCodeUnique(iotDevice);
        return deviceMapper.insertDevice(iotDevice);
    }

    /**
     * 修改设备
     */
    @Override
    @Transactional
    public int updateDevice(IoTDevice iotDevice)
    {
        iotDevice.setTenantId(TenantContextHolder.getTenantId());
        validateDeviceCodeUnique(iotDevice);
        return deviceMapper.updateDevice(iotDevice);
    }

    private void validateDeviceCodeUnique(IoTDevice iotDevice)
    {
        if (StringUtils.isBlank(iotDevice.getDeviceCode()))
        {
            throw new ServiceException("传感器编号不能为空");
        }
        IoTDevice query = new IoTDevice();
        query.setTenantId(iotDevice.getTenantId());
        query.setDeviceCode(iotDevice.getDeviceCode());
        IoTDevice existing = deviceMapper.selectDeviceByCode(query);
        if (existing != null && !existing.getDeviceId().equals(iotDevice.getDeviceId()))
        {
            throw new ServiceException("传感器编号“" + iotDevice.getDeviceCode() + "”已存在，请修改现有传感器或使用新的编号");
        }
    }

    /**
     * 删除设备（逻辑删除）
     */
    @Override
    @Transactional
    public int deleteDeviceById(Long deviceId)
    {
        IoTDevice d = new IoTDevice();
        d.setDeviceId(deviceId);
        d.setTenantId(TenantContextHolder.getTenantId());
        return deviceMapper.deleteDeviceById(d);
    }

    /**
     * 批量删除设备
     */
    @Override
    @Transactional
    public int deleteDeviceByIds(Long[] deviceIds)
    {
        return deviceMapper.deleteDeviceByIds(deviceIds, TenantContextHolder.getTenantId());
    }

    /**
     * 更新设备状态
     */
    @Override
    @Transactional
    public int updateDeviceStatus(Long deviceId, String status)
    {
        return deviceMapper.updateDeviceStatus(deviceId, status);
    }

    /**
     * 更新最后在线时间
     */
    @Override
    @Transactional
    public int updateLastSeenAt(Long deviceId)
    {
        return deviceMapper.updateLastSeenAt(deviceId);
    }

    /**
     * 查询厂商下设备数量
     */
    @Override
    public int countByManufacturerId(Long manufacturerId)
    {
        return deviceMapper.countByManufacturerId(manufacturerId);
    }

    /**
     * 根据设备编号查找设备ID
     */
    @Override
    public Long findDeviceIdByCode(String deviceCode)
    {
        IoTDevice device = deviceMapper.selectDeviceByCodeRaw(deviceCode);
        return device != null ? device.getDeviceId() : null;
    }

    /**
     * 根据厂商ID查询该厂商下所有设备
     * XIN-145: manufacturer detail页的设备列表
     */
    @Override
    public List<IoTDevice> selectDeviceListByManufacturerId(Long manufacturerId)
    {
        IoTDevice query = new IoTDevice();
        query.setManufacturerId(manufacturerId);
        query.setTenantId(TenantContextHolder.getTenantId());
        return deviceMapper.selectDeviceList(query);
    }
}
