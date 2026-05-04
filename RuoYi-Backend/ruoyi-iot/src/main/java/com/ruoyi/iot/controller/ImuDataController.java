package com.ruoyi.iot.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.iot.domain.model.ImuData;
import com.ruoyi.iot.domain.entity.ImuDataRecord;
import com.ruoyi.iot.mqtt.MqttListener;
import com.ruoyi.iot.service.IImuDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * IMU 数据查询 API
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/iot/imu")
public class ImuDataController extends BaseController
{
    @Autowired
    private MqttListener mqttListener;

    @Autowired
    private IImuDataService imuDataService;

    // ---- 实时数据 (Redis缓存) ----

    /**
     * 获取设备最新IMU数据（从Redis缓存）
     */
    @PreAuthorize("@ss.hasPermi('iot:imu:query')")
    @GetMapping(value = "/latest/{deviceCode}")
    public AjaxResult getLatest(@PathVariable("deviceCode") String deviceCode)
    {
        ImuData imuData = mqttListener.getLatestImuData(deviceCode);
        if (imuData != null)
        {
            return success(imuData);
        }
        return error("设备暂无数据或已离线");
    }

    /**
     * 获取设备缓存状态（Redis）
     */
    @PreAuthorize("@ss.hasPermi('iot:imu:query')")
    @GetMapping(value = "/status/{deviceCode}")
    public AjaxResult getStatus(@PathVariable("deviceCode") String deviceCode)
    {
        String status = mqttListener.getCachedDeviceStatus(deviceCode);
        if (status != null)
        {
            return success(status);
        }
        return success("offline");
    }

    /**
     * MQTT连接状态检查
     */
    @PreAuthorize("@ss.hasPermi('iot:imu:query')")
    @GetMapping(value = "/mqtt/health")
    public AjaxResult mqttHealth()
    {
        return success(mqttListener.isConnected());
    }

    // ---- 历史数据 (PostgreSQL/TimescaleDB) ----

    /**
     * 查询设备最近N条IMU历史记录
     *
     * @param deviceCode 设备编号
     * @param limit      返回条数，默认100，上限1000
     */
    @PreAuthorize("@ss.hasPermi('iot:imu:query')")
    @GetMapping(value = "/history/{deviceCode}")
    public AjaxResult getHistory(
        @PathVariable("deviceCode") String deviceCode,
        @RequestParam(value = "limit", defaultValue = "100") int limit)
    {
        if (limit <= 0 || limit > 1000)
        {
            limit = 100;
        }
        List<ImuDataRecord> records = imuDataService.selectLatestByDeviceCode(deviceCode, limit);
        return success(records);
    }

    /**
     * 按时间范围查询设备IMU历史数据
     *
     * @param deviceCode 设备编号
     * @param beginTime  开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTime    结束时间 (yyyy-MM-dd HH:mm:ss)
     */
    @PreAuthorize("@ss.hasPermi('iot:imu:query')")
    @GetMapping(value = "/history/{deviceCode}/range")
    public AjaxResult getHistoryByTimeRange(
        @PathVariable("deviceCode") String deviceCode,
        @RequestParam("beginTime")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,
        @RequestParam("endTime")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime)
    {
        if (beginTime == null || endTime == null)
        {
            return error("beginTime 和 endTime 不能为空");
        }
        if (beginTime.after(endTime))
        {
            return error("beginTime 必须早于 endTime");
        }
        // 时间范围限制：最多查7天
        long diffMs = endTime.getTime() - beginTime.getTime();
        if (diffMs > 7 * 24 * 3600 * 1000L)
        {
            return error("时间范围不能超过7天");
        }
        List<ImuDataRecord> records = imuDataService.selectByDeviceCodeAndTimeRange(deviceCode, beginTime, endTime);
        return success(records);
    }

    /**
     * 查询设备IMU数据统计
     *
     * @param deviceCode 设备编号
     * @param beginTime  开始时间 (yyyy-MM-dd HH:mm:ss)
     * @param endTime    结束时间 (yyyy-MM-dd HH:mm:ss)
     */
    @PreAuthorize("@ss.hasPermi('iot:imu:query')")
    @GetMapping(value = "/history/{deviceCode}/stats")
    public AjaxResult getHistoryStats(
        @PathVariable("deviceCode") String deviceCode,
        @RequestParam("beginTime")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,
        @RequestParam("endTime")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime)
    {
        if (beginTime == null || endTime == null || beginTime.after(endTime))
        {
            return error("参数错误");
        }
        long count = imuDataService.countByDeviceCodeAndTimeRange(deviceCode, beginTime, endTime);
        return success(count);
    }
}
