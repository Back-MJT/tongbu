package com.ruoyi.iot.mapper;

import java.util.List;
import com.ruoyi.iot.domain.entity.IoTDevice;
import org.apache.ibatis.annotations.Param;

/**
 * IoT 设备 Mapper 接口
 *
 * @author ruoyi
 */
public interface IoTDeviceMapper
{
    /**
     * 查询设备列表
     */
    public List<IoTDevice> selectDeviceList(IoTDevice iotDevice);

    /**
     * 查询设备详情
     */
    public IoTDevice selectDeviceById(IoTDevice iotDevice);

    /**
     * 根据设备编号查询
     */
    public IoTDevice selectDeviceByCode(IoTDevice iotDevice);

    /**
     * 根据设备编号查询（忽略删除标志，内部使用）
     */
    public IoTDevice selectDeviceByCodeRaw(String deviceCode);

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
    public int deleteDeviceById(IoTDevice iotDevice);

    /**
     * 批量删除设备
     */
    public int deleteDeviceByIds(@Param("deviceIds") Long[] deviceIds, @Param("tenantId") Long tenantId);

    /**
     * 更新设备状态
     */
    public int updateDeviceStatus(@Param("deviceId") Long deviceId, @Param("status") String status);

    /**
     * 更新最后在线时间
     */
    public int updateLastSeenAt(@Param("deviceId") Long deviceId);

    /**
     * 查询厂商下设备数量
     */
    public int countByManufacturerId(Long manufacturerId);
}
