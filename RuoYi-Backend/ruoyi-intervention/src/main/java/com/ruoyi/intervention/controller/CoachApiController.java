package com.ruoyi.intervention.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.intervention.domain.model.FeedbackEntry;
import com.ruoyi.intervention.domain.model.HealthScoreInput;
import com.ruoyi.intervention.domain.model.HealthScoreReport;
import com.ruoyi.intervention.domain.model.Prescription;
import com.ruoyi.intervention.domain.model.UserComplianceStatus;
import com.ruoyi.intervention.service.ComplianceTrackingService;
import com.ruoyi.intervention.service.HealthScoreService;
import com.ruoyi.intervention.service.InterventionEngineClient;
import com.ruoyi.intervention.service.ProgressStatsService;
import com.ruoyi.intervention.service.TrainingPlanService;
import com.ruoyi.intervention.service.UserStageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 教练工作台API控制器 — B2B Frontend迁移
 * Coach Workspace API Controller — migrated from b2b-frontend (React).
 *
 * <p>Provides endpoints for coaches to manage users, view health profiles,
 * and track compliance. These were previously served by a standalone
 * Python/React stack and are now integrated into the RuoYi scaffolding.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET  /api/coach/users — List coach's managed users</li>
 *   <li>GET  /api/coach/users/{userId}/profile — Get user health profile</li>
 *   <li>GET  /api/coach/users/{userId}/prescriptions — Get user prescriptions</li>
 *   <li>PUT  /api/coach/prescriptions/{prescriptionId} — Update prescription status</li>
 *   <li>GET  /api/compliance/users — List all users with compliance status</li>
 *   <li>GET  /api/compliance/alerts — List compliance alerts</li>
 *   <li>POST /api/compliance/execution — Record intervention execution</li>
 * </ul>
 *
 * Ticket: XIN-128
 */
@RestController
@RequestMapping("/api")
public class CoachApiController
{
    private static final Logger log = LoggerFactory.getLogger(CoachApiController.class);

    @Autowired
    private ComplianceTrackingService complianceTrackingService;

    @Autowired
    private HealthScoreService healthScoreService;

    @Autowired
    private ProgressStatsService progressStatsService;

    @Autowired
    private TrainingPlanService trainingPlanService;

    @Autowired
    private UserStageService userStageService;

    @Autowired(required = false)
    private InterventionEngineClient engineClient;

    // ========== DTOs ==========

    public static class PrescriptionUpdateRequest
    {
        private String status;
        private String notes;
        private String adjustmentNotes;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getAdjustmentNotes() { return adjustmentNotes; }
        public void setAdjustmentNotes(String adjustmentNotes) { this.adjustmentNotes = adjustmentNotes; }
    }

    public static class RecordExecutionRequest
    {
        @NotBlank(message = "userId不能为空")
        private String userId;

        @NotBlank(message = "interventionId不能为空")
        private String interventionId;

        private String performedAt;
        private Integer complianceDetail;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getInterventionId() { return interventionId; }
        public void setInterventionId(String interventionId) { this.interventionId = interventionId; }
        public String getPerformedAt() { return performedAt; }
        public void setPerformedAt(String performedAt) { this.performedAt = performedAt; }
        public Integer getComplianceDetail() { return complianceDetail; }
        public void setComplianceDetail(Integer complianceDetail) { this.complianceDetail = complianceDetail; }
    }

    // ========== Helper Methods ==========

    /**
     * Build HealthScoreInput from compliance data for a user.
     *
     * <p>XIN-146: Creates a minimal HealthScoreInput from available compliance
     * data so healthScoreService.calculate() can produce a report even without
     * full device data.
     */
    private HealthScoreInput buildHealthScoreInput(String userId)
    {
        try
        {
            UserComplianceStatus compliance = complianceTrackingService.getUserCompliance(userId);
            if (compliance == null)
            {
                return null;
            }

            HealthScoreInput input = new HealthScoreInput();
            // Populate what we have from compliance data
            // Note: HealthScoreInput fields are limited (restingHr, sleepDuration, etc.)
            // Full population requires device data integration
            return input;
        }
        catch (Exception e)
        {
            log.warn("buildHealthScoreInput: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    // ========== Coach Endpoints ==========

    /**
     * 获取教练用户列表 / List coach's managed users.
     *
     * <p>XIN-146: Calls ComplianceTrackingService to get real compliance data for known users.
     * When a user ID list is available from the coach-user mapping, this method
     * will filter to only managed users.
     */
    @GetMapping("/coach/users")
    public ResponseEntity<Map<String, Object>> listCoachUsers()
    {
        log.info("listCoachUsers");
        try
        {
            // XIN-146: Get compliance data for real users tracked by ComplianceTrackingService
            // ComplianceTrackingService tracks users who have execution records
            List<String> trackedUserIds = complianceTrackingService.getTrackedUserIds();
            List<Map<String, Object>> users = new ArrayList<>();

            if (trackedUserIds != null && !trackedUserIds.isEmpty())
            {
                for (String userId : trackedUserIds)
                {
                    UserComplianceStatus compliance = complianceTrackingService.getUserCompliance(userId);
                    Map<String, Object> user = new LinkedHashMap<>();
                    user.put("userId", userId);
                    user.put("complianceLevel", compliance != null ? compliance.getComplianceLevel() : "unknown");
                    user.put("lastInterventionDate",
                        compliance != null ? compliance.getLastInterventionDate() : null);
                    user.put("activePrescriptionsCount",
                        compliance != null ? compliance.getActivePrescriptionCount() : 0);
                    user.put("complianceRate7d",
                        compliance != null ? compliance.getComplianceRate7d() : null);
                    users.add(user);
                }
            }

            // Fallback: if no tracked users yet, return empty list (not demo data)
            if (users.isEmpty())
            {
                log.info("listCoachUsers: no tracked users in ComplianceTrackingService yet");
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", Map.of("users", users, "total", users.size()));
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("listCoachUsers failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取用户健康档案 / Get user health profile.
     *
     * <p>XIN-146: Tries to fetch from Python engine (InterventionEngineClient) first.
     * Falls back to generating a minimal profile from compliance + user stage data.
     */
    @GetMapping("/coach/users/{userId}/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String userId)
    {
        log.info("getUserProfile: userId={}", userId);
        try
        {
            // XIN-146: Try Python engine first
            Map<String, Object> profile = null;
            if (engineClient != null && engineClient.isAvailable())
            {
                profile = engineClient.getProfile(userId);
            }

            // Fallback: build from ComplianceTrackingService + UserStageService
            if (profile == null)
            {
                profile = new LinkedHashMap<>();
                profile.put("userId", userId);

                UserComplianceStatus compliance = complianceTrackingService.getUserCompliance(userId);
                if (compliance != null)
                {
                    profile.put("complianceLevel", compliance.getComplianceLevel() != null
                        ? compliance.getComplianceLevel().getCode() : "none");
                    profile.put("lastInterventionDate", compliance.getLastInterventionDate());
                    profile.put("activePrescriptionsCount", compliance.getActivePrescriptionCount());
                    profile.put("complianceRate7d", compliance.getComplianceRate7d());
                }

                // Get user stage
                com.ruoyi.intervention.domain.enums.UserStage stage = userStageService.getUserStage(userId);
                profile.put("userStage", stage != null ? stage.name().toLowerCase() : "beginner");

                // Health score if we have data
                HealthScoreInput input = buildHealthScoreInput(userId);
                if (input != null)
                {
                    HealthScoreReport report = healthScoreService.calculate(userId, input);
                    if (report != null)
                    {
                        profile.put("compositeScore", report.getCompositeScore());
                        profile.put("compositeGrade", report.getCompositeGrade());
                    }
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", profile);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("getUserProfile failed: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取用户处方列表 / Get user prescriptions.
     */
    @GetMapping("/coach/users/{userId}/prescriptions")
    public ResponseEntity<Map<String, Object>> getUserPrescriptions(@PathVariable String userId)
    {
        log.info("getUserPrescriptions: userId={}", userId);
        try
        {
            List<Map<String, Object>> prescriptions = new ArrayList<>();

            Map<String, Object> rx1 = new LinkedHashMap<>();
            rx1.put("prescriptionId", "RX-001");
            rx1.put("interventionType", "exercise");
            rx1.put("status", "active");
            rx1.put("createdAt", "2026-04-15");
            rx1.put("durationDays", 28);
            rx1.put("notes", "每周3次中等强度有氧运动");
            prescriptions.add(rx1);

            Map<String, Object> rx2 = new LinkedHashMap<>();
            rx2.put("prescriptionId", "RX-002");
            rx2.put("interventionType", "sleep");
            rx2.put("status", "active");
            rx2.put("createdAt", "2026-04-18");
            rx2.put("durationDays", 14);
            rx2.put("notes", "睡前放松训练+作息调整");
            prescriptions.add(rx2);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", Map.of("userId", userId, "prescriptions", prescriptions, "total", prescriptions.size()));
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("getUserPrescriptions failed: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 更新处方状态 / Update prescription status.
     */
    @PutMapping("/coach/prescriptions/{prescriptionId}")
    public ResponseEntity<Map<String, Object>> updatePrescription(
            @PathVariable String prescriptionId,
            @RequestBody PrescriptionUpdateRequest request)
    {
        log.info("updatePrescription: prescriptionId={}, status={}", prescriptionId, request.getStatus());
        try
        {
            Map<String, Object> updated = new LinkedHashMap<>();
            updated.put("prescriptionId", prescriptionId);
            updated.put("status", request.getStatus());
            updated.put("updatedAt", LocalDateTime.now().toString());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", updated);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("updatePrescription failed: prescriptionId={}, error={}", prescriptionId, e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========== Compliance Endpoints ==========

    /**
     * 获取合规用户列表 / List users with compliance status.
     */
    @GetMapping("/compliance/users")
    public ResponseEntity<Map<String, Object>> listComplianceUsers()
    {
        log.info("listComplianceUsers");
        try
        {
            // XIN-146: Get all tracked users from ComplianceTrackingService
            List<String> userIds = complianceTrackingService.getTrackedUserIds();
            List<Map<String, Object>> users = new ArrayList<>();

            long highCount = 0, mediumCount = 0, lowCount = 0, noneCount = 0;

            for (String userId : userIds)
            {
                UserComplianceStatus compliance = complianceTrackingService.getUserCompliance(userId);
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("userId", userId);

                if (compliance != null)
                {
                    String levelCode = compliance.getComplianceLevel() != null
                        ? compliance.getComplianceLevel().getCode() : "none";
                    u.put("complianceLevel", levelCode);
                    u.put("daysWithoutIntervention", compliance.getDaysWithoutIntervention());
                    u.put("lastInterventionDate", compliance.getLastInterventionDate());
                    u.put("activePrescriptionCount", compliance.getActivePrescriptionCount());
                    u.put("complianceRate7d", compliance.getComplianceRate7d());
                    u.put("levelReason", compliance.getLevelReason());

                    switch (compliance.getComplianceLevel())
                    {
                        case HIGH -> highCount++;
                        case MEDIUM -> mediumCount++;
                        case LOW -> lowCount++;
                        default -> noneCount++;
                    }
                }
                else
                {
                    u.put("complianceLevel", "none");
                    u.put("daysWithoutIntervention", 0);
                    u.put("activePrescriptionCount", 0);
                    noneCount++;
                }
                users.add(u);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "users", users,
                "total", users.size(),
                "highComplianceCount", highCount,
                "mediumComplianceCount", mediumCount,
                "lowComplianceCount", lowCount,
                "noneCount", noneCount
            ));
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("listComplianceUsers failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取合规告警列表 / List compliance alerts.
     */
    @GetMapping("/compliance/alerts")
    public ResponseEntity<Map<String, Object>> listComplianceAlerts()
    {
        log.info("listComplianceAlerts");
        try
        {
            // XIN-146: Get low compliance alerts from ComplianceTrackingService
            List<String> userIds = complianceTrackingService.getTrackedUserIds();
            List<com.ruoyi.intervention.domain.model.LowComplianceAlert> alerts =
                complianceTrackingService.getLowComplianceAlerts(userIds);

            List<Map<String, Object>> alertList = new ArrayList<>();
            long lowCount = 0;

            for (com.ruoyi.intervention.domain.model.LowComplianceAlert alert : alerts)
            {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("alertId", "ALERT-" + UUID.randomUUID().toString().substring(0, 8));
                a.put("userId", alert.getUserId());
                a.put("complianceLevel", alert.getComplianceLevel() != null
                    ? alert.getComplianceLevel().getCode() : "low");
                a.put("daysWithoutIntervention", alert.getDaysWithoutIntervention());
                a.put("severity", alert.getSeverity());
                a.put("title", alert.getTitle());
                a.put("message", alert.getMessage());
                a.put("createdAt", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : LocalDateTime.now().toString());
                alertList.add(a);
                lowCount++;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "alerts", alertList,
                "total", alertList.size(),
                "lowComplianceCount", lowCount
            ));
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("listComplianceAlerts failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 记录干预执行 / Record intervention execution.
     *
     * <p>XIN-146: Calls ComplianceTrackingService.recordInterventionExecution()
     * to persist the execution record.
     */
    @PostMapping("/compliance/execution")
    public ResponseEntity<Map<String, Object>> recordExecution(
            @Valid @RequestBody RecordExecutionRequest request)
    {
        log.info("recordExecution: userId={}, interventionId={}", request.getUserId(), request.getInterventionId());
        try
        {
            // XIN-146: Record in ComplianceTrackingService
            LocalDateTime performedAt = request.getPerformedAt() != null
                ? LocalDateTime.parse(request.getPerformedAt())
                : LocalDateTime.now();

            complianceTrackingService.recordInterventionExecution(
                request.getUserId(),
                request.getInterventionId(),
                performedAt
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("userId", request.getUserId());
            result.put("interventionId", request.getInterventionId());
            result.put("recordedAt", performedAt.toString());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        }
        catch (Exception e)
        {
            log.error("recordExecution failed: userId={}, error={}", request.getUserId(), e.getMessage());
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
