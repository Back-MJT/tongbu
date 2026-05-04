package com.ruoyi.iot.mapper;

import java.util.List;
import com.ruoyi.iot.domain.entity.DeviceGroup;
import org.apache.ibatis.annotations.Param;

/**
 * IoT 设备分组 Mapper 接口
 *
 * @author ruoyi
 */
public interface DeviceGroupMapper
{
    /**
     * 查询分组列表
     */
    public List<DeviceGroup> selectDeviceGroupList(DeviceGroup deviceGroup);

    /**
     * 查询分组详情
     */
    public DeviceGroup selectDeviceGroupById(DeviceGroup deviceGroup);

    /**
     * 新增分组
     */
    public int insertDeviceGroup(DeviceGroup deviceGroup);

    /**
     * 修改分组
     */
    public int updateDeviceGroup(DeviceGroup deviceGroup);

    /**
     * 删除分组（逻辑删除）
     */
    public int deleteDeviceGroupById(DeviceGroup deviceGroup);

    /**
     * 批量删除分组
     */
    public int deleteDeviceGroupByIds(@Param("groupIds") Long[] groupIds, @Param("tenantId") Long tenantId);

    /**
     * 查询分组下的设备数量
     */
    public int countDevicesInGroup(Long groupId);

    /**
     * 添加设备到分组
     */
    public int insertDeviceGroupRel(@Param("deviceId") Long deviceId, @Param("groupId") Long groupId);

    /**
     * 从分组移除设备
     */
    public int deleteDeviceGroupRel(@Param("deviceId") Long deviceId, @Param("groupId") Long groupId);

    /**
     * 清空分组下所有设备
     */
    public int deleteAllDeviceGroupRels(Long groupId);

    /**
     * 查询设备所属分组ID列表
     */
    public List<Long> selectGroupIdsByDeviceId(Long deviceId);

    /**
     * 查询分组下所有设备ID
     */
    public List<Long> selectDeviceIdsByGroupId(Long groupId);
}
