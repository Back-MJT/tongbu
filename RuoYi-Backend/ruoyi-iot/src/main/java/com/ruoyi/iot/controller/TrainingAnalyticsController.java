package com.ruoyi.iot.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 训练数据分析 TrainingAnalytics
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/iot/training")
public class TrainingAnalyticsController extends BaseController
{
    /**
     * 查询训练完成率统计
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/completion-rates")
    public AjaxResult getCompletionRates(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "period", defaultValue = "7d") String period)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overall_rate", 73.2);
        data.put("period", period);

        List<Map<String, Object>> byWeek = new ArrayList<>();
        String[] weeks = {"2026-W14", "2026-W13", "2026-W12", "2026-W11"};
        double[] rates = {73.2, 70.5, 68.1, 71.8};
        for (int i = 0; i < weeks.length; i++)
        {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("week", weeks[i]);
            w.put("rate", rates[i]);
            w.put("total_users", 8000 + (int) (Math.random() * 2000));
            w.put("completed_users", (int) (8000 * rates[i] / 100));
            byWeek.add(w);
        }
        data.put("weekly_breakdown", byWeek);
        return success(data);
    }

    /**
     * 查询AI训练计划采纳率指标
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/ai-plan-adoption")
    public AjaxResult getAiPlanAdoption(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "period", defaultValue = "30d") String period)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period);
        data.put("total_plans_generated", 15420);
        data.put("plans_adopted", 9859);
        data.put("adoption_rate", 63.9);
        data.put("plans_completed", 6890);
        data.put("completion_rate_for_adopted", 69.9);

        List<Map<String, Object>> byStage = new ArrayList<>();
        String[] stages = {"BEGINNER", "INTERMEDIATE", "ADVANCED"};
        double[] adoptionRates = {72.1, 61.3, 55.8};
        for (int i = 0; i < stages.length; i++)
        {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("stage", stages[i]);
            s.put("adoption_rate", adoptionRates[i]);
            byStage.add(s);
        }
        data.put("by_stage", byStage);
        return success(data);
    }

    /**
     * 查询用户参与度指标（日活、平均会话时长、留存率）
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/user-engagement")
    public AjaxResult getUserEngagement(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "period", defaultValue = "7d") String period)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period);
        data.put("daily_active_users", 4215);
        data.put("monthly_active_users", 12836);
        data.put("avg_session_duration_min", 32.5);
        data.put("day1_retention", 85.3);
        data.put("day7_retention", 62.1);
        data.put("day30_retention", 41.7);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--)
        {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("date", "2026-04-" + String.format("%02d", 15 - i));
            d.put("dau", 3800 + (int) (Math.random() * 800));
            d.put("avg_duration", 28.0 + Math.random() * 10);
            trend.add(d);
        }
        data.put("trend", trend);
        return success(data);
    }

    /**
     * 查询运动类型分布
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/exercise-distribution")
    public AjaxResult getExerciseDistribution(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "period", defaultValue = "30d") String period)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period);

        List<Map<String, Object>> distribution = new ArrayList<>();
        String[][] exercises = {
                {"步行", "35.2", "4520"},
                {"跑步", "22.8", "2930"},
                {"骑行", "15.1", "1940"},
                {"游泳", "10.3", "1325"},
                {"瑜伽", "8.7", "1120"},
                {"力量训练", "5.2", "670"},
                {"其他", "2.7", "350"}
        };
        for (String[] e : exercises)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("exercise_type", e[0]);
            item.put("percentage", Double.parseDouble(e[1]));
            item.put("count", Integer.parseInt(e[2]));
            distribution.add(item);
        }
        data.put("distribution", distribution);
        return success(data);
    }

    /**
     * 查询厂商训练指标对比
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/manufacturer-comparison")
    public AjaxResult getManufacturerComparison(
            @RequestParam(value = "period", defaultValue = "30d") String period)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period);

        List<Map<String, Object>> manufacturers = new ArrayList<>();
        String[][] mfrs = {
                {"芯动科技", "78.5", "65.2", "4120", "33.8"},
                {"华为运动健康", "82.1", "70.8", "8930", "35.2"},
                {"小米手环", "71.3", "58.6", "12350", "29.5"},
                {"佳明", "85.7", "74.3", "2100", "42.1"}
        };
        for (String[] m : mfrs)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("manufacturer_name", m[0]);
            item.put("completion_rate", Double.parseDouble(m[1]));
            item.put("ai_adoption_rate", Double.parseDouble(m[2]));
            item.put("active_users", Integer.parseInt(m[3]));
            item.put("avg_session_min", Double.parseDouble(m[4]));
            manufacturers.add(item);
        }
        data.put("manufacturers", manufacturers);
        return success(data);
    }

    /**
     * 查询实时训练状态
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/realtime-status")
    public AjaxResult getRealtimeStatus(@RequestParam(required = false) Long manufacturerId)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", "2026-04-15T20:53:00");
        data.put("active_sessions", 342);
        data.put("online_devices", 1893);
        data.put("total_users_today", 4215);

        List<Map<String, Object>> activeList = new ArrayList<>();
        String[] stages = {"BEGINNER", "INTERMEDIATE", "ADVANCED"};
        String[] types = {"步行", "跑步", "骑行", "力量训练"};
        for (int i = 0; i < 5; i++)
        {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("session_id", "SS-" + (1000 + i));
            session.put("user_id", "U-" + (2000 + i));
            session.put("device_id", "D-" + (3000 + i));
            session.put("exercise_type", types[i % types.length]);
            session.put("stage", stages[i % stages.length]);
            session.put("duration_min", 10 + i * 5);
            session.put("heart_rate", 90 + i * 10);
            session.put("started_at", "2026-04-15T20:" + String.format("%02d", 30 + i) + ":00");
            activeList.add(session);
        }
        data.put("active_sessions_detail", activeList);
        return success(data);
    }

    /**
     * 查询训练趋势（按设备/厂商）
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/trends")
    public AjaxResult getTrainingTrends(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "period", defaultValue = "30d") String period,
            @RequestParam(value = "metric", defaultValue = "completion_rate") String metric)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("period", period);
        data.put("metric", metric);

        int points = "7d".equals(period) ? 7 : "90d".equals(period) ? 12 : 30;
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < points; i++)
        {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", "2026-04-" + String.format("%02d", 15 - points + i + 1));
            switch (metric)
            {
                case "active_users":
                    point.put("value", 3800 + (int) (Math.random() * 800));
                    break;
                case "session_duration":
                    point.put("value", 28.0 + Math.random() * 10);
                    break;
                default:
                    point.put("value", 65.0 + Math.random() * 15);
                    break;
            }
            series.add(point);
        }
        data.put("series", series);
        return success(data);
    }

    /**
     * 查询每日训练汇总
     */
    @PreAuthorize("@ss.hasPermi('iot:training:query')")
    @GetMapping("/daily-summary")
    public AjaxResult getDailySummary(
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "date", required = false) String date)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", date != null ? date : "2026-04-15");
        data.put("total_sessions", 1523);
        data.put("total_users", 892);
        data.put("total_duration_min", 48560);
        data.put("avg_session_duration_min", 31.9);
        data.put("completion_rate", 74.5);
        data.put("calories_burned", 285400);
        data.put("top_exercise", "步行");
        data.put("ai_plans_delivered", 312);
        data.put("ai_plans_completed", 218);
        return success(data);
    }

    /**
     * 导出训练分析报告
     */
    @PreAuthorize("@ss.hasPermi('iot:training:export')")
    @PostMapping("/export")
    public void exportTrainingReport(
            HttpServletResponse response,
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(value = "period", defaultValue = "30d") String period)
    {
        logger.info("Exporting training report: manufacturerId={}, period={}", manufacturerId, period);
        // Mock export - in production this would use ExcelUtil
        try
        {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=training_report_" + period + ".xlsx");
            response.getWriter().write("Mock training report data");
            response.getWriter().flush();
        }
        catch (Exception e)
        {
            logger.error("Export failed", e);
        }
    }
}
