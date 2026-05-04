package com.ruoyi.iot.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.iot.domain.entity.DeviceGroup;
import com.ruoyi.iot.service.IDeviceGroupService;

/**
 * IoT 设备分组管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/iot/group")
public class DeviceGroupController extends BaseController
{
    @Autowired
    private IDeviceGroupService deviceGroupService;

    /**
     * 查询分组列表
     */
    @PreAuthorize("@ss.hasPermi('iot:group:list')")
    @GetMapping("/list")
    public TableDataInfo list(DeviceGroup deviceGroup)
    {
        startPage();
        List<DeviceGroup> list = deviceGroupService.selectDeviceGroupList(deviceGroup);
        return getDataTable(list);
    }

    /**
     * 获取分组详情
     */
    @PreAuthorize("@ss.hasPermi('iot:group:query')")
    @GetMapping(value = "/{groupId}")
    public AjaxResult getInfo(@PathVariable("groupId") Long groupId)
    {
        return success(deviceGroupService.selectDeviceGroupById(groupId));
    }

    /**
     * 新增分组
     */
    @PreAuthorize("@ss.hasPermi('iot:group:add')")
    @Log(title = "设备分组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DeviceGroup deviceGroup)
    {
        deviceGroup.setCreateBy(getUsername());
        return toAjax(deviceGroupService.insertDeviceGroup(deviceGroup));
    }

    /**
     * 修改分组
     */
    @PreAuthorize("@ss.hasPermi('iot:group:edit')")
    @Log(title = "设备分组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DeviceGroup deviceGroup)
    {
        deviceGroup.setUpdateBy(getUsername());
        return toAjax(deviceGroupService.updateDeviceGroup(deviceGroup));
    }

    /**
     * 删除分组
     */
    @PreAuthorize("@ss.hasPermi('iot:group:remove')")
    @Log(title = "设备分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{groupIds}")
    public AjaxResult remove(@PathVariable Long[] groupIds)
    {
        return toAjax(deviceGroupService.deleteDeviceGroupByIds(groupIds));
    }

    /**
     * 添加设备到分组
     */
    @PreAuthorize("@ss.hasPermi('iot:group:edit')")
    @Log(title = "设备分组", businessType = BusinessType.UPDATE)
    @PostMapping("/device/{groupId}/{deviceId}")
    public AjaxResult addDevice(@PathVariable("groupId") Long groupId, @PathVariable("deviceId") Long deviceId)
    {
        return toAjax(deviceGroupService.addDeviceToGroup(deviceId, groupId));
    }

    /**
     * 从分组移除设备
     */
    @PreAuthorize("@ss.hasPermi('iot:group:edit')")
    @Log(title = "设备分组", businessType = BusinessType.UPDATE)
    @DeleteMapping("/device/{groupId}/{deviceId}")
    public AjaxResult removeDevice(@PathVariable("groupId") Long groupId, @PathVariable("deviceId") Long deviceId)
    {
        return toAjax(deviceGroupService.removeDeviceFromGroup(deviceId, groupId));
    }

    /**
     * 查询分组下所有设备ID
     */
    @PreAuthorize("@ss.hasPermi('iot:group:query')")
    @GetMapping("/devices/{groupId}")
    public AjaxResult getGroupDevices(@PathVariable("groupId") Long groupId)
    {
        return success(deviceGroupService.getDeviceIdsByGroupId(groupId));
    }

    /**
     * 查询设备所属分组
     */
    @PreAuthorize("@ss.hasPermi('iot:group:query')")
    @GetMapping("/device/{deviceId}/groups")
    public AjaxResult getDeviceGroups(@PathVariable("deviceId") Long deviceId)
    {
        return success(deviceGroupService.getGroupsByDeviceId(deviceId));
    }
}
