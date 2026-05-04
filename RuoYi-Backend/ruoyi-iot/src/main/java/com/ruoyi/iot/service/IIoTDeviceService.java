package com.ruoyi.iot.service;

import java.util.List;
import com.ruoyi.iot.domain.entity.IoTDevice;

/**
 * IoT 设备 Service接口
 *
 * @author ruoyi
 */
public interface IIoTDeviceService
{
    /**
     * 查询设备列表
     */
    public List<IoTDevice> selectDeviceList(IoTDevice iotDevice);

    /**
     * 查询设备详情
     */
    public IoTDevice selectDeviceById(Long deviceId);

    /**
     * 根据设备编号查询
     */
    public IoTDevice selectDeviceByCode(String deviceCode);

    /**
     * 新增设备
     */
    public int insertDevice(IoTDevice iotDevice);

    /**
     * 修改设备
     */
    public int updateDevice(IoTDevice iotDevice);

    /**
     * 删除设备（逻辑删除）
     */
    public int deleteDeviceById(Long deviceId);

    /**
     * 批量删除设备
     */
    public int deleteDeviceByIds(Long[] deviceIds);

    /**
     * 更新设备状态
     */
    public int updateDeviceStatus(Long deviceId, String status);

    /**
     * 更新最后在线时间
     */
    public int updateLastSeenAt(Long deviceId);

    /**
     * 查询厂商下设备数量
     */
    public int countByManufacturerId(Long manufacturerId);

    /**
     * 根据设备编号查找设备ID（供IMU数据管道内部使用）
     * @param deviceCode 设备编号
     * @return 设备ID，若不存在返回null
     */
    public Long findDeviceIdByCode(String deviceCode);

    /**
     * 根据厂商ID查询该厂商下所有设备
     * XIN-145: manufacturer detail页的设备列表
     */
    public List<IoTDevice> selectDeviceListByManufacturerId(Long manufacturerId);
}
