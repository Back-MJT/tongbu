package com.ruoyi.intervention.service.aline;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stable B Line analysis result shape for mini-program rendering.
 */
public class AlineAnalysisResult
{
    private String taskId;
    private String sessionId;
    private String userId;
    private String status;
    private int progress;
    private String message;
    private Map<String, Object> aiFeedback;
    private String provider = "b_line_stub";
    private String source = "b_line";
    private String createdAt;
    private String completedAt;

    public static AlineAnalysisResult unavailable(AlineAnalysisRequest request)
    {
        AlineAnalysisResult result = new AlineAnalysisResult();
        String now = Instant.now().toString();
        result.setTaskId("ALINE-STUB-" + System.currentTimeMillis());
        result.setSessionId(request != null ? request.getSessionId() : null);
        result.setUserId(request != null ? request.getUserId() : null);
        result.setStatus("unavailable");
        result.setProgress(0);
        result.setMessage("A Line API 尚未提供，B Line 已保留身体引擎分析任务占位。");
        result.setCreatedAt(now);
        result.setAiFeedback(buildStubFeedback(request));
        return result;
    }

    private static Map<String, Object> buildStubFeedback(AlineAnalysisRequest request)
    {
        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("overallScore", 0);
        feedback.put("strengthLevel", "待接入");
        feedback.put("summary", "本次训练记录已在 B Line 保存。A Line 身体引擎 API 给到后，将在此生成正式分析。");
        feedback.put("suggestions", java.util.List.of(
            "保持动作节奏稳定，优先完成计划组数。",
            "训练后记录主观疲劳和疼痛情况，后续可作为身体引擎输入。",
            "下一次接入 A Line 后，将结合训练历史、HRV/DFA 与安全约束生成反馈。"
        ));
        feedback.put("warnings", java.util.List.of());
        feedback.put("nextRecommendation", request != null && request.getTotalReps() != null && request.getTotalReps() > 0
            ? "下次训练先维持当前强度，等待身体引擎正式评估。"
            : "请先完成一组有效训练，再生成更有参考价值的反馈。");
        feedback.put("isPlaceholder", true);
        feedback.put("provider", "b_line_stub");
        return feedback;
    }

    public Map<String, Object> toMap()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("analysisTaskId", taskId);
        data.put("sessionId", sessionId);
        data.put("userId", userId);
        data.put("status", status);
        data.put("analysisStatus", status);
        data.put("progress", progress);
        data.put("message", message);
        data.put("aiFeedback", aiFeedback);
        data.put("provider", provider);
        data.put("source", source);
        data.put("createdAt", createdAt);
        data.put("completedAt", completedAt);
        return data;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(Map<String, Object> aiFeedback) { this.aiFeedback = aiFeedback; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
