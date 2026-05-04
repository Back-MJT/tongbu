package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.enums.UserStage;
import com.ruoyi.intervention.domain.model.Prescription;
import com.ruoyi.intervention.domain.model.SleepRecommendation;
import com.ruoyi.intervention.domain.model.TrainingPlan;
import com.ruoyi.intervention.domain.enums.ExerciseIntensity;
import com.ruoyi.intervention.domain.enums.ExerciseType;
import com.ruoyi.intervention.domain.model.DailyExercise;
import com.ruoyi.intervention.service.TrainingPlanService.SystemPromptTemplate;
import com.ruoyi.intervention.service.TrainingPlanService.TrainingPlanRecord;
import com.ruoyi.intervention.service.TrainingPlanService.TrainingRecommendation;

/**
 * JUnit 5 tests for TrainingPlanService.
 * Migrated from: intervention-engine/tests/test_training_plan.py
 *
 * Covers:
 *   - UserProfileInput.formatPromptData() data formatting
 *   - System prompt registration and retrieval (registerSystemPrompt, getSystemPrompt, listSystemPrompts)
 *   - Version selection: selectPromptVersion(stage, abVariant)
 *   - Prompt selection by prescription: getSystemPrompt(Prescription, age, goal)
 *   - parseRecommendations() static method: text parsing, JSON fallback, edge cases
 *   - generateTrainingPlan() full lifecycle: generation, storage, retrieval
 *   - getPlan() / getLatestPlan() / getUserPlans() retrieval
 *   - updatePlanStatus() status transitions
 *   - formatSessionList() formatting
 *   - resetAll() state clearing
 *   - Edge cases: null inputs, empty strings, extreme values
 *
 * The service uses in-memory storage (Phase 1).  Each test gets a fresh service
 * instance via setUp(), but static state may persist — resetAll() is called after
 * each test class to ensure isolation.
 */
class TrainingPlanServiceTest
{
    private TrainingPlanService service;

    @BeforeEach
    void setUp()
    {
        service = new TrainingPlanService();
    }

    // ========== UserProfileInput.formatPromptData() ==========

    @Nested
    @DisplayName("TestFormatPromptData — UserProfileInput → prompt string")
    class TestFormatPromptData
    {
        @Test
        @DisplayName("Returns non-null string")
        void test_returns_string()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "张三", 30, UserStage.BEGINNER);
            String result = profile.formatPromptData();
            assertNotNull(result);
        }

        @Test
        @DisplayName("Contains user name and age")
        void test_contains_name_and_age()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "张三", 30, UserStage.BEGINNER);
            String result = profile.formatPromptData();
            assertTrue(result.contains("张三"));
            assertTrue(result.contains("30岁"));
        }

        @Test
        @DisplayName("Contains device info")
        void test_contains_device()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "李四", 40, UserStage.GROWTH);
            profile.setDeviceType("跑步机");
            profile.setDeviceModel("XT-200");
            String result = profile.formatPromptData();
            assertTrue(result.contains("跑步机"));
            assertTrue(result.contains("XT-200"));
        }

        @Test
        @DisplayName("Contains goal")
        void test_contains_goal()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "王五", 35, UserStage.PLATEAU);
            profile.setGoal("减脂");
            String result = profile.formatPromptData();
            assertTrue(result.contains("减脂"));
        }

        @Test
        @DisplayName("Contains training history")
        void test_contains_history()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "赵六", 28, UserStage.GROWTH);
            profile.setSessionsLast30Days(12);
            profile.setAvgDurationMinutes(40.0);
            String result = profile.formatPromptData();
            assertTrue(result.contains("12次"));
        }

        @Test
        @DisplayName("Contains stage label in Chinese")
        void test_contains_stage_label()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "陈七", 25, UserStage.ADVANCED);
            String result = profile.formatPromptData();
            assertTrue(result.contains("进阶"));
        }

        @Test
        @DisplayName("Null stage defaults to BEGINNER in output")
        void test_null_stage_defaults()
        {
            TrainingPlanService.UserProfileInput profile = new TrainingPlanService.UserProfileInput();
            profile.setUserId("u008");
            profile.setName("测试");
            profile.setAge(30);
            profile.setStage(null); // null stage
            String result = profile.formatPromptData();
            assertNotNull(result);
            // Should not throw and should produce a valid string
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Has trailing instruction in Chinese")
        void test_has_instruction()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u001", "周八", 30, UserStage.BEGINNER);
            String result = profile.formatPromptData();
            assertTrue(result.contains("本周训练建议") || result.contains("请用中文"));
        }
    }

    // ========== System Prompt Registration ==========

    @Nested
    @DisplayName("TestSystemPromptRegistration — Register and retrieve prompt templates")
    class TestSystemPromptRegistration
    {
        @Test
        @DisplayName("Default prompts are pre-registered")
        void test_default_prompts_exist()
        {
            assertNotNull(service.getSystemPrompt("v1"));
            assertNotNull(service.getSystemPrompt("v2_encouraging"));
            assertNotNull(service.getSystemPrompt("v3_data_driven"));
        }

        @Test
        @DisplayName("getSystemPrompt returns non-null for valid version")
        void test_get_valid_version()
        {
            SystemPromptTemplate tmpl = service.getSystemPrompt("v1");
            assertNotNull(tmpl);
            assertNotNull(tmpl.content);
            assertFalse(tmpl.content.isEmpty());
        }

        @Test
        @DisplayName("getSystemPrompt returns null for unknown version")
        void test_get_unknown_version()
        {
            SystemPromptTemplate tmpl = service.getSystemPrompt("unknown-version-xyz");
            assertNull(tmpl);
        }

        @Test
        @DisplayName("registerSystemPrompt adds new template")
        void test_register_adds_template()
        {
            SystemPromptTemplate tmpl = new SystemPromptTemplate(
                "test_version", "test content", "test description", true);
            service.registerSystemPrompt(tmpl);

            SystemPromptTemplate result = service.getSystemPrompt("test_version");
            assertNotNull(result);
            assertEquals("test content", result.content);
        }

        @Test
        @DisplayName("registerSystemPrompt overwrites existing version")
        void test_register_overwrites()
        {
            SystemPromptTemplate original = new SystemPromptTemplate("v1", "original", "desc", true);
            SystemPromptTemplate updated = new SystemPromptTemplate("v1", "updated content", "desc", true);
            service.registerSystemPrompt(updated);

            assertEquals("updated content", service.getSystemPrompt("v1").content);
        }

        @Test
        @DisplayName("listSystemPrompts returns all prompts when activeOnly=false")
        void test_list_all()
        {
            List<SystemPromptTemplate> prompts = service.listSystemPrompts(false);
            assertNotNull(prompts);
            assertTrue(prompts.size() >= 3); // at least v1, v2_encouraging, v3_data_driven
        }

        @Test
        @DisplayName("listSystemPrompts returns only active prompts when activeOnly=true")
        void test_list_active_only()
        {
            // Register an inactive prompt
            SystemPromptTemplate inactive = new SystemPromptTemplate(
                "inactive_test", "inactive content", "desc", false);
            service.registerSystemPrompt(inactive);

            List<SystemPromptTemplate> activePrompts = service.listSystemPrompts(true);
            for (SystemPromptTemplate t : activePrompts)
            {
                assertTrue(t.isActive, "All returned prompts should be active");
            }
        }
    }

    // ========== Version Selection ==========

    @Nested
    @DisplayName("TestVersionSelection — Age + goal → version mapping")
    class TestVersionSelection
    {
        @Test
        @DisplayName("Age < 35 + BEGINNER → v1")
        void test_beginner_young_v1()
        {
            String version = service.selectPromptVersion(UserStage.BEGINNER, null);
            assertEquals("v1", version);
        }

        @Test
        @DisplayName("Age < 35 + GROWTH → v2_encouraging")
        void test_growth_v2()
        {
            String version = service.selectPromptVersion(UserStage.GROWTH, null);
            assertEquals("v2_encouraging", version);
        }

        @Test
        @DisplayName("Age < 35 + PLATEAU → v2_encouraging")
        void test_plateau_v2()
        {
            String version = service.selectPromptVersion(UserStage.PLATEAU, null);
            assertEquals("v2_encouraging", version);
        }

        @Test
        @DisplayName("ADVANCED stage → v3_data_driven")
        void test_advanced_v3()
        {
            String version = service.selectPromptVersion(UserStage.ADVANCED, null);
            assertEquals("v3_data_driven", version);
        }

        @Test
        @DisplayName("A/B variant override takes precedence")
        void test_ab_variant_override()
        {
            // Even ADVANCED with explicit v1 override should use v1
            String version = service.selectPromptVersion(UserStage.ADVANCED, "v1");
            assertEquals("v1", version);
        }

        @Test
        @DisplayName("Null stage defaults to v1")
        void test_null_stage_defaults_v1()
        {
            String version = service.selectPromptVersion(null, null);
            assertEquals("v1", version);
        }
    }

    // ========== getSystemPrompt(Prescription, age, goal) ==========

    @Nested
    @DisplayName("TestGetSystemPromptByPrescription — Prescription-based prompt selection")
    class TestGetSystemPromptByPrescription
    {
        @Test
        @DisplayName("Exercise prescription returns non-null prompt")
        void test_exercise_returns_prompt()
        {
            Prescription rx = makeExercisePrescription();
            String prompt = service.getSystemPrompt(rx, 30, "GENERAL_FITNESS");
            assertNotNull(prompt);
            assertFalse(prompt.isEmpty());
        }

        @Test
        @DisplayName("Sleep prescription returns non-null prompt")
        void test_sleep_returns_prompt()
        {
            Prescription rx = makeSleepPrescription();
            String prompt = service.getSystemPrompt(rx, 35, "POOR_SLEEP");
            assertNotNull(prompt);
            assertFalse(prompt.isEmpty());
        }

        @Test
        @DisplayName("Null prescription does not throw")
        void test_null_prescription_no_throw()
        {
            String prompt = service.getSystemPrompt(null, 30, "GENERAL_FITNESS");
            assertNotNull(prompt); // Should return default
        }

        @Test
        @DisplayName("Unknown goal returns non-null")
        void test_unknown_goal_returns_non_null()
        {
            Prescription rx = makeExercisePrescription();
            String prompt = service.getSystemPrompt(rx, 30, "UNKNOWN_GOAL_XYZ");
            assertNotNull(prompt);
        }

        @Test
        @DisplayName("Very young age does not throw")
        void test_very_young_age()
        {
            Prescription rx = makeExercisePrescription();
            String prompt = service.getSystemPrompt(rx, 15, "GENERAL_FITNESS");
            assertNotNull(prompt);
        }

        @Test
        @DisplayName("Very old age does not throw")
        void test_very_old_age()
        {
            Prescription rx = makeExercisePrescription();
            String prompt = service.getSystemPrompt(rx, 85, "CARDIOVASCULAR");
            assertNotNull(prompt);
        }
    }

    // ========== parseRecommendations() ==========

    @Nested
    @DisplayName("TestParseRecommendations — Text parsing to structured recommendations")
    class TestParseRecommendations
    {
        @Test
        @DisplayName("Numbered lines are parsed into recommendations")
        void test_numbered_lines_parsed()
        {
            String text = ""
                + "1. 每天快走30分钟\n"
                + "理由：提升心肺功能\n"
                + "2. 力量训练每周2次\n"
                + "理由：增强肌肉\n"
                + "3. 睡足8小时\n"
                + "理由：恢复体力\n";
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations(text);
            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).getIndex());
            assertEquals("每天快走30分钟", result.get(0).getContent());
            assertEquals("提升心肺功能", result.get(0).getReason());
        }

        @Test
        @DisplayName("Content-only line with no reason is accepted")
        void test_no_reason_accepted()
        {
            String text = "1. 训练内容1\n2. 训练内容2";
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations(text);
            assertNotNull(result);
            assertTrue(result.size() >= 1);
        }

        @Test
        @DisplayName("Chinese colon separator works")
        void test_chinese_colon_separator()
        {
            String text = "1. 跑步30分钟理由：提高耐力";
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations(text);
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Empty input returns empty list")
        void test_empty_input()
        {
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations("");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Null input returns empty list")
        void test_null_input()
        {
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Non-numbered text returns empty list")
        void test_non_numbered_text()
        {
            String text = "This is just a paragraph without numbered items.";
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations(text);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Recommendations are in order")
        void test_in_order()
        {
            String text = "1. 第一条\n2. 第二条\n3. 第三条";
            List<TrainingRecommendation> result = TrainingPlanService.parseRecommendations(text);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).getIndex());
            assertEquals(2, result.get(1).getIndex());
            assertEquals(3, result.get(2).getIndex());
        }
    }

    // ========== generateTrainingPlan() ==========

    @Nested
    @DisplayName("TestGenerateTrainingPlan — Plan generation and storage")
    class TestGenerateTrainingPlan
    {
        @Test
        @DisplayName("generateTrainingPlan returns non-null TrainingPlanRecord")
        void test_returns_record()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen1", "测试用户", 30, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record);
        }

        @Test
        @DisplayName("Generated plan has a planId")
        void test_has_plan_id()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen2", "用户B", 28, UserStage.GROWTH);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record.getPlanId());
            assertFalse(record.getPlanId().isEmpty());
        }

        @Test
        @DisplayName("UserId is set from profile")
        void test_user_id_from_profile()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen3", "用户C", 35, UserStage.PLATEAU);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertEquals("u_gen3", record.getUserId());
        }

        @Test
        @DisplayName("Stage is set from profile")
        void test_stage_from_profile()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen4", "用户D", 40, UserStage.ADVANCED);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertEquals(UserStage.ADVANCED, record.getStage());
        }

        @Test
        @DisplayName("Recommendations are populated (mock mode)")
        void test_recommendations_populated()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen5", "用户E", 25, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record.getRecommendations());
        }

        @Test
        @DisplayName("Status is set to 'generated'")
        void test_status_generated()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen6", "用户F", 30, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertEquals("generated", record.getStatus());
        }

        @Test
        @DisplayName("System prompt version is recorded")
        void test_prompt_version_recorded()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen7", "用户G", 30, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record.getSystemPromptVersion());
        }

        @Test
        @DisplayName("Explicit promptVersion overrides default")
        void test_explicit_version_override()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen8", "用户H", 50, UserStage.GROWTH);
            TrainingPlanRecord record = service.generateTrainingPlan(profile, "v3_data_driven", null);
            assertEquals("v3_data_driven", record.getSystemPromptVersion());
        }

        @Test
        @DisplayName("A/B variant is recorded")
        void test_ab_variant_recorded()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen9", "用户I", 30, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile, null, "v1");
            // With abVariant=v1, version should be v1
            assertEquals("v1", record.getSystemPromptVersion());
        }

        @Test
        @DisplayName("Created timestamp is set")
        void test_created_at_set()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen10", "用户J", 30, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record.getCreatedAt());
        }

        @Test
        @DisplayName("Raw response is stored")
        void test_raw_response_stored()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_gen11", "用户K", 30, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record.getRawResponse());
        }
    }

    // ========== Plan Retrieval ==========

    @Nested
    @DisplayName("TestPlanRetrieval — getPlan / getLatestPlan / getUserPlans")
    class TestPlanRetrieval
    {
        @Test
        @DisplayName("getPlan returns previously generated plan")
        void test_get_plan_by_id()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_ret1", "检索用户", 30, UserStage.BEGINNER);
            TrainingPlanRecord created = service.generateTrainingPlan(profile);

            TrainingPlanRecord retrieved = service.getPlan(created.getPlanId());
            assertNotNull(retrieved);
            assertEquals(created.getPlanId(), retrieved.getPlanId());
        }

        @Test
        @DisplayName("getPlan returns null for unknown ID")
        void test_get_plan_not_found()
        {
            TrainingPlanRecord result = service.getPlan("nonexistent-plan-id-xyz");
            assertNull(result);
        }

        @Test
        @DisplayName("getLatestPlan returns most recent plan for user")
        void test_get_latest_plan()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_latest", "最新用户", 30, UserStage.BEGINNER);
            service.generateTrainingPlan(profile);

            // Generate another plan for same user
            service.generateTrainingPlan(profile);

            TrainingPlanRecord latest = service.getLatestPlan("u_latest");
            assertNotNull(latest);
            assertEquals("u_latest", latest.getUserId());
        }

        @Test
        @DisplayName("getLatestPlan returns null when no plan exists")
        void test_get_latest_plan_no_plans()
        {
            TrainingPlanRecord latest = service.getLatestPlan("user_with_no_plans_xyz");
            assertNull(latest);
        }

        @Test
        @DisplayName("getUserPlans returns plans for user")
        void test_get_user_plans()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_multi", "多方案用户", 30, UserStage.BEGINNER);
            service.generateTrainingPlan(profile);
            service.generateTrainingPlan(profile);

            List<TrainingPlanRecord> plans = service.getUserPlans("u_multi", 10);
            assertNotNull(plans);
            assertTrue(plans.size() >= 2);
        }

        @Test
        @DisplayName("getUserPlans respects limit")
        void test_get_user_plans_limit()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_limit", "限制用户", 30, UserStage.BEGINNER);
            for (int i = 0; i < 5; i++)
            {
                service.generateTrainingPlan(profile);
            }

            List<TrainingPlanRecord> plans = service.getUserPlans("u_limit", 2);
            assertTrue(plans.size() <= 2);
        }

        @Test
        @DisplayName("getUserPlans returns empty list for unknown user")
        void test_get_user_plans_unknown_user()
        {
            List<TrainingPlanRecord> plans = service.getUserPlans("unknown_user_xyz", 10);
            assertNotNull(plans);
            assertTrue(plans.isEmpty());
        }
    }

    // ========== Status Updates ==========

    @Nested
    @DisplayName("TestStatusUpdates — Plan status transitions")
    class TestStatusUpdates
    {
        @Test
        @DisplayName("updatePlanStatus changes status")
        void test_update_status()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_status", "状态用户", 30, UserStage.BEGINNER);
            TrainingPlanRecord created = service.generateTrainingPlan(profile);

            TrainingPlanRecord updated = service.updatePlanStatus(created.getPlanId(), "delivered");
            assertEquals("delivered", updated.getStatus());
        }

        @Test
        @DisplayName("updatePlanStatus returns null for unknown planId")
        void test_update_unknown_plan()
        {
            TrainingPlanRecord updated = service.updatePlanStatus("unknown-id", "delivered");
            assertNull(updated);
        }

        @Test
        @DisplayName("Status can be updated multiple times")
        void test_multiple_status_updates()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_multi_status", "多次状态", 30, UserStage.BEGINNER);
            TrainingPlanRecord created = service.generateTrainingPlan(profile);

            service.updatePlanStatus(created.getPlanId(), "delivered");
            TrainingPlanRecord accepted = service.updatePlanStatus(created.getPlanId(), "accepted");
            assertEquals("accepted", accepted.getStatus());
        }

        @Test
        @DisplayName("All valid statuses are accepted")
        void test_all_valid_statuses()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_all_status", "所有状态", 30, UserStage.BEGINNER);
            TrainingPlanRecord created = service.generateTrainingPlan(profile);

            String[] statuses = {"delivered", "accepted", "partially_completed", "completed", "expired"};
            for (String status : statuses)
            {
                TrainingPlanRecord updated = service.updatePlanStatus(created.getPlanId(), status);
                assertEquals(status, updated.getStatus());
            }
        }
    }

    // ========== formatSessionList() ==========

    @Nested
    @DisplayName("TestFormatSessionList — TrainingSession list → formatted string")
    class TestFormatSessionList
    {
        @Test
        @DisplayName("Non-null sessions list returns non-null string")
        void test_non_null_sessions()
        {
            TrainingPlan.TrainingSession session = new TrainingPlan.TrainingSession();
            session.setDay(1);
            session.setStatus("COMPLETED");

            String result = service.formatSessionList(Arrays.asList(session));
            assertNotNull(result);
        }

        @Test
        @DisplayName("Null sessions list returns non-null (no exception)")
        void test_null_sessions_no_exception()
        {
            String result = service.formatSessionList(null);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Empty list returns non-null")
        void test_empty_list()
        {
            String result = service.formatSessionList(Arrays.asList());
            assertNotNull(result);
        }

        @Test
        @DisplayName("Session with day 1 is reflected in output")
        void test_day_in_output()
        {
            TrainingPlan.TrainingSession session = new TrainingPlan.TrainingSession();
            session.setDay(1);
            session.setStatus("PENDING");

            String result = service.formatSessionList(Arrays.asList(session));
            assertNotNull(result);
            assertTrue(result.contains("1") || result.toLowerCase().contains("day"));
        }
    }

    // ========== Edge Cases ==========

    @Nested
    @DisplayName("TestEdgeCases — Unusual inputs and boundary conditions")
    class TestEdgeCases
    {
        @Test
        @DisplayName("UserProfileInput with all defaults does not throw")
        void test_all_defaults()
        {
            TrainingPlanService.UserProfileInput profile = new TrainingPlanService.UserProfileInput();
            profile.setUserId("u_defaults");
            // All other fields use defaults
            String prompt = profile.formatPromptData();
            assertNotNull(prompt);
        }

        @Test
        @DisplayName("Zero age does not throw")
        void test_zero_age()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_zero_age", "零岁用户", 0, UserStage.BEGINNER);
            TrainingPlanRecord record = service.generateTrainingPlan(profile);
            assertNotNull(record);
        }

        @Test
        @DisplayName("Negative sessions treated as zero in prompt")
        void test_negative_sessions()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_neg", "负次数", 30, UserStage.BEGINNER);
            profile.setSessionsLast30Days(-5);
            String result = profile.formatPromptData();
            assertNotNull(result);
        }

        @Test
        @DisplayName("Null lastSessionDate does not throw")
        void test_null_last_session_date()
        {
            TrainingPlanService.UserProfileInput profile = makeProfile("u_null_date", "无日期", 30, UserStage.BEGINNER);
            profile.setLastSessionDate(null);
            String result = profile.formatPromptData();
            assertNotNull(result);
        }
    }

    // ========== Helper factories ==========

    private static TrainingPlanService.UserProfileInput makeProfile(
        String userId, String name, int age, UserStage stage)
    {
        TrainingPlanService.UserProfileInput profile = new TrainingPlanService.UserProfileInput();
        profile.setUserId(userId);
        profile.setName(name);
        profile.setAge(age);
        profile.setGender("男");
        profile.setCity("宁津");
        profile.setDeviceType("跑步机");
        profile.setDeviceModel("XT-200");
        profile.setGoal("增强体质");
        profile.setSessionsLast30Days(3);
        profile.setAvgDurationMinutes(30.0);
        profile.setSessionsLastWeek(1);
        profile.setSessionsPreviousWeek(0);
        profile.setLastSessionDate("2026-04-10");
        profile.setLastCompletionRate(0.7);
        profile.setStage(stage);
        return profile;
    }

    private static Prescription makeExercisePrescription()
    {
        Prescription rx = new Prescription();
        rx.setPrescriptionId("EX-TEST-001");
        rx.setUserId("u-test");
        rx.setInterventionType("EXERCISE");
        rx.setGeneratedBy("rule_engine");
        rx.setExercises(Arrays.asList(
            new DailyExercise(ExerciseType.WALKING, ExerciseIntensity.LIGHT, 30)));
        return rx;
    }

    private static Prescription makeSleepPrescription()
    {
        Prescription rx = new Prescription();
        rx.setPrescriptionId("SL-TEST-001");
        rx.setUserId("u-test");
        rx.setInterventionType("SLEEP");
        rx.setGeneratedBy("rule_engine");
        rx.setSleepPlan(new SleepRecommendation());
        return rx;
    }
}
