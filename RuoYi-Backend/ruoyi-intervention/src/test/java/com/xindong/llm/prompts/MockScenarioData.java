package com.xindong.llm.prompts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock测试数据集 / Mock test data for all prompt scenarios.
 *
 * <p>Provides 5 typical user profiles (beginner, growth, plateau, advanced, recovery)
 * with pre-built PromptInput instances and expected JSON output for each scenario.
 *
 * <p>Usage:
 * <pre>
 *   MockScenarioData.UserProfile profile = MockScenarioData.getProfile("beginner");
 *   PromptInput input = profile.initialAssessmentInput();
 *   String expectedJson = profile.initialAssessmentOutput();
 * </pre>
 *
 * <p>Ticket: XIN-105
 */
public final class MockScenarioData
{
    // ========== User Profile IDs ==========
    public static final String BEGINNER = "beginner";
    public static final String GROWTH = "growth";
    public static final String PLATEAU = "plateau";
    public static final String ADVANCED = "advanced";
    public static final String RECOVERY = "recovery";

    private static final Map<String, UserProfile> PROFILES = new HashMap<>();

    static
    {
        PROFILES.put(BEGINNER, createBeginner());
        PROFILES.put(GROWTH, createGrowth());
        PROFILES.put(PLATEAU, createPlateau());
        PROFILES.put(ADVANCED, createAdvanced());
        PROFILES.put(RECOVERY, createRecovery());
    }

    private MockScenarioData() {} // utility class

    // ========== Public API ==========

    /** Get a user profile by key */
    public static UserProfile getProfile(String key)
    {
        return PROFILES.get(key);
    }

    /** Get all user profiles */
    public static Map<String, UserProfile> allProfiles()
    {
        return new HashMap<>(PROFILES);
    }

    /** Get all profile keys */
    public static List<String> profileKeys()
    {
        return new ArrayList<>(PROFILES.keySet());
    }

    // ========== User Profile Data Class ==========

    public static class UserProfile
    {
        private final String key;
        private final String labelZh;
        private final String userId;
        private final Map<String, Object> healthProfile;

        public UserProfile(String key, String labelZh, String userId, Map<String, Object> healthProfile)
        {
            this.key = key;
            this.labelZh = labelZh;
            this.userId = userId;
            this.healthProfile = healthProfile;
        }

        public String getKey() { return key; }
        public String getLabelZh() { return labelZh; }
        public String getUserId() { return userId; }
        public Map<String, Object> getHealthProfile() { return new HashMap<>(healthProfile); }

        // --- Input builders for each scenario ---

        /** Build input for INITIAL_ASSESSMENT scenario */
        public PromptInput initialAssessmentInput()
        {
            return new PromptInput(PromptScenario.INITIAL_ASSESSMENT, userId)
                .userStage(key)
                .healthProfile(healthProfile);
        }

        /** Build input for DAILY_TASK scenario */
        public PromptInput dailyTaskInput()
        {
            Map<String, Object> imuSummary = buildImuSummary(key);
            List<Map<String, Object>> history = buildRecentHistory(key);

            return new PromptInput(PromptScenario.DAILY_TASK, userId)
                .userStage(key)
                .imuSummary(imuSummary)
                .recentHistory(history);
        }

        /** Build input for PROGRESS_REVIEW scenario */
        public PromptInput progressReviewInput()
        {
            return new PromptInput(PromptScenario.PROGRESS_REVIEW, userId)
                .userStage(key)
                .healthScoreTrend(buildHealthScoreTrend(key))
                .compliance(buildCompliance(key));
        }

        /** Build input for PLAN_ADJUSTMENT scenario */
        public PromptInput planAdjustmentInput()
        {
            return new PromptInput(PromptScenario.PLAN_ADJUSTMENT, userId)
                .userStage(key)
                .currentPlan(buildCurrentPlan(key))
                .adjustmentTrigger(buildAdjustmentTrigger(key));
        }
    }

    // ========== Profile Constructors ==========

    private static UserProfile createBeginner()
    {
        Map<String, Object> hp = new HashMap<>();
        hp.put("age", 35);
        hp.put("gender", "male");
        hp.put("height", 175);
        hp.put("weight", 82);
        hp.put("bmi", 26.8);
        hp.put("restingHeartRate", 78);
        hp.put("exerciseHistory", "none");
        hp.put("goals", List.of("weight_loss", "basic_fitness"));
        hp.put("medicalNotes", "无心血管疾病史，轻度脂肪肝");
        hp.put("availableEquipment", List.of("treadmill", "stationary_bike"));

        return new UserProfile(BEGINNER, "初学者(新手适应期)", "user-beginner-001", hp);
    }

    private static UserProfile createGrowth()
    {
        Map<String, Object> hp = new HashMap<>();
        hp.put("age", 28);
        hp.put("gender", "female");
        hp.put("height", 163);
        hp.put("weight", 58);
        hp.put("bmi", 21.8);
        hp.put("restingHeartRate", 68);
        hp.put("exerciseHistory", "3次/周跑步，持续8周");
        hp.put("goals", List.of("endurance", "strength"));
        hp.put("medicalNotes", "无特殊病史");
        hp.put("availableEquipment", List.of("treadmill", "elliptical", "dumbbells"));

        return new UserProfile(GROWTH, "进阶者(提升期)", "user-growth-001", hp);
    }

    private static UserProfile createPlateau()
    {
        Map<String, Object> hp = new HashMap<>();
        hp.put("age", 42);
        hp.put("gender", "male");
        hp.put("height", 178);
        hp.put("weight", 80);
        hp.put("bmi", 25.2);
        hp.put("restingHeartRate", 72);
        hp.put("exerciseHistory", "2次/周跑步+力量，持续6个月，近1个月无进展");
        hp.put("goals", List.of("muscle_gain", "break_plateau"));
        hp.put("medicalNotes", "膝关节轻度磨损");
        hp.put("availableEquipment", List.of("treadmill", "leg_press", "chest_press", "lat_pulldown"));

        return new UserProfile(PLATEAU, "停滞期", "user-plateau-001", hp);
    }

    private static UserProfile createAdvanced()
    {
        Map<String, Object> hp = new HashMap<>();
        hp.put("age", 25);
        hp.put("gender", "male");
        hp.put("height", 180);
        hp.put("weight", 75);
        hp.put("bmi", 23.1);
        hp.put("restingHeartRate", 55);
        hp.put("exerciseHistory", "5次/周混合训练，持续18个月");
        hp.put("goals", List.of("performance", "competition_prep"));
        hp.put("medicalNotes", "无");
        hp.put("availableEquipment", List.of("all_gym_equipment"));

        return new UserProfile(ADVANCED, "高级者(突破期)", "user-advanced-001", hp);
    }

    private static UserProfile createRecovery()
    {
        Map<String, Object> hp = new HashMap<>();
        hp.put("age", 50);
        hp.put("gender", "female");
        hp.put("height", 160);
        hp.put("weight", 65);
        hp.put("bmi", 25.4);
        hp.put("restingHeartRate", 74);
        hp.put("exerciseHistory", "曾经规律运动，因腰椎间盘突出停训3个月");
        hp.put("goals", List.of("rehab", "gradual_return"));
        hp.put("medicalNotes", "L4-L5腰椎间盘突出，已过急性期，医生允许低强度运动");
        hp.put("availableEquipment", List.of("stationary_bike", "yoga_mat", "resistance_bands"));

        return new UserProfile(RECOVERY, "恢复期", "user-recovery-001", hp);
    }

    // ========== Scenario-Specific Data Builders ==========

    private static Map<String, Object> buildImuSummary(String stage)
    {
        Map<String, Object> imu = new HashMap<>();
        imu.put("deviceType", "GY-BLE25T");
        imu.put("protocol", "GATT_FFE4_Notify");

        switch (stage)
        {
            case BEGINNER:
                imu.put("restingHeartRate", 78);
                imu.put("sleepQuality", 0.6);
                imu.put("stepsYesterday", 3200);
                imu.put("readinessScore", 55);
                imu.put("note", "睡眠一般，准备度偏低");
                break;
            case GROWTH:
                imu.put("restingHeartRate", 66);
                imu.put("sleepQuality", 0.8);
                imu.put("stepsYesterday", 8500);
                imu.put("readinessScore", 82);
                imu.put("note", "恢复良好，可进行中等强度训练");
                break;
            case PLATEAU:
                imu.put("restingHeartRate", 72);
                imu.put("sleepQuality", 0.7);
                imu.put("stepsYesterday", 5100);
                imu.put("readinessScore", 65);
                imu.put("note", "正常状态");
                break;
            case ADVANCED:
                imu.put("restingHeartRate", 52);
                imu.put("sleepQuality", 0.9);
                imu.put("stepsYesterday", 12000);
                imu.put("readinessScore", 92);
                imu.put("note", "恢复充分，可进行高强度训练");
                break;
            case RECOVERY:
                imu.put("restingHeartRate", 74);
                imu.put("sleepQuality", 0.5);
                imu.put("stepsYesterday", 1800);
                imu.put("readinessScore", 40);
                imu.put("note", "准备度不足，建议极轻度活动");
                break;
            default:
                imu.put("readinessScore", 50);
        }
        return imu;
    }

    private static List<Map<String, Object>> buildRecentHistory(String stage)
    {
        List<Map<String, Object>> history = new ArrayList<>();

        switch (stage)
        {
            case BEGINNER:
                history.add(session("2026-04-13", "walking", 20, "light", 180));
                history.add(session("2026-04-11", "stationary_bike", 15, "light", 120));
                history.add(session("2026-04-09", "walking", 10, "light", 90));
                break;
            case GROWTH:
                history.add(session("2026-04-14", "running", 35, "moderate", 380));
                history.add(session("2026-04-12", "elliptical", 40, "moderate", 420));
                history.add(session("2026-04-10", "running", 30, "moderate", 340));
                history.add(session("2026-04-08", "dumbbells", 25, "moderate", 200));
                break;
            case PLATEAU:
                history.add(session("2026-04-13", "running", 30, "moderate", 320));
                history.add(session("2026-04-10", "leg_press", 20, "moderate", 180));
                history.add(session("2026-04-07", "chest_press", 20, "moderate", 170));
                break;
            case ADVANCED:
                history.add(session("2026-04-14", "hiit", 45, "vigorous", 550));
                history.add(session("2026-04-13", "strength_training", 60, "vigorous", 400));
                history.add(session("2026-04-12", "running", 40, "vigorous", 480));
                history.add(session("2026-04-11", "swimming", 50, "vigorous", 500));
                history.add(session("2026-04-10", "strength_training", 55, "vigorous", 380));
                break;
            case RECOVERY:
                history.add(session("2026-04-12", "stationary_bike", 10, "light", 60));
                history.add(session("2026-04-09", "stretching", 15, "light", 40));
                break;
        }
        return history;
    }

    private static Map<String, Object> session(String date, String type, int duration, String intensity, int calories)
    {
        Map<String, Object> s = new HashMap<>();
        s.put("date", date);
        s.put("exerciseType", type);
        s.put("durationMinutes", duration);
        s.put("intensity", intensity);
        s.put("caloriesBurned", calories);
        s.put("completed", true);
        return s;
    }

    private static Map<String, Object> buildHealthScoreTrend(String stage)
    {
        Map<String, Object> trend = new HashMap<>();
        trend.put("currentScore", stageScore(stage));
        trend.put("previousScore", stageScore(stage) - stageDelta(stage));
        trend.put("periodWeeks", 4);

        List<Integer> weekly = new ArrayList<>();
        int base = stageScore(stage) - stageDelta(stage) * 3;
        for (int i = 0; i < 4; i++)
        {
            weekly.add(base + stageDelta(stage) * i);
        }
        trend.put("weeklyScores", weekly);
        return trend;
    }

    private static int stageScore(String stage)
    {
        switch (stage)
        {
            case BEGINNER: return 45;
            case GROWTH: return 68;
            case PLATEAU: return 58;
            case ADVANCED: return 85;
            case RECOVERY: return 35;
            default: return 50;
        }
    }

    private static int stageDelta(String stage)
    {
        switch (stage)
        {
            case BEGINNER: return 3;
            case GROWTH: return 5;
            case PLATEAU: return 0;
            case ADVANCED: return 2;
            case RECOVERY: return -2;
            default: return 1;
        }
    }

    private static Map<String, Object> buildCompliance(String stage)
    {
        Map<String, Object> c = new HashMap<>();
        c.put("periodWeeks", 4);

        switch (stage)
        {
            case BEGINNER:
                c.put("plannedSessions", 12);
                c.put("completedSessions", 5);
                c.put("completionRate", 0.42);
                c.put("avgDurationMinutes", 15);
                c.put("note", "完成率偏低，需要降低门槛");
                break;
            case GROWTH:
                c.put("plannedSessions", 16);
                c.put("completedSessions", 13);
                c.put("completionRate", 0.81);
                c.put("avgDurationMinutes", 33);
                c.put("note", "稳定高完成率");
                break;
            case PLATEAU:
                c.put("plannedSessions", 12);
                c.put("completedSessions", 7);
                c.put("completionRate", 0.58);
                c.put("avgDurationMinutes", 25);
                c.put("note", "完成率中等，训练强度停滞");
                break;
            case ADVANCED:
                c.put("plannedSessions", 20);
                c.put("completedSessions", 19);
                c.put("completionRate", 0.95);
                c.put("avgDurationMinutes", 50);
                c.put("note", "极高完成率和强度");
                break;
            case RECOVERY:
                c.put("plannedSessions", 8);
                c.put("completedSessions", 3);
                c.put("completionRate", 0.38);
                c.put("avgDurationMinutes", 12);
                c.put("note", "恢复期低频，遵医嘱");
                break;
        }
        return c;
    }

    private static Map<String, Object> buildCurrentPlan(String stage)
    {
        Map<String, Object> plan = new HashMap<>();

        switch (stage)
        {
            case BEGINNER:
                plan.put("weeklyFrequency", 3);
                plan.put("sessionDurationMinutes", 20);
                plan.put("intensity", "light");
                plan.put("exerciseTypes", List.of("walking", "stationary_bike"));
                break;
            case GROWTH:
                plan.put("weeklyFrequency", 4);
                plan.put("sessionDurationMinutes", 35);
                plan.put("intensity", "moderate");
                plan.put("exerciseTypes", List.of("running", "elliptical", "dumbbells"));
                break;
            case PLATEAU:
                plan.put("weeklyFrequency", 3);
                plan.put("sessionDurationMinutes", 30);
                plan.put("intensity", "moderate");
                plan.put("exerciseTypes", List.of("running", "leg_press", "chest_press"));
                break;
            case ADVANCED:
                plan.put("weeklyFrequency", 5);
                plan.put("sessionDurationMinutes", 55);
                plan.put("intensity", "vigorous");
                plan.put("exerciseTypes", List.of("hiit", "strength_training", "running", "swimming"));
                break;
            case RECOVERY:
                plan.put("weeklyFrequency", 2);
                plan.put("sessionDurationMinutes", 15);
                plan.put("intensity", "light");
                plan.put("exerciseTypes", List.of("stationary_bike", "stretching", "resistance_bands"));
                break;
        }
        return plan;
    }

    private static Map<String, Object> buildAdjustmentTrigger(String stage)
    {
        Map<String, Object> trigger = new HashMap<>();

        switch (stage)
        {
            case BEGINNER:
                trigger.put("type", "low_compliance");
                trigger.put("detail", "过去4周完成率42%，低于50%阈值");
                trigger.put("suggestedAction", "reduce_frequency_and_duration");
                break;
            case GROWTH:
                trigger.put("type", "positive_adaptation");
                trigger.put("detail", "连续4周完成率>75%，可以安全增加强度");
                trigger.put("suggestedAction", "increment_intensity");
                break;
            case PLATEAU:
                trigger.put("type", "performance_plateau");
                trigger.put("detail", "近4周训练强度无增长，健康分停滞在58分");
                trigger.put("suggestedAction", "increase_variety");
                break;
            case ADVANCED:
                trigger.put("type", "optimization_request");
                trigger.put("detail", "用户要求增加爆发力训练");
                trigger.put("suggestedAction", "add_plyometrics");
                break;
            case RECOVERY:
                trigger.put("type", "medical_clearance");
                trigger.put("detail", "医生已许可恢复低强度运动");
                trigger.put("suggestedAction", "gradual_reintroduction");
                break;
        }
        return trigger;
    }
}
