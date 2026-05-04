package com.ruoyi.iot.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.iot.domain.entity.IoTDevice;
import com.ruoyi.iot.service.IIoTDeviceService;

/**
 * IoT 设备管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/iot/device")
public class IoTDeviceController extends BaseController
{
    @Autowired
    private IIoTDeviceService deviceService;

    /**
     * 查询设备列表
     */
    @PreAuthorize("@ss.hasPermi('iot:device:list')")
    @GetMapping("/list")
    public TableDataInfo list(IoTDevice iotDevice)
    {
        startPage();
        List<IoTDevice> list = deviceService.selectDeviceList(iotDevice);
        return getDataTable(list);
    }

    /**
     * 导出设备列表
     */
    @PreAuthorize("@ss.hasPermi('iot:device:export')")
    @Log(title = "设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, IoTDevice iotDevice)
    {
        List<IoTDevice> list = deviceService.selectDeviceList(iotDevice);
        ExcelUtil<IoTDevice> util = new ExcelUtil<>(IoTDevice.class);
        util.exportExcel(response, list, "设备数据");
    }

    /**
     * 获取设备详情
     */
    @PreAuthorize("@ss.hasPermi('iot:device:query')")
    @GetMapping(value = "/{deviceId}")
    public AjaxResult getInfo(@PathVariable("deviceId") Long deviceId)
    {
        return success(deviceService.selectDeviceById(deviceId));
    }

    /**
     * 根据设备编号获取详情
     */
    @PreAuthorize("@ss.hasPermi('iot:device:query')")
    @GetMapping(value = "/code/{deviceCode}")
    public AjaxResult getInfoByCode(@PathVariable("deviceCode") String deviceCode)
    {
        return success(deviceService.selectDeviceByCode(deviceCode));
    }

    /**
     * 新增设备
     */
    @PreAuthorize("@ss.hasPermi('iot:device:add')")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody IoTDevice iotDevice)
    {
        iotDevice.setCreateBy(getUsername());
        return toAjax(deviceService.insertDevice(iotDevice));
    }

    /**
     * 修改设备
     */
    @PreAuthorize("@ss.hasPermi('iot:device:edit')")
    @Log(title = "设备", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody IoTDevice iotDevice)
    {
        iotDevice.setUpdateBy(getUsername());
        return toAjax(deviceService.updateDevice(iotDevice));
    }

    /**
     * 删除设备（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('iot:device:remove')")
    @Log(title = "设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable Long[] deviceIds)
    {
        return toAjax(deviceService.deleteDeviceByIds(deviceIds));
    }

    /**
     * 批量更新设备状态
     */
    @PreAuthorize("@ss.hasPermi('iot:device:edit')")
    @Log(title = "设备", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{deviceId}")
    public AjaxResult changeStatus(@PathVariable("deviceId") Long deviceId, @RequestBody IoTDevice iotDevice)
    {
        return toAjax(deviceService.updateDeviceStatus(deviceId, iotDevice.getStatus()));
    }

    /**
     * 查询厂商下设备数量
     */
    @PreAuthorize("@ss.hasPermi('iot:device:query')")
    @GetMapping(value = "/count/{manufacturerId}")
    public AjaxResult countByManufacturer(@PathVariable("manufacturerId") Long manufacturerId)
    {
        return success(deviceService.countByManufacturerId(manufacturerId));
    }
}
