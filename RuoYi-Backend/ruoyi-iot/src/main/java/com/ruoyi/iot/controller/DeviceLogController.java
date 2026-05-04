package com.ruoyi.iot.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.iot.domain.entity.DeviceLog;
import com.ruoyi.iot.service.IDeviceLogService;

/**
 * IoT 设备日志管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/iot/log")
public class DeviceLogController extends BaseController
{
    @Autowired
    private IDeviceLogService deviceLogService;

    /**
     * 查询日志列表
     */
    @PreAuthorize("@ss.hasPermi('iot:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(DeviceLog deviceLog)
    {
        startPage();
        List<DeviceLog> list = deviceLogService.selectDeviceLogList(deviceLog);
        return getDataTable(list);
    }

    /**
     * 导出日志列表
     */
    @PreAuthorize("@ss.hasPermi('iot:log:export')")
    @Log(title = "设备日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceLog deviceLog)
    {
        List<DeviceLog> list = deviceLogService.selectDeviceLogList(deviceLog);
        ExcelUtil<DeviceLog> util = new ExcelUtil<>(DeviceLog.class);
        util.exportExcel(response, list, "设备日志");
    }

    /**
     * 查询设备最新日志
     */
    @PreAuthorize("@ss.hasPermi('iot:log:query')")
    @GetMapping(value = "/latest/{deviceId}/{limit}")
    public AjaxResult latest(@PathVariable("deviceId") Long deviceId, @PathVariable("limit") int limit)
    {
        return success(deviceLogService.selectLatestByDeviceId(deviceId, limit));
    }

    /**
     * 删除日志
     */
    @PreAuthorize("@ss.hasPermi('iot:log:remove')")
    @Log(title = "设备日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(deviceLogService.deleteDeviceLogByIds(logIds));
    }

    /**
     * 清空设备日志
     */
    @PreAuthorize("@ss.hasPermi('iot:log:remove')")
    @Log(title = "设备日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean/{deviceId}")
    public AjaxResult clean(@PathVariable("deviceId") Long deviceId)
    {
        return toAjax(deviceLogService.deleteByDeviceId(deviceId));
    }
}
