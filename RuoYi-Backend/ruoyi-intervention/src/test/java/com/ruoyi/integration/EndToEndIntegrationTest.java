package com.ruoyi.integration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.ruoyi.intervention.domain.enums.ComplianceLevel;
import com.ruoyi.intervention.domain.enums.ComplianceStatus;
import com.ruoyi.intervention.domain.enums.FeedbackType;
import com.ruoyi.intervention.domain.enums.UserStage;
import com.ruoyi.intervention.domain.model.FeedbackEntry;
import com.ruoyi.intervention.domain.model.HealthScoreInput;
import com.ruoyi.intervention.domain.model.HealthScoreReport;
import com.ruoyi.intervention.domain.model.StageBehavior;
import com.ruoyi.intervention.domain.model.StageTransition;
import com.ruoyi.intervention.domain.model.UserTrainingData;
import com.ruoyi.intervention.service.ClaudeEngineService;
import com.ruoyi.intervention.service.ComplianceTrackingService;
import com.ruoyi.intervention.service.HealthScoreService;
import com.ruoyi.intervention.service.TrainingPlanService;
import com.ruoyi.intervention.service.TrainingPlanService.TrainingPlanRecord;
import com.ruoyi.intervention.service.TrainingPlanService.UserProfileInput;
import com.ruoyi.intervention.service.UserStageService;

/**
 * End-to-end integration test for the full HealthHub intervention pipeline.
 *
 * Tests the complete user lifecycle flow:
 *   1. User arrives with no history → HealthScoreService baseline assessment
 *   2. UserStageService identifies stage as BEGINNER
 *   3. TrainingPlanService generates plan via ClaudeEngineService (mock mode)
 *   4. User trains, records compliance via ComplianceTrackingService
 *   5. UserStageService detects stage transition (BEGINNER → GROWTH)
 *   6. TrainingPlanService generates new plan with stage-appropriate prompt
 *   7. Full cycle: feedback → stage change → new plan → cost tracking
 *
 * Evidence base:
 *   - Prochaska & DiClemente 1983: Stages of Change
 *   - ACSM Guidelines: Exercise prescription parameters
 *   - Board v1.1 Section 2.3/3.3: Data models and flow
 *
 * @author Algorithm Engineer (XIN-87)
 */
@DisplayName("End-to-End: Full Intervention Pipeline")
class EndToEndIntegrationTest
{
    private ClaudeEngineService claudeEngine;
    private TrainingPlanService trainingPlanService;
    private UserStageService userStageService;
    private ComplianceTrackingService complianceService;
    private HealthScoreService healthScoreService;

    @BeforeEach
    void setUp()
    {
        claudeEngine = new ClaudeEngineService();
        // Mock ApiClient: (model, system, user, temperature, maxTokens) → Map
        claudeEngine.setApiClient((model, system, user, temperature, maxTokens) ->
            Map.<String, Object>of(
                "response", "1. 建议本周进行3次30分钟中等强度有氧训练。理由：作为初学者，循序渐进建立基础体能。\n"
                    + "2. 每次训练前做5分钟热身。理由：预防运动损伤，逐步提升心率。\n"
                    + "3. 尝试跑步机快走模式，速度5-6km/h。理由：符合当前体能水平，可持续执行。",
                "input_tokens", 200,
                "output_tokens", 150
            )
        );

        trainingPlanService = new TrainingPlanService();
        trainingPlanService.setClaudeEngine(claudeEngine);

        userStageService = new UserStageService();
        complianceService = new ComplianceTrackingService();
        healthScoreService = new HealthScoreService();
    }

    @AfterEach
    void tearDown()
    {
        claudeEngine.resetAll();
        trainingPlanService.resetAll();
        userStageService.resetAll();
        complianceService.resetAll();
    }

    // ========== Test 1: New User Full Lifecycle ==========

    @Test
    @DisplayName("E2E: New user → baseline → BEGINNER → plan → compliance → GROWTH transition")
    void newUserFullLifecycle()
    {
        String userId = "user_e2e_001";
        LocalDateTime now = LocalDateTime.now();

        // --- Step 1: Assess baseline health score ---
        HealthScoreInput healthInput = new HealthScoreInput();
        healthInput.setDailySteps(3000);
        healthInput.setWeeklyExerciseMinutes(15.0);
        healthInput.setRestingHr(80);
        healthInput.setSleepDuration(5.5);
        healthInput.setSleepEfficiency(0.65);

        HealthScoreReport scoreReport = healthScoreService.calculate(userId, healthInput);
        assertNotNull(scoreReport, "Health score report should be generated");
        assertTrue(scoreReport.getCompositeScore() < 60,
            "Sedentary user should score below 60, got: " + scoreReport.getCompositeScore());

        // --- Step 2: Identify stage → BEGINNER ---
        UserTrainingData trainingData = new UserTrainingData();
        trainingData.setUserId(userId);
        trainingData.setTenureWeeks(0.5);
        trainingData.setWeeklyFrequency(1.0);
        trainingData.setCompletionRate(0.4);
        trainingData.setLastSessionDate(now.minusDays(2));
        trainingData.setIntensityTrend(0.0);
        trainingData.setTotalSessions(2);

        UserStage initialStage = userStageService.identifyStage(trainingData);
        assertEquals(UserStage.BEGINNER, initialStage, "New user with low frequency = BEGINNER");

        // --- Step 3: Generate first training plan ---
        UserProfileInput profile = new UserProfileInput();
        profile.setUserId(userId);
        profile.setName("张三");
        profile.setAge(35);
        profile.setGender("男");
        profile.setCity("宁津县");
        profile.setDeviceType("跑步机");
        profile.setDeviceModel("NK-3000");
        profile.setGoal("增强体质");
        profile.setSessionsLast30Days(2);
        profile.setAvgDurationMinutes(15.0);
        profile.setSessionsLastWeek(1);
        profile.setSessionsPreviousWeek(0);
        profile.setLastSessionDate("2026-04-12");
        profile.setLastCompletionRate(0.4);
        profile.setStage(initialStage);

        TrainingPlanRecord plan1 = trainingPlanService.generateTrainingPlan(profile);
        assertNotNull(plan1, "Training plan should be generated");
        assertNotNull(plan1.getPlanId(), "Plan should have an ID");
        assertEquals(userId, plan1.getUserId());
        assertEquals(UserStage.BEGINNER, plan1.getStage());
        assertEquals("v1", plan1.getSystemPromptVersion(), "BEGINNER uses v1 prompt");
        assertEquals("generated", plan1.getStatus());
        assertFalse(plan1.getRecommendations().isEmpty(), "Plan should have recommendations");

        // --- Step 4: Verify Claude engine logged the call ---
        var callLogs = claudeEngine.getCallLogs(userId, null, null, 10);
        assertFalse(callLogs.isEmpty(), "Claude engine should have logged the API call");
        assertEquals("success", callLogs.get(0).getStatus());

        // --- Step 5: User trains for 3 sessions ---
        complianceService.setActivePrescriptionCount(userId, 3);
        String intv1 = plan1.getPlanId() + "_s1";
        String intv2 = plan1.getPlanId() + "_s2";
        String intv3 = plan1.getPlanId() + "_s3";
        complianceService.recordInterventionExecution(userId, intv1, now.minusDays(1));
        complianceService.recordInterventionExecution(userId, intv2, now);
        complianceService.recordInterventionExecution(userId, intv3, now);

        // Record positive feedback
        for (String intvId : List.of(intv1, intv2, intv3))
        {
            FeedbackEntry feedback = new FeedbackEntry();
            feedback.setUserId(userId);
            feedback.setInterventionId(intvId);
            feedback.setFeedbackType(FeedbackType.COMPLIANCE);
            feedback.setComplianceStatus(ComplianceStatus.FULL);
            feedback.setComplianceDetail(1.0);
            feedback.setRpe(5);
            feedback.setSubjectiveOutcome("energized");
            feedback.setEnergyLevel(7);
            complianceService.recordFeedback(feedback);
        }

        // --- Step 6: Check stage transition after sustained training ---
        UserTrainingData updatedData = new UserTrainingData();
        updatedData.setUserId(userId);
        updatedData.setTenureWeeks(6.0);
        updatedData.setWeeklyFrequency(4.0);
        updatedData.setCompletionRate(0.85);
        updatedData.setLastSessionDate(now.minusDays(1));
        updatedData.setIntensityTrend(0.15);
        updatedData.setTotalSessions(24);

        UserStage newStage = userStageService.identifyStage(updatedData);
        assertNotEquals(UserStage.BEGINNER, newStage,
            "6 weeks, 85% completion → should advance from BEGINNER");

        // First set initial stage so transition is detected
        userStageService.setUserStage(userId, UserStage.BEGINNER, "test setup");
        StageTransition transition = userStageService.determineTransition(userId, updatedData);
        assertNotNull(transition, "Transition should be detected");
        assertEquals(UserStage.BEGINNER, transition.getFromStage());
        assertNotEquals(UserStage.BEGINNER, transition.getToStage());

        // --- Step 7: Generate new plan with updated stage ---
        UserProfileInput updatedProfile = new UserProfileInput();
        updatedProfile.setUserId(userId);
        updatedProfile.setName("张三");
        updatedProfile.setAge(35);
        updatedProfile.setGender("男");
        updatedProfile.setCity("宁津县");
        updatedProfile.setDeviceType("跑步机");
        updatedProfile.setDeviceModel("NK-3000");
        updatedProfile.setGoal("增强体质");
        updatedProfile.setSessionsLast30Days(16);
        updatedProfile.setAvgDurationMinutes(35.0);
        updatedProfile.setSessionsLastWeek(4);
        updatedProfile.setSessionsPreviousWeek(3);
        updatedProfile.setLastSessionDate("2026-04-14");
        updatedProfile.setLastCompletionRate(0.85);
        updatedProfile.setStage(newStage);

        TrainingPlanRecord plan2 = trainingPlanService.generateTrainingPlan(updatedProfile);
        assertNotNull(plan2);
        assertEquals(newStage, plan2.getStage());

        if (newStage == UserStage.GROWTH)
        {
            assertEquals("v2_encouraging", plan2.getSystemPromptVersion());
        }
        else if (newStage == UserStage.ADVANCED)
        {
            assertEquals("v3_data_driven", plan2.getSystemPromptVersion());
        }

        // --- Step 8: Verify cost tracking ---
        var costSummary = claudeEngine.getDailyCostSummary(now.toLocalDate().toString());
        assertNotNull(costSummary, "Cost summary should exist");
        assertTrue((int) costSummary.get("total_calls") >= 2, "Should have >= 2 calls logged");

        // --- Step 9: Verify multiple plans ---
        List<TrainingPlanRecord> allPlans = trainingPlanService.getUserPlans(userId, 10);
        assertEquals(2, allPlans.size(), "User should have 2 plans");

        TrainingPlanRecord latestPlan = trainingPlanService.getLatestPlan(userId);
        assertEquals(plan2.getPlanId(), latestPlan.getPlanId());
    }

    // ========== Test 2: Plateau Detection and Recovery ==========

    @Test
    @DisplayName("E2E: User hits plateau → detection → adapted plan → recovery")
    void plateauDetectionAndRecovery()
    {
        String userId = "user_plateau_001";
        LocalDateTime now = LocalDateTime.now();

        // 10 days inactive + declining intensity = PLATEAU
        UserTrainingData plateauData = new UserTrainingData();
        plateauData.setUserId(userId);
        plateauData.setTenureWeeks(10.0);
        plateauData.setWeeklyFrequency(1.5);
        plateauData.setCompletionRate(0.5);
        plateauData.setLastSessionDate(now.minusDays(10));
        plateauData.setIntensityTrend(-0.05);
        plateauData.setTotalSessions(25);

        UserStage plateauStage = userStageService.identifyStage(plateauData);
        assertEquals(UserStage.PLATEAU, plateauStage,
            "10 days inactive = PLATEAU");

        UserProfileInput profile = new UserProfileInput();
        profile.setUserId(userId);
        profile.setName("李四");
        profile.setAge(42);
        profile.setGender("女");
        profile.setCity("宁津县");
        profile.setDeviceType("动感单车");
        profile.setDeviceModel("SP-500");
        profile.setGoal("减脂塑形");
        profile.setSessionsLast30Days(4);
        profile.setAvgDurationMinutes(20.0);
        profile.setSessionsLastWeek(0);
        profile.setSessionsPreviousWeek(1);
        profile.setLastSessionDate("2026-04-04");
        profile.setLastCompletionRate(0.5);
        profile.setStage(plateauStage);

        TrainingPlanRecord plateauPlan = trainingPlanService.generateTrainingPlan(profile);
        assertEquals(UserStage.PLATEAU, plateauPlan.getStage());
        assertEquals("v2_encouraging", plateauPlan.getSystemPromptVersion(),
            "PLATEAU uses v2_encouraging (supportive tone)");

        StageBehavior behavior = userStageService.getStageBehavior(UserStage.PLATEAU);
        assertNotNull(behavior, "Stage behavior should be defined for PLATEAU");
        assertEquals("reengagement", behavior.getFeedbackStyle());
        assertEquals("churn_prevention", behavior.getIncentiveType());

        // Simulate recovery: recent session + higher frequency + positive trend
        UserTrainingData recoveredData = new UserTrainingData();
        recoveredData.setUserId(userId);
        recoveredData.setTenureWeeks(12.0);
        recoveredData.setWeeklyFrequency(3.5);
        recoveredData.setCompletionRate(0.80);
        recoveredData.setLastSessionDate(now.minusDays(1));
        recoveredData.setIntensityTrend(0.10);
        recoveredData.setTotalSessions(32);

        UserStage recoveredStage = userStageService.identifyStage(recoveredData);
        assertNotEquals(UserStage.PLATEAU, recoveredStage,
            "Recovered user should no longer be PLATEAU");
    }

    // ========== Test 3: A/B Testing Variant Override ==========

    @Test
    @DisplayName("E2E: A/B test variant overrides stage-default prompt selection")
    void abTestVariantOverride()
    {
        String userId = "user_ab_001";

        UserProfileInput profile = new UserProfileInput();
        profile.setUserId(userId);
        profile.setName("王五");
        profile.setAge(28);
        profile.setStage(UserStage.BEGINNER);
        profile.setSessionsLast30Days(2);

        TrainingPlanRecord plan = trainingPlanService.generateTrainingPlan(
            profile, null, "v3_data_driven");

        assertEquals("v3_data_driven", plan.getSystemPromptVersion(),
            "A/B variant overrides stage-default");
        assertEquals(UserStage.BEGINNER, plan.getStage(),
            "Stage recorded independently of prompt variant");
    }

    // ========== Test 4: Multi-User Cost Tracking ==========

    @Test
    @DisplayName("E2E: Multiple users → separate call logs and cost tracking")
    void multiUserCostTracking()
    {
        String user1 = "user_cost_001";
        String user2 = "user_cost_002";

        for (String uid : List.of(user1, user2))
        {
            UserProfileInput profile = new UserProfileInput();
            profile.setUserId(uid);
            profile.setName("用户" + uid);
            profile.setAge(30);
            profile.setStage(UserStage.BEGINNER);
            trainingPlanService.generateTrainingPlan(profile);
        }

        var logs1 = claudeEngine.getCallLogs(user1, null, null, 10);
        var logs2 = claudeEngine.getCallLogs(user2, null, null, 10);
        assertEquals(1, logs1.size(), "User 1 should have 1 call");
        assertEquals(1, logs2.size(), "User 2 should have 1 call");

        Map<String, Object> cacheStats = claudeEngine.getCacheStats();
        assertNotNull(cacheStats, "Cache stats should be available");
        assertEquals(2, cacheStats.get("total_logs"));
    }

    // ========== Test 5: Low Compliance Alert Flow ==========

    @Test
    @DisplayName("E2E: Low compliance → alerts → feedback captured for analysis")
    void lowComplianceAlertFlow()
    {
        String userId = "user_low_comp_001";
        LocalDateTime now = LocalDateTime.now();

        // Set up active prescriptions and record an old execution (5 days ago)
        // This makes daysWithout=5 >= LOW_COMPLIANCE_THRESHOLD_DAYS(3) → LOW
        complianceService.setActivePrescriptionCount(userId, 5);
        LocalDateTime fiveDaysAgo = now.minusDays(5);
        System.out.println("[DEBUG] Recording execution at: " + fiveDaysAgo + " (date: " + fiveDaysAgo.toLocalDate() + ")");
        complianceService.recordInterventionExecution(userId, "intv_old", fiveDaysAgo);
        System.out.println("[DEBUG] Today: " + java.time.LocalDate.now());

        // Record partial compliance feedback (timestamp aligned with the old execution
        // so it doesn't override lastExecution with "now")
        FeedbackEntry lowFeedback = new FeedbackEntry();
        lowFeedback.setUserId(userId);
        lowFeedback.setInterventionId("intv_old");
        lowFeedback.setFeedbackType(FeedbackType.COMPLIANCE);
        lowFeedback.setTimestamp(java.sql.Timestamp.valueOf(fiveDaysAgo));
        lowFeedback.setComplianceStatus(ComplianceStatus.PARTIAL);
        lowFeedback.setComplianceDetail(0.3);
        lowFeedback.setRpe(8);
        lowFeedback.setSubjectiveOutcome("fatigued");
        lowFeedback.setEnergyLevel(3);
        complianceService.recordFeedback(lowFeedback);

        // Debug: check compliance status directly
        var status = complianceService.getUserCompliance(userId);
        System.out.println("[DEBUG] complianceLevel=" + status.getComplianceLevel()
            + ", daysWithout=" + status.getDaysWithoutIntervention()
            + ", activePrescriptions=" + status.getActivePrescriptionCount()
            + ", reason=" + status.getLevelReason()
            + ", now=" + java.time.LocalDateTime.now().toLocalDate()
            + ", execDate=" + now.minusDays(5).toLocalDate());

        // Verify low compliance alert fires
        var alerts = complianceService.getLowComplianceAlerts(List.of(userId));
        assertFalse(alerts.isEmpty(), "Low compliance (5 days inactive) should generate alert");
        assertEquals(ComplianceLevel.LOW, alerts.get(0).getComplianceLevel());

        // Verify feedback was recorded
        var feedbackHistory = complianceService.getFeedbackHistory(userId, null, null, 10);
        assertFalse(feedbackHistory.isEmpty(), "Feedback should be recorded");
        assertEquals(ComplianceStatus.PARTIAL, feedbackHistory.get(0).getComplianceStatus());
    }

    // ========== Test 6: Health Score Provides Plan Context ==========

    @Test
    @DisplayName("E2E: Health score report with dimension breakdown")
    void healthScoreProvidesPlanContext()
    {
        String userId = "user_score_001";

        HealthScoreInput goodHealth = new HealthScoreInput();
        goodHealth.setDailySteps(10000);
        goodHealth.setWeeklyExerciseMinutes(180.0);
        goodHealth.setRestingHr(60);
        goodHealth.setSleepDuration(7.5);
        goodHealth.setSleepEfficiency(0.90);

        var goodReport = healthScoreService.calculate(userId, goodHealth);
        assertTrue(goodReport.getCompositeScore() >= 70,
            "Good health metrics should score >= 70, got: " + goodReport.getCompositeScore());
        assertNotNull(goodReport.getDimensionResults());
        assertFalse(goodReport.getDimensionResults().isEmpty());

        HealthScoreInput poorHealth = new HealthScoreInput();
        poorHealth.setDailySteps(1500);
        poorHealth.setWeeklyExerciseMinutes(0.0);
        poorHealth.setRestingHr(95);
        poorHealth.setSleepDuration(4.0);
        poorHealth.setSleepEfficiency(0.50);

        var poorReport = healthScoreService.calculate(userId, poorHealth);
        assertTrue(poorReport.getCompositeScore() < goodReport.getCompositeScore(),
            "Poor health (" + poorReport.getCompositeScore() + ") should score lower than good (" + goodReport.getCompositeScore() + ")");
    }

    // ========== Test 7: Stage Transition History ==========

    @Test
    @DisplayName("E2E: Stage transitions are tracked in history")
    void stageTransitionHistory()
    {
        String userId = "user_history_001";
        LocalDateTime now = LocalDateTime.now();

        // Set initial stage
        userStageService.setUserStage(userId, UserStage.BEGINNER, "test setup");

        // Transition to GROWTH
        UserTrainingData growthData = new UserTrainingData();
        growthData.setUserId(userId);
        growthData.setTenureWeeks(5.0);
        growthData.setWeeklyFrequency(3.0);
        growthData.setCompletionRate(0.75);
        growthData.setLastSessionDate(now.minusDays(1));
        growthData.setIntensityTrend(0.05);
        growthData.setTotalSessions(15);

        StageTransition t1 = userStageService.determineTransition(userId, growthData);
        assertNotNull(t1);
        assertEquals(UserStage.BEGINNER, t1.getFromStage());
        assertEquals(UserStage.GROWTH, t1.getToStage());

        var history = userStageService.getTransitionHistory(userId);
        assertEquals(2, history.size(), "Should have 2 transitions (setup + growth)");

        // Verify current stage
        assertEquals(UserStage.GROWTH, userStageService.getUserStage(userId));
    }
}
