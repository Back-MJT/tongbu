package com.ruoyi.iot.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.iot.mapper.DeviceGroupMapper;
import com.ruoyi.iot.domain.entity.DeviceGroup;
import com.ruoyi.iot.service.IDeviceGroupService;
import com.ruoyi.common.tenant.TenantContextHolder;

/**
 * IoT 设备分组 业务层处理
 *
 * @author ruoyi
 */
@Service
public class DeviceGroupServiceImpl implements IDeviceGroupService
{
    @Autowired
    private DeviceGroupMapper deviceGroupMapper;

    /**
     * 查询分组列表
     */
    @Override
    public List<DeviceGroup> selectDeviceGroupList(DeviceGroup deviceGroup)
    {
        deviceGroup.setTenantId(TenantContextHolder.getTenantId());
        return deviceGroupMapper.selectDeviceGroupList(deviceGroup);
    }

    /**
     * 查询分组详情
     */
    @Override
    public DeviceGroup selectDeviceGroupById(Long groupId)
    {
        DeviceGroup g = new DeviceGroup();
        g.setGroupId(groupId);
        g.setTenantId(TenantContextHolder.getTenantId());
        return deviceGroupMapper.selectDeviceGroupById(g);
    }

    /**
     * 新增分组
     */
    @Override
    @Transactional
    public int insertDeviceGroup(DeviceGroup deviceGroup)
    {
        deviceGroup.setTenantId(TenantContextHolder.getTenantId());
        return deviceGroupMapper.insertDeviceGroup(deviceGroup);
    }

    /**
     * 修改分组
     */
    @Override
    @Transactional
    public int updateDeviceGroup(DeviceGroup deviceGroup)
    {
        deviceGroup.setTenantId(TenantContextHolder.getTenantId());
        return deviceGroupMapper.updateDeviceGroup(deviceGroup);
    }

    /**
     * 删除分组（逻辑删除）
     */
    @Override
    @Transactional
    public int deleteDeviceGroupById(Long groupId)
    {
        DeviceGroup g = new DeviceGroup();
        g.setGroupId(groupId);
        g.setTenantId(TenantContextHolder.getTenantId());
        // 先清空分组下的设备关联
        deviceGroupMapper.deleteAllDeviceGroupRels(groupId);
        return deviceGroupMapper.deleteDeviceGroupById(g);
    }

    /**
     * 批量删除分组
     */
    @Override
    @Transactional
    public int deleteDeviceGroupByIds(Long[] groupIds)
    {
        Long tenantId = TenantContextHolder.getTenantId();
        for (Long groupId : groupIds)
        {
            deviceGroupMapper.deleteAllDeviceGroupRels(groupId);
        }
        return deviceGroupMapper.deleteDeviceGroupByIds(groupIds, tenantId);
    }

    /**
     * 添加设备到分组
     */
    @Override
    @Transactional
    public int addDeviceToGroup(Long deviceId, Long groupId)
    {
        return deviceGroupMapper.insertDeviceGroupRel(deviceId, groupId);
    }

    /**
     * 从分组移除设备
     */
    @Override
    @Transactional
    public int removeDeviceFromGroup(Long deviceId, Long groupId)
    {
        return deviceGroupMapper.deleteDeviceGroupRel(deviceId, groupId);
    }

    /**
     * 清空分组下所有设备
     */
    @Override
    @Transactional
    public int clearGroupDevices(Long groupId)
    {
        return deviceGroupMapper.deleteAllDeviceGroupRels(groupId);
    }

    /**
     * 查询设备所属分组
     */
    @Override
    public List<Long> getGroupsByDeviceId(Long deviceId)
    {
        return deviceGroupMapper.selectGroupIdsByDeviceId(deviceId);
    }

    /**
     * 查询分组下所有设备ID
     */
    @Override
    public List<Long> getDeviceIdsByGroupId(Long groupId)
    {
        return deviceGroupMapper.selectDeviceIdsByGroupId(groupId);
    }
}
