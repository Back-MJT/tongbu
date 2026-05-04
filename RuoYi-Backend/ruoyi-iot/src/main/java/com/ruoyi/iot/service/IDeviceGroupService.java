package com.ruoyi.iot.service;

import java.util.List;
import com.ruoyi.iot.domain.entity.DeviceGroup;

/**
 * IoT 设备分组 Service接口
 *
 * @author ruoyi
 */
public interface IDeviceGroupService
{
    /**
     * 查询分组列表
     */
    public List<DeviceGroup> selectDeviceGroupList(DeviceGroup deviceGroup);

    /**
     * 查询分组详情
     */
    public DeviceGroup selectDeviceGroupById(Long groupId);

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
    public int deleteDeviceGroupById(Long groupId);

    /**
     * 批量删除分组
     */
    public int deleteDeviceGroupByIds(Long[] groupIds);

    /**
     * 添加设备到分组
     */
    public int addDeviceToGroup(Long deviceId, Long groupId);

    /**
     * 从分组移除设备
     */
    public int removeDeviceFromGroup(Long deviceId, Long groupId);

    /**
     * 清空分组下所有设备
     */
    public int clearGroupDevices(Long groupId);

    /**
     * 查询设备所属分组
     */
    public List<Long> getGroupsByDeviceId(Long deviceId);

    /**
     * 查询分组下所有设备ID
     */
    public List<Long> getDeviceIdsByGroupId(Long groupId);
}
