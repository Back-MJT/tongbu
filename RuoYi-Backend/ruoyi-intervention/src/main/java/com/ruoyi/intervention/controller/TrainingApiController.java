package com.ruoyi.intervention.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.intervention.domain.enums.UserStage;
import com.ruoyi.intervention.domain.model.DeviceData;
import com.ruoyi.intervention.domain.model.HealthProfile;
import com.ruoyi.intervention.domain.model.UserTrainingData;
import com.ruoyi.intervention.service.ClaudeEngineService;
import com.ruoyi.intervention.service.ImuDataAggregationService;
import com.ruoyi.intervention.service.ProgressStatsService;
import com.ruoyi.intervention.service.TrainingPlanService;
import com.ruoyi.intervention.service.UserStageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 训练方案API控制器 — W2核心交付物
 * Training Plan API Controller — W2 core deliverable.
 *
 * <p>Exposes the training engine functionality via REST:
 * <ul>
 *   <li>POST /api/training/plan — Generate personalized training plan</li>
 *   <li>POST /api/training/daily-task — Generate daily exercise task</li>
 *   <li>GET  /api/training/progress/{userId} — Get progress statistics</li>
 *   <li>GET  /api/training/stage/{userId} — Get user stage assessment</li>
 *   <li>POST /api/training/adjust — Adaptive plan adjustment</li>
 *   <li>POST /api/training/imu/ingest — Ingest IMU data</li>
 * </ul>
 *
 * Ticket: XIN-107
 */
@RestController
@RequestMapping("/api/training")
@Validated
public class TrainingApiController
{
    private static final Logger log = LoggerFactory.getLogger(TrainingApiController.class);

    @Autowired
    private TrainingPlanService trainingPlanService;

    @Autowired
    private ClaudeEngineService claudeEngineService;

    @Autowired
    private ImuDataAggregationService imuDataAggregationService;

    @Autowired
    private ProgressStatsService progressStatsService;

    @Autowired
    private UserStageService userStageService;

    // ========== DTOs ==========

    /**
     * 生成训练方案请求 / Generate training plan request.
     */
    public static class GeneratePlanRequest
    {
        @NotBlank(message = "userId不能为空")
        private String userId;

        private String stage;
        private Integer totalWeeks;
        private HealthProfile healthProfile;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public Integer getTotalWeeks() { return totalWeeks; }
        public void setTotalWeeks(Integer totalWeeks) { this.totalWeeks = totalWeeks; }
        public HealthProfile getHealthProfile() { return healthProfile; }
        public void setHealthProfile(HealthProfile healthProfile) { this.healthProfile = healthProfile; }
    }

    /**
     * 生成每日任务请求 / Generate daily task request.
     */
    public static class DailyTaskRequest
    {
        @NotBlank(message = "userId不能为空")
        private String userId;

        private String date; // YYYY-MM-DD, defaults to today
        private String planId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
    }

    /**
     * 自适应调整请求 / Adaptive plan adjustment request.
     */
    public static class AdjustPlanRequest
    {
        @NotBlank(message = "userId不能为空")
        private String userId;

        @NotBlank(message = "planId不能为空")
        private String planId;

        private String reason; // low_compliance, plateau, user_feedback
        private Map<String, Object> context;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    /**
     * IMU数据上报请求 / IMU data ingestion request.
     */
    public static class ImuIngestRequest
    {
        @NotBlank(message = "userId不能为空")
        private String userId;

        @NotBlank(message = "deviceId不能为空")
        private String deviceId;

        @NotNull(message = "data不能为空")
        private DeviceData data;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public DeviceData getData() { return data; }
        public void setData(DeviceData data) { this.data = data; }
    }

    // ========== API Endpoints ==========

    /**
     * 生成训练方案 / Generate a personalized training plan.
     *
     * <p>Uses Claude API (or mock) to generate a plan based on user profile and stage.
     *
     * @param request plan generation parameters
     * @return generated training plan with recommendations
     */
    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> generatePlan(
            @Valid @RequestBody GeneratePlanRequest request)
    {
        log.info("generatePlan: userId={}, stage={}", request.getUserId(), request.getStage());

        try
        {
            UserStage stage = request.getStage() != null
                ? UserStage.fromValue(request.getStage())
                : UserStage.BEGINNER;

            // Build UserProfileInput for TrainingPlanService
            TrainingPlanService.UserProfileInput profileInput = new TrainingPlanService.UserProfileInput();
            profileInput.setUserId(request.getUserId());
            profileInput.setStage(stage);

            if (request.getHealthProfile() != null)
            {
                profileInput.setAge(request.getHealthProfile().getAge());
                profileInput.setGender(request.getHealthProfile().getGender());
                if (request.getHealthProfile().getHealthGoals() != null
                    && !request.getHealthProfile().getHealthGoals().isEmpty())
                {
                    profileInput.setGoal(request.getHealthProfile().getHealthGoals().get(0));
                }
            }

            // Generate plan via TrainingPlanService
            TrainingPlanService.TrainingPlanRecord planRecord =
                trainingPlanService.generateTrainingPlan(profileInput);

            Map<String, Object> planResult = new LinkedHashMap<>();
            planResult.put("planId", planRecord.getPlanId());
            planResult.put("stage", planRecord.getStage().getValue());
            planResult.put("systemPromptVersion", planRecord.getSystemPromptVersion());
            planResult.put("recommendations", planRecord.getRecommendations());
            planResult.put("rawResponse", planRecord.getRawResponse());
            planResult.put("status", planRecord.getStatus());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", planResult);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("generatePlan failed: userId={}, error={}", request.getUserId(), e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 生成每日训练任务 / Generate daily exercise task.
     *
     * <p>Generates a specific daily exercise prescription based on the user's
     * current plan, IMU readiness data, and training history.
     */
    @PostMapping("/daily-task")
    public ResponseEntity<Map<String, Object>> generateDailyTask(
            @Valid @RequestBody DailyTaskRequest request)
    {
        log.info("generateDailyTask: userId={}, date={}", request.getUserId(), request.getDate());

        try
        {
            String dateStr = request.getDate() != null
                ? request.getDate()
                : LocalDate.now().toString();

            LocalDate targetDate = LocalDate.parse(dateStr);

            // Get IMU data and aggregate for the day
            List<DeviceData> rawData = imuDataAggregationService.getRawDataForUserOnDate(
                request.getUserId(), targetDate);
            Map<String, Object> imuSummary;
            if (rawData != null && !rawData.isEmpty())
            {
                Map<String, Object> sessionSummary = imuDataAggregationService.aggregateSession(rawData);
                List<Map<String, Object>> sessions = new ArrayList<>();
                sessions.add(sessionSummary);
                imuSummary = imuDataAggregationService.aggregateDaily(sessions);
            }
            else
            {
                imuSummary = new LinkedHashMap<>();
                imuSummary.put("totalSessions", 0);
                imuSummary.put("totalDurationMinutes", 0L);
            }

            // Get user stage from stored state (or default BEGINNER)
            UserStage stage = userStageService.getUserStage(request.getUserId());
            if (stage == null)
            {
                stage = UserStage.BEGINNER;
            }

            // Build user profile for plan generation
            TrainingPlanService.UserProfileInput profileInput = new TrainingPlanService.UserProfileInput();
            profileInput.setUserId(request.getUserId());
            profileInput.setStage(stage);

            // Generate daily task as a new plan
            TrainingPlanService.TrainingPlanRecord taskRecord =
                trainingPlanService.generateTrainingPlan(profileInput);

            Map<String, Object> taskResult = new LinkedHashMap<>();
            taskResult.put("planId", taskRecord.getPlanId());
            taskResult.put("date", dateStr);
            taskResult.put("userStage", stage.getValue());
            taskResult.put("imuSummary", imuSummary);
            taskResult.put("recommendations", taskRecord.getRecommendations());
            taskResult.put("rawResponse", taskRecord.getRawResponse());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", taskResult);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("generateDailyTask failed: userId={}, error={}",
                       request.getUserId(), e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取训练进度统计 / Get training progress statistics.
     *
     * <p>Returns compliance rate, streaks, volume trends, and intensity progression.
     */
    @GetMapping("/progress/{userId}")
    public ResponseEntity<Map<String, Object>> getProgress(
            @PathVariable String userId,
            @RequestParam(defaultValue = "28") int days)
    {
        log.info("getProgress: userId={}, days={}", userId, days);

        try
        {
            Map<String, Object> stats = progressStatsService.getFullStats(userId);

            // Get stored user stage (or default BEGINNER)
            UserStage stage = userStageService.getUserStage(userId);
            if (stage == null)
            {
                stage = UserStage.BEGINNER;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", userId);
            data.put("stage", stage.getValue());
            data.put("stageLabel", stage.getLabelZh());
            data.put("stats", stats);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("getProgress failed: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取用户阶段评估 / Get user stage assessment.
     */
    @GetMapping("/stage/{userId}")
    public ResponseEntity<Map<String, Object>> getStage(@PathVariable String userId)
    {
        log.info("getStage: userId={}", userId);

        try
        {
            // Get stored stage (or default BEGINNER)
            UserStage stage = userStageService.getUserStage(userId);
            if (stage == null)
            {
                stage = UserStage.BEGINNER;
            }

            // Build stage info from available service methods
            Map<String, Object> stageInfo = new LinkedHashMap<>();
            stageInfo.put("stage", stage.getValue());
            stageInfo.put("stageLabel", stage.getLabelZh());
            stageInfo.put("transitionHistory", userStageService.getTransitionHistory(userId));

            com.ruoyi.intervention.domain.model.StageBehavior behavior =
                userStageService.getStageBehavior(stage);
            if (behavior != null)
            {
                stageInfo.put("behavior", behavior);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", userId);
            data.put("stage", stage.getValue());
            data.put("stageLabel", stage.getLabelZh());
            data.put("details", stageInfo);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("getStage failed: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 自适应计划调整 / Adaptive plan adjustment.
     *
     * <p>Triggered by low compliance, plateau detection, or user feedback.
     * Uses Claude API to generate an adjusted plan that is easier (not harder)
     * per Fogg behavior model principles.
     */
    @PostMapping("/adjust")
    public ResponseEntity<Map<String, Object>> adjustPlan(
            @Valid @RequestBody AdjustPlanRequest request)
    {
        log.info("adjustPlan: userId={}, planId={}, reason={}",
                 request.getUserId(), request.getPlanId(), request.getReason());

        try
        {
            // Retrieve existing plan
            TrainingPlanService.TrainingPlanRecord existingPlan =
                trainingPlanService.getPlan(request.getPlanId());

            if (existingPlan == null)
            {
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "方案不存在: " + request.getPlanId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Build adjusted profile input — use existing plan's stage and data
            TrainingPlanService.UserProfileInput profileInput = new TrainingPlanService.UserProfileInput();
            profileInput.setUserId(request.getUserId());
            profileInput.setStage(existingPlan.getStage());

            // Inject adjustment context into the user prompt via goal field
            String adjustmentNote = "调整原因: " + (request.getReason() != null ? request.getReason() : "用户反馈");
            if (request.getContext() != null && !request.getContext().isEmpty())
            {
                adjustmentNote += ", 上下文: " + request.getContext();
            }
            profileInput.setGoal(adjustmentNote);

            // Regenerate plan with adjustment context
            TrainingPlanService.TrainingPlanRecord adjustedPlan =
                trainingPlanService.generateTrainingPlan(profileInput);

            // Mark old plan as superseded
            trainingPlanService.updatePlanStatus(request.getPlanId(), "superseded");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("originalPlanId", request.getPlanId());
            result.put("newPlanId", adjustedPlan.getPlanId());
            result.put("reason", request.getReason());
            result.put("recommendations", adjustedPlan.getRecommendations());
            result.put("status", adjustedPlan.getStatus());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("adjustPlan failed: userId={}, error={}", request.getUserId(), e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 上报IMU数据 / Ingest IMU sensor data.
     *
     * <p>Accepts raw IMU data from BLE device, aggregates it into
     * training session summaries for downstream consumption.
     */
    @PostMapping("/imu/ingest")
    public ResponseEntity<Map<String, Object>> ingestImuData(
            @Valid @RequestBody ImuIngestRequest request)
    {
        log.info("ingestImuData: userId={}, deviceId={}", request.getUserId(), request.getDeviceId());

        try
        {
            DeviceData data = request.getData();
            // Ensure userId and deviceId are set on the DeviceData
            if (data != null)
            {
                data.setUserId(request.getUserId());
                data.setDeviceId(request.getDeviceId());
            }

            imuDataAggregationService.addRawData(data);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "IMU数据已接收并处理");
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("ingestImuData failed: userId={}, error={}",
                       request.getUserId(), e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Claude引擎调用统计 / Claude engine call statistics.
     */
    @GetMapping("/engine/stats")
    public ResponseEntity<Map<String, Object>> getEngineStats()
    {
        Map<String, Object> stats = claudeEngineService.getCacheStats();
        Map<String, Object> costSummary = claudeEngineService.getDailyCostSummary(null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cache", stats);
        data.put("dailyCost", costSummary);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
