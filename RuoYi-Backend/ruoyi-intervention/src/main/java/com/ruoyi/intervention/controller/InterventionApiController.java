package com.ruoyi.intervention.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.intervention.service.aline.AlineAnalysisGateway;
import com.ruoyi.intervention.service.aline.AlineAnalysisRequest;
import com.ruoyi.intervention.service.aline.AlineAnalysisResult;

/**
 * B Line owned A Line placeholder API - training result analysis.
 *
 * <p>这些接口用于：
 * <ul>
 *   <li>GET /api/intervention/health-profile/{userId} — 获取用户健康档案</li>
 *   <li>POST /api/intervention/exercise-prescription — 提交训练结果并触发AI分析</li>
 *   <li>GET /api/intervention/analysis/{sessionId} — 获取训练分析结果</li>
 * </ul>
 *
 * 注意：A Line API 尚未提供，本控制器只调用 B Line 门面与 stub，不做外部 A Line 网络请求。
 */
@RestController
@RequestMapping("/api/intervention")
public class InterventionApiController
{
    private static final Logger log = LoggerFactory.getLogger(InterventionApiController.class);

    @Autowired
    private AlineAnalysisGateway alineAnalysisGateway;

    /**
     * 获取用户健康档案 / Get user health profile.
     *
     * <p>XIN-146 预留接口：前端调用此接口获取用户健康档案数据。
     * 目前返回模拟数据，A Line API 正式接入后替换为真实调用。
     */
    @GetMapping("/health-profile/{userId}")
    public ResponseEntity<Map<String, Object>> getHealthProfile(@PathVariable String userId)
    {
        log.info("getHealthProfile: userId={}", userId);
        try
        {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("userId", userId);
            profile.put("profileStatus", "available");
            profile.put("lastUpdated", java.time.LocalDateTime.now().toString());
            profile.put("healthScore", 75);
            profile.put("fitnessLevel", "moderate");
            profile.put("recommendations", java.util.List.of(
                "建议每次训练保持60分钟以上",
                "注意组间休息控制在60秒内",
                "每周至少3次力量训练"
            ));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", profile);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("getHealthProfile failed: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "健康档案获取失败");
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 提交训练结果并触发 AI 分析
     * Submit training result and trigger AI analysis.
     *
     * <p>训练提交后调用此接口，触发后端 A Line 分析任务。
     * 前端应展示"分析中"状态，并在5秒内返回结果或状态。
     */
    @PostMapping("/analysis")
    public ResponseEntity<Map<String, Object>> submitForAnalysis(
            @RequestBody Map<String, Object> trainingResult)
    {
        log.info("submitForAnalysis: trainingResult={}", trainingResult);
        try
        {
            AlineAnalysisResult result = alineAnalysisGateway.createAnalysis(toAnalysisRequest(trainingResult));
            return ResponseEntity.ok(success(result.toMap()));
        }
        catch (Exception e)
        {
            log.error("submitForAnalysis failed: error={}", e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "分析任务创建失败");
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Backward compatible alias retained for existing callers.
     */
    @PostMapping("/exercise-prescription")
    public ResponseEntity<Map<String, Object>> submitExercisePrescriptionAlias(
            @RequestBody Map<String, Object> trainingResult)
    {
        return submitForAnalysis(trainingResult);
    }

    /**
     * 获取训练分析结果
     * Get training analysis result.
     *
     * <p>轮询此接口获取 AI 分析进度和结果。
     * 返回状态：processing（分析中）/ completed（已完成）/ failed（失败）
     */
    @GetMapping("/analysis/{sessionId}")
    public ResponseEntity<Map<String, Object>> getAnalysisResult(
            @PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "false") boolean waitForComplete)
    {
        log.info("getAnalysisResult: sessionId={}, waitForComplete={}", sessionId, waitForComplete);
        try
        {
            AlineAnalysisResult result = alineAnalysisGateway.getAnalysisResult(sessionId);
            return ResponseEntity.ok(success(result.toMap()));
        }
        catch (Exception e)
        {
            log.error("getAnalysisResult failed: sessionId={}, error={}", sessionId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "分析结果获取失败");
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 获取分析状态（轻量级轮询接口）
     * Get analysis status (lightweight polling endpoint).
     */
    @GetMapping("/analysis-status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(@PathVariable String sessionId)
    {
        log.info("getAnalysisStatus: sessionId={}", sessionId);
        try
        {
            AlineAnalysisResult result = alineAnalysisGateway.getAnalysisStatus(sessionId);
            Map<String, Object> status = result.toMap();
            status.put("hasResult", result.getAiFeedback() != null);
            return ResponseEntity.ok(success(status));
        }
        catch (Exception e)
        {
            log.error("getAnalysisStatus failed: sessionId={}, error={}", sessionId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "状态查询失败");
            return ResponseEntity.ok(errorResponse);
        }
    }

    @SuppressWarnings("unchecked")
    private AlineAnalysisRequest toAnalysisRequest(Map<String, Object> body)
    {
        body = body != null ? body : java.util.Collections.emptyMap();
        AlineAnalysisRequest request = new AlineAnalysisRequest();
        request.setTenantId(longValue(body.get("tenantId")));
        request.setUserId(stringValue(body.get("userId")));
        request.setSessionId(stringValue(body.get("sessionId")));
        request.setTaskId(stringValue(body.get("taskId")));
        request.setPrescriptionId(stringValue(body.get("prescriptionId")));
        request.setEquipmentCode(stringValue(body.get("equipmentCode")));
        request.setDeviceCode(stringValue(body.get("deviceCode")));
        request.setExerciseType(stringValue(body.get("exerciseType")));
        request.setCompletedSets(intValue(body.get("completedSets")));
        request.setTotalReps(intValue(body.get("totalReps")));
        request.setDurationMin(intValue(body.get("durationMin")));
        request.setTotalVolumeKg(doubleValue(body.get("totalVolumeKg")));
        request.setSource("b_line");
        if (body.get("sets") instanceof java.util.List<?>)
        {
            request.setSets((java.util.List<Map<String, Object>>) body.get("sets"));
        }
        return request;
    }

    private Map<String, Object> success(Map<String, Object> data)
    {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("data", data);
        return response;
    }

    private String stringValue(Object value)
    {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private Long longValue(Object value)
    {
        try
        {
            if (value instanceof Number)
            {
                return ((Number) value).longValue();
            }
            return value == null || String.valueOf(value).isBlank() ? null : Long.valueOf(String.valueOf(value));
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private Integer intValue(Object value)
    {
        try
        {
            if (value instanceof Number)
            {
                return ((Number) value).intValue();
            }
            return value == null || String.valueOf(value).isBlank() ? null : Integer.valueOf(String.valueOf(value));
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private Double doubleValue(Object value)
    {
        try
        {
            if (value instanceof Number)
            {
                return ((Number) value).doubleValue();
            }
            return value == null || String.valueOf(value).isBlank() ? null : Double.valueOf(String.valueOf(value));
        }
        catch (Exception ignored)
        {
            return null;
        }
    }
}
