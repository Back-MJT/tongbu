package com.ruoyi.iot.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * IoT系统配置 ConfigController
 * 包含 MQTT Topic配置、告警规则配置、数据保留策略
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/iot/config")
public class ConfigController extends BaseController
{
    // ========== MQTT Topic 配置 ==========

    /**
     * 查询MQTT Topic列表
     */
    @PreAuthorize("@ss.hasPermi('iot:config:query')")
    @GetMapping("/mqtt/topic/list")
    public TableDataInfo listMqttTopic(@RequestParam(required = false) String topicName)
    {
        startPage();
        List<Map<String, Object>> topics = new ArrayList<>();

        String[][] mockTopics = {
                {"1", "xindong/imu/+/data", "IMU数据上报", "1", "pub_sub", "1", "2026-01-15 10:00:00"},
                {"2", "xindong/heart_rate/+/data", "心率数据上报", "1", "subscribe", "1", "2026-01-15 10:05:00"},
                {"3", "xindong/sleep/+/data", "睡眠数据上报", "1", "subscribe", "1", "2026-01-16 08:30:00"},
                {"4", "xindong/device/+/status", "设备状态上报", "2", "pub_sub", "1", "2026-02-01 14:00:00"},
                {"5", "xindong/cmd/+/config", "设备配置下发", "2", "publish", "1", "2026-02-10 09:00:00"}
        };

        for (String[] t : mockTopics)
        {
            Map<String, Object> topic = new LinkedHashMap<>();
            topic.put("topicId", Long.valueOf(t[0]));
            topic.put("topicName", t[1]);
            topic.put("description", t[2]);
            topic.put("manufacturerId", Long.valueOf(t[3]));
            topic.put("permission", t[4]);
            topic.put("status", "1".equals(t[5]) ? "0" : "1");
            topic.put("createTime", t[6]);
            topics.add(topic);
        }
        return getDataTable(topics);
    }

    /**
     * 获取MQTT Topic详情
     */
    @PreAuthorize("@ss.hasPermi('iot:config:query')")
    @GetMapping("/mqtt/topic/{topicId}")
    public AjaxResult getMqttTopic(@PathVariable("topicId") Long topicId)
    {
        Map<String, Object> topic = new LinkedHashMap<>();
        topic.put("topicId", topicId);
        topic.put("topicName", "xindong/imu/+/data");
        topic.put("description", "IMU数据上报Topic");
        topic.put("manufacturerId", 1);
        topic.put("permission", "pub_sub");
        topic.put("qos", 1);
        topic.put("retained", false);
        topic.put("status", "0");
        topic.put("createBy", "admin");
        topic.put("createTime", "2026-01-15 10:00:00");
        return success(topic);
    }

    /**
     * 新增MQTT Topic
     */
    @PreAuthorize("@ss.hasPermi('iot:config:add')")
    @PostMapping("/mqtt/topic")
    public AjaxResult addMqttTopic(@RequestBody Map<String, Object> data)
    {
        logger.info("Add MQTT topic: {}", data.get("topicName"));
        return success();
    }

    /**
     * 修改MQTT Topic
     */
    @PreAuthorize("@ss.hasPermi('iot:config:edit')")
    @PutMapping("/mqtt/topic")
    public AjaxResult updateMqttTopic(@RequestBody Map<String, Object> data)
    {
        logger.info("Update MQTT topic: {}", data.get("topicId"));
        return success();
    }

    /**
     * 删除MQTT Topic
     */
    @PreAuthorize("@ss.hasPermi('iot:config:remove')")
    @org.springframework.web.bind.annotation.DeleteMapping("/mqtt/topic/{topicId}")
    public AjaxResult delMqttTopic(@PathVariable Long[] topicId)
    {
        logger.info("Delete MQTT topic: {}", (Object) topicId);
        return success();
    }

    // ========== 告警规则配置 ==========

    /**
     * 查询告警规则列表
     */
    @PreAuthorize("@ss.hasPermi('iot:config:query')")
    @GetMapping("/alert/rule/list")
    public TableDataInfo listAlertRule(@RequestParam(required = false) String ruleName)
    {
        startPage();
        List<Map<String, Object>> rules = new ArrayList<>();

        String[][] mockRules = {
                {"1", "心率异常告警", "heart_rate > 180 OR heart_rate < 40", "HIGH", "1", "1", "2026-01-20 10:00:00"},
                {"2", "设备离线告警", "offline_duration > 30min", "HIGH", "1", "1", "2026-01-20 10:30:00"},
                {"3", "睡眠数据缺失", "sleep_data_gap > 60min", "MEDIUM", "1", "0", "2026-02-05 14:00:00"},
                {"4", "电池低电量", "battery_level < 20", "LOW", "2", "1", "2026-02-10 09:00:00"},
                {"5", "训练合规率低", "compliance_rate < 50% for 7d", "MEDIUM", "2", "1", "2026-02-15 11:00:00"}
        };

        for (String[] r : mockRules)
        {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("ruleId", Long.valueOf(r[0]));
            rule.put("ruleName", r[1]);
            rule.put("condition", r[2]);
            rule.put("severity", r[3]);
            rule.put("manufacturerId", Long.valueOf(r[4]));
            rule.put("enabled", "1".equals(r[5]));
            rule.put("createTime", r[6]);
            rules.add(rule);
        }
        return getDataTable(rules);
    }

    /**
     * 获取告警规则详情
     */
    @PreAuthorize("@ss.hasPermi('iot:config:query')")
    @GetMapping("/alert/rule/{ruleId}")
    public AjaxResult getAlertRule(@PathVariable("ruleId") Long ruleId)
    {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("ruleId", ruleId);
        rule.put("ruleName", "心率异常告警");
        rule.put("condition", "heart_rate > 180 OR heart_rate < 40");
        rule.put("severity", "HIGH");
        rule.put("manufacturerId", 1);
        rule.put("enabled", true);
        rule.put("notifyChannels", "[\"email\", \"sms\"]");
        rule.put("cooldownMin", 5);
        rule.put("createBy", "admin");
        rule.put("createTime", "2026-01-20 10:00:00");
        return success(rule);
    }

    /**
     * 新增告警规则
     */
    @PreAuthorize("@ss.hasPermi('iot:config:add')")
    @PostMapping("/alert/rule")
    public AjaxResult addAlertRule(@RequestBody Map<String, Object> data)
    {
        logger.info("Add alert rule: {}", data.get("ruleName"));
        return success();
    }

    /**
     * 修改告警规则
     */
    @PreAuthorize("@ss.hasPermi('iot:config:edit')")
    @PutMapping("/alert/rule")
    public AjaxResult updateAlertRule(@RequestBody Map<String, Object> data)
    {
        logger.info("Update alert rule: {}", data.get("ruleId"));
        return success();
    }

    /**
     * 删除告警规则
     */
    @PreAuthorize("@ss.hasPermi('iot:config:remove')")
    @org.springframework.web.bind.annotation.DeleteMapping("/alert/rule/{ruleId}")
    public AjaxResult delAlertRule(@PathVariable Long[] ruleId)
    {
        logger.info("Delete alert rule: {}", (Object) ruleId);
        return success();
    }

    /**
     * 切换告警规则启用/禁用
     */
    @PreAuthorize("@ss.hasPermi('iot:config:edit')")
    @PutMapping("/alert/rule/{ruleId}/toggle")
    public AjaxResult toggleAlertRule(
            @PathVariable("ruleId") Long ruleId,
            @RequestBody Map<String, Object> data)
    {
        logger.info("Toggle alert rule {}: enabled={}", ruleId, data.get("enabled"));
        return success();
    }

    // ========== 数据保留策略 ==========

    /**
     * 查询数据保留策略列表
     */
    @PreAuthorize("@ss.hasPermi('iot:config:query')")
    @GetMapping("/retention/list")
    public TableDataInfo listDataRetention(@RequestParam(required = false) String policyName)
    {
        startPage();
        List<Map<String, Object>> policies = new ArrayList<>();

        String[][] mockPolicies = {
                {"1", "IMU原始数据保留", "imu_raw", "30", "自动清理", "1", "2026-01-15 10:00:00", "2026-03-01 02:00:00"},
                {"2", "心率数据保留", "heart_rate", "90", "自动清理", "1", "2026-01-15 10:05:00", "2026-03-01 02:00:00"},
                {"3", "睡眠汇总数据保留", "sleep_summary", "365", "自动清理", "1", "2026-01-15 10:10:00", "2026-04-01 02:00:00"},
                {"4", "训练计划数据保留", "training_plan", "730", "无", "1", "2026-02-01 09:00:00", null},
                {"5", "设备日志保留", "device_log", "60", "自动清理", "2", "2026-02-10 14:00:00", "2026-04-01 02:00:00"}
        };

        for (String[] p : mockPolicies)
        {
            Map<String, Object> policy = new LinkedHashMap<>();
            policy.put("id", Long.valueOf(p[0]));
            policy.put("policyName", p[1]);
            policy.put("dataType", p[2]);
            policy.put("retentionDays", Integer.parseInt(p[3]));
            policy.put("action", p[4]);
            policy.put("manufacturerId", Long.valueOf(p[5]));
            policy.put("createTime", p[6]);
            policy.put("lastExecuted", p[7]);
            policies.add(policy);
        }
        return getDataTable(policies);
    }

    /**
     * 获取数据保留策略详情
     */
    @PreAuthorize("@ss.hasPermi('iot:config:query')")
    @GetMapping("/retention/{id}")
    public AjaxResult getDataRetention(@PathVariable("id") Long id)
    {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("id", id);
        policy.put("policyName", "IMU原始数据保留");
        policy.put("dataType", "imu_raw");
        policy.put("retentionDays", 30);
        policy.put("action", "自动清理");
        policy.put("manufacturerId", 1);
        policy.put("cronExpression", "0 0 2 * * ?");
        policy.put("createBy", "admin");
        policy.put("createTime", "2026-01-15 10:00:00");
        policy.put("lastExecuted", "2026-03-01 02:00:00");
        policy.put("recordsProcessed", 1250000);
        return success(policy);
    }

    /**
     * 修改数据保留策略
     */
    @PreAuthorize("@ss.hasPermi('iot:config:edit')")
    @PutMapping("/retention")
    public AjaxResult updateDataRetention(@RequestBody Map<String, Object> data)
    {
        logger.info("Update data retention policy: {}", data.get("id"));
        return success();
    }

    /**
     * 手动执行数据保留策略
     */
    @PreAuthorize("@ss.hasPermi('iot:config:edit')")
    @PostMapping("/retention/{id}/execute")
    public AjaxResult executeDataRetention(@PathVariable("id") Long id)
    {
        logger.info("Execute data retention policy id={}", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policyId", id);
        result.put("status", "RUNNING");
        result.put("message", "数据清理任务已触发");
        return success(result);
    }
}
