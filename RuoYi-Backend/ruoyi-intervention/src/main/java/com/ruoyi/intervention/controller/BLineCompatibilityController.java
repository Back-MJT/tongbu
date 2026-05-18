package com.ruoyi.intervention.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;

/**
 * B Line compatibility API hosted inside RuoYi.
 *
 * <p>Older mini-program code used intervention-engine style paths
 * (/api/profiles, /api/prescriptions/*, /api/integration/data). Those paths now
 * terminate in RuoYi so B Line has a single backend entrypoint while the A Line
 * body-engine contract remains reserved.
 */
@RestController
@RequestMapping("/api")
public class BLineCompatibilityController
{
    @GetMapping("/profiles/{userId}")
    public AjaxResult getProfile(@PathVariable String userId)
    {
        return AjaxResult.success(buildProfile(userId, null));
    }

    @PostMapping("/profiles")
    public AjaxResult createProfile(@RequestBody Map<String, Object> body)
    {
        String userId = stringValue(body.get("userId"), stringValue(body.get("user_id"), "mp-user"));
        return AjaxResult.success("健康档案已接收", buildProfile(userId, body));
    }

    @PutMapping("/profiles/{profileId}")
    public AjaxResult updateProfile(@PathVariable String profileId, @RequestBody Map<String, Object> body)
    {
        Map<String, Object> profile = buildProfile(stringValue(body.get("userId"), profileId), body);
        profile.put("profileId", profileId);
        profile.put("updated", true);
        return AjaxResult.success("健康档案已更新", profile);
    }

    @PostMapping("/prescriptions/exercise")
    public AjaxResult generateExercisePrescription(@RequestBody Map<String, Object> body)
    {
        String userId = stringValue(body.get("userId"), stringValue(body.get("user_id"), "mp-user"));
        String exerciseType = stringValue(body.get("exerciseType"), stringValue(body.get("exercise_type"), "strength"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prescriptionId", "BL-RX-" + System.currentTimeMillis());
        result.put("userId", userId);
        result.put("type", "exercise");
        result.put("exerciseType", exerciseType);
        result.put("status", "generated");
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("recommendations", List.of(
                recommendation(1, "完成今日器械训练", "3组 x 10-12次，保持动作节奏稳定。", "B Line 本地规则处方"),
                recommendation(2, "训练后记录主观反馈", "记录疲劳、疼痛和完成难度，作为后续身体引擎输入。", "反馈闭环"),
                recommendation(3, "保持安全优先", "如出现疼痛或头晕，立即停止并联系教练。", "安全约束")));
        result.put("source", "ruoyi_b_line");
        result.put("alineStatus", "reserved");
        return AjaxResult.success(result);
    }

    @PostMapping("/prescriptions/sleep")
    public AjaxResult generateSleepPrescription(@RequestBody Map<String, Object> body)
    {
        String userId = stringValue(body.get("userId"), stringValue(body.get("user_id"), "mp-user"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prescriptionId", "BL-SLEEP-" + System.currentTimeMillis());
        result.put("userId", userId);
        result.put("type", "sleep");
        result.put("status", "generated");
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("recommendations", List.of(
                "睡前30分钟降低屏幕刺激。",
                "训练日晚间补充轻量拉伸和呼吸放松。",
                "连续记录7天睡眠与疲劳评分，等待 A Line 正式评估。"));
        result.put("source", "ruoyi_b_line");
        return AjaxResult.success(result);
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    public AjaxResult getPrescription(@PathVariable String prescriptionId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prescriptionId", prescriptionId);
        result.put("status", "available");
        result.put("source", "ruoyi_b_line");
        result.put("date", LocalDate.now().toString());
        return AjaxResult.success(result);
    }

    @PostMapping("/integration/data")
    public AjaxResult ingestIntegrationData(@RequestBody Map<String, Object> body)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("source", "ruoyi_b_line");
        result.put("receivedAt", LocalDateTime.now().toString());
        result.put("deviceId", firstPresent(body, "deviceId", "device_id"));
        result.put("userId", firstPresent(body, "userId", "user_id"));
        return AjaxResult.success("设备数据已进入 RuoYi B Line 边界", result);
    }

    private Map<String, Object> buildProfile(String userId, Map<String, Object> source)
    {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("profileId", "BL-PROFILE-" + userId);
        profile.put("userId", userId);
        profile.put("profileStatus", "available");
        profile.put("lastUpdated", LocalDateTime.now().toString());
        profile.put("healthScore", numberValue(source != null ? source.get("healthScore") : null, 75));
        profile.put("fitnessLevel", stringValue(source != null ? source.get("fitnessLevel") : null, "moderate"));
        profile.put("stage", Map.of("stage", "beginner", "label", "初学期", "color", "#4CAF50"));
        profile.put("source", "ruoyi_b_line");
        profile.put("alineStatus", "reserved");
        return profile;
    }

    private Map<String, Object> recommendation(int index, String title, String content, String reason)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("index", index);
        item.put("title", title);
        item.put("content", content);
        item.put("reason", reason);
        return item;
    }

    private Object firstPresent(Map<String, Object> body, String... keys)
    {
        if (body == null)
        {
            return null;
        }
        for (String key : keys)
        {
            Object value = body.get(key);
            if (value != null && !String.valueOf(value).isBlank())
            {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value, String fallback)
    {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private int numberValue(Object value, int fallback)
    {
        if (value instanceof Number number)
        {
            return number.intValue();
        }
        try
        {
            return value == null || String.valueOf(value).isBlank() ? fallback : Integer.parseInt(String.valueOf(value));
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }
}
