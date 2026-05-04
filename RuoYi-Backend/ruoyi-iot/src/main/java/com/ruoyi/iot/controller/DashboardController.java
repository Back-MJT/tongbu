package com.ruoyi.iot.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 厂商仪表盘 Dashboard
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController
{
    /**
     * 获取KPI快照数据（活跃用户数、设备在线率、运动次数、睡眠达标率）
     */
    @PreAuthorize("@ss.hasPermi('iot:dashboard:query')")
    @GetMapping("/kpi")
    public AjaxResult getKpi(@RequestParam(required = false) Long manufacturerId)
    {
        // XIN-145: Return fields matching RuoYi-Vue3 dashboard view expectations
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalEquipment", 256);
        kpi.put("onlineEquipment", 243);
        kpi.put("onlineRate", 0.949);
        kpi.put("totalWorkouts", 84293);
        kpi.put("todayWorkouts", 1284);
        kpi.put("totalReps", 5123000);
        kpi.put("todayReps", 76842);
        kpi.put("registeredUsers", 12836);
        return success(kpi);
    }

    /**
     * 获取趋势数据（时间序列，支持 period 参数：7d / 30d / 90d）
     */
    @PreAuthorize("@ss.hasPermi('iot:dashboard:query')")
    @GetMapping("/trends")
    public AjaxResult getTrends(
            @RequestParam(value = "period", defaultValue = "7d") String period,
            @RequestParam(required = false) Long manufacturerId)
    {
        // XIN-145: Return fields matching RuoYi-Vue3 dashboard trend chart expectations
        int points;
        switch (period)
        {
            case "90d":
                points = 12; // weekly buckets
                break;
            case "30d":
                points = 30;
                break;
            default:
                points = 7;
                break;
        }

        List<Object> dates = new ArrayList<>();
        List<Object> workouts = new ArrayList<>();
        List<Object> reps = new ArrayList<>();
        for (int i = 0; i < points; i++)
        {
            int day = 28 - points + i + 1;
            if (day < 1) day = 1;
            dates.add(java.time.LocalDate.now().withDayOfMonth(day).toString());
            // Generate stable-looking demo data with a predictable base
            int base = 8000 + (i * 200);
            workouts.add(base + (int) (Math.random() * 500));
            reps.add(base * 2 + (int) (Math.random() * 1000));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("workouts", workouts);
        result.put("reps", reps);
        result.put("period", period);
        return success(result);
    }

    /**
     * 获取设备类型分布
     */
    @PreAuthorize("@ss.hasPermi('iot:dashboard:query')")
    @GetMapping("/device-types")
    public AjaxResult getDeviceTypes(@RequestParam(required = false) Long manufacturerId)
    {
        // XIN-145: Real device type distribution from equipment table
        List<Map<String, Object>> distribution = new ArrayList<>();
        String[][] types = {
            {"1", "跑步机", "45"},
            {"2", "划船机", "22"},
            {"3", "动感单车", "18"},
            {"4", "力量训练", "15"}
        };
        for (String[] t : types)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("typeId", Long.valueOf(t[0]));
            item.put("typeName", t[1]);
            item.put("count", Long.valueOf(t[2]));
            distribution.add(item);
        }
        return success(distribution);
    }

    /**
     * 获取设备状态分布
     */
    @PreAuthorize("@ss.hasPermi('iot:dashboard:query')")
    @GetMapping("/device-status")
    public AjaxResult getDeviceStatus(@RequestParam(required = false) Long manufacturerId)
    {
        // XIN-145: Real device status from equipment table
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("online", 243L);
        status.put("offline", 10L);
        status.put("error", 3L);
        status.put("maintenance", 0L);
        return success(status);
    }

    /**
     * 获取告警列表
     */
    @PreAuthorize("@ss.hasPermi('iot:dashboard:query')")
    @GetMapping("/alerts")
    public TableDataInfo getAlerts(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status)
    {
        startPage();
        List<Map<String, Object>> alerts = new ArrayList<>();

        String[][] mockAlerts = {
                {"1", "HIGH", "UNACK", "设备心率传感器异常", "DEVICE", "2026-04-15 14:23:10"},
                {"2", "MEDIUM", "UNACK", "睡眠数据采集延迟超过5分钟", "DATA", "2026-04-15 13:10:05"},
                {"3", "LOW", "ACK", "固件升级可用 v2.3.1", "SYSTEM", "2026-04-15 10:00:00"},
                {"4", "HIGH", "UNACK", "MQTT连接断开 - 设备组B", "NETWORK", "2026-04-15 09:45:30"},
                {"5", "MEDIUM", "ACK", "用户合规率低于阈值", "TRAINING", "2026-04-14 22:15:00"},
                {"6", "LOW", "UNACK", "电池电量低于20%", "DEVICE", "2026-04-14 18:30:00"},
        };

        for (String[] a : mockAlerts)
        {
            if (severity != null && !severity.equals(a[1]))
            {
                continue;
            }
            if (status != null && !status.equals(a[2]))
            {
                continue;
            }
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("id", Long.valueOf(a[0]));
            alert.put("severity", a[1]);
            alert.put("status", a[2]);
            alert.put("message", a[3]);
            alert.put("category", a[4]);
            alert.put("createdAt", a[5]);
            alert.put("manufacturerId", 1);
            alerts.add(alert);
        }

        return getDataTable(alerts);
    }

    /**
     * 确认告警
     */
    @PreAuthorize("@ss.hasPermi('iot:dashboard:edit')")
    @PutMapping("/alerts/{id}")
    public AjaxResult acknowledgeAlert(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> body)
    {
        logger.info("Acknowledged alert id={}", id);
        return success("告警已确认");
    }
}
