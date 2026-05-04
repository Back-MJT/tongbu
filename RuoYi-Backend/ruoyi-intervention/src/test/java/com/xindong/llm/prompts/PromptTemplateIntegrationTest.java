package com.xindong.llm.prompts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Prompt模板集成测试 / Integration tests for prompt templates with mock scenario data.
 *
 * <p>Validates:
 * <ul>
 *   <li>All 4 scenarios can render system+user prompts for all 5 user profiles</li>
 *   <li>Expected output schemas are valid JSON descriptions</li>
 *   <li>PromptInput produces correct toPromptString() output</li>
 *   <li>PromptTemplateRegistry contains all expected templates</li>
 *   <li>PromptOutputParser correctly extracts JSON from LLM-like output</li>
 * </ul>
 *
 * <p>Ticket: XIN-105
 */
class PromptTemplateIntegrationTest
{
    private List<String> profileKeys;

    @BeforeEach
    void setUp()
    {
        profileKeys = MockScenarioData.profileKeys();
        // Ensure we have exactly 5 profiles
        assertEquals(5, profileKeys.size(), "Should have exactly 5 user profiles");
    }

    // ========== Registry Tests ==========

    @Nested
    @DisplayName("PromptTemplateRegistry")
    class RegistryTests
    {
        @Test
        @DisplayName("Registry has all 4 scenario templates")
        void test_registry_has_all_scenarios()
        {
            for (PromptScenario scenario : PromptScenario.values())
            {
                assertTrue(PromptTemplateRegistry.has(scenario),
                    "Registry should have template for " + scenario);
            }
            assertEquals(4, PromptTemplateRegistry.size());
        }

        @Test
        @DisplayName("getByCode works for all scenario codes")
        void test_get_by_code()
        {
            assertNotNull(PromptTemplateRegistry.getByCode("initial_assessment"));
            assertNotNull(PromptTemplateRegistry.getByCode("daily_task"));
            assertNotNull(PromptTemplateRegistry.getByCode("progress_review"));
            assertNotNull(PromptTemplateRegistry.getByCode("plan_adjustment"));
        }

        @Test
        @DisplayName("Unknown code defaults to INITIAL_ASSESSMENT")
        void test_unknown_code()
        {
            PromptTemplate t = PromptTemplateRegistry.getByCode("nonexistent");
            assertNotNull(t);
            assertEquals(PromptScenario.INITIAL_ASSESSMENT, t.scenario());
        }
    }

    // ========== Scenario Rendering Tests ==========

    @Nested
    @DisplayName("Initial Assessment Scenario")
    class InitialAssessmentTests
    {
        @Test
        @DisplayName("All 5 profiles render valid prompts")
        void test_all_profiles_render()
        {
            PromptTemplate template = PromptTemplateRegistry.get(PromptScenario.INITIAL_ASSESSMENT);
            assertNotNull(template);

            String systemPrompt = template.systemPrompt();
            assertTrue(systemPrompt.contains("ACSM"), "System prompt should reference ACSM guidelines");
            assertTrue(systemPrompt.contains("JSON"), "System prompt should specify JSON output");

            for (String key : profileKeys)
            {
                MockScenarioData.UserProfile profile = MockScenarioData.getProfile(key);
                PromptInput input = profile.initialAssessmentInput();

                assertEquals(PromptScenario.INITIAL_ASSESSMENT, input.getScenario());
                assertNotNull(input.getUserId());

                String userPrompt = template.userPrompt(input);
                assertFalse(userPrompt.isEmpty(), "User prompt should not be empty for " + key);
                assertTrue(userPrompt.contains("健康画像"), "Should include health profile section");
            }
        }
    }

    @Nested
    @DisplayName("Daily Task Scenario")
    class DailyTaskTests
    {
        @Test
        @DisplayName("All 5 profiles render with IMU data")
        void test_all_profiles_render()
        {
            PromptTemplate template = PromptTemplateRegistry.get(PromptScenario.DAILY_TASK);
            assertNotNull(template);

            String systemPrompt = template.systemPrompt();
            assertTrue(systemPrompt.contains("Karvonen"), "Should reference Karvonen formula");
            assertTrue(systemPrompt.contains("10%"), "Should reference 10% overload principle");

            for (String key : profileKeys)
            {
                MockScenarioData.UserProfile profile = MockScenarioData.getProfile(key);
                PromptInput input = profile.dailyTaskInput();

                assertEquals(PromptScenario.DAILY_TASK, input.getScenario());

                String userPrompt = template.userPrompt(input);
                assertFalse(userPrompt.isEmpty(), "User prompt should not be empty for " + key);
                assertTrue(userPrompt.contains("IMU"), "Should include IMU data section");
            }
        }
    }

    @Nested
    @DisplayName("Progress Review Scenario")
    class ProgressReviewTests
    {
        @Test
        @DisplayName("All 5 profiles render with trend data")
        void test_all_profiles_render()
        {
            PromptTemplate template = PromptTemplateRegistry.get(PromptScenario.PROGRESS_REVIEW);
            assertNotNull(template);

            String systemPrompt = template.systemPrompt();
            assertTrue(systemPrompt.contains("趋势"), "Should discuss trend analysis");
            assertTrue(systemPrompt.contains("阶段转换"), "Should discuss stage transitions");

            for (String key : profileKeys)
            {
                MockScenarioData.UserProfile profile = MockScenarioData.getProfile(key);
                PromptInput input = profile.progressReviewInput();

                assertEquals(PromptScenario.PROGRESS_REVIEW, input.getScenario());

                String userPrompt = template.userPrompt(input);
                assertFalse(userPrompt.isEmpty(), "User prompt should not be empty for " + key);
                assertTrue(userPrompt.contains("依从性") || userPrompt.contains("趋势"),
                    "Should include compliance or trend data");
            }
        }
    }

    @Nested
    @DisplayName("Plan Adjustment Scenario")
    class PlanAdjustmentTests
    {
        @Test
        @DisplayName("All 5 profiles render with trigger data")
        void test_all_profiles_render()
        {
            PromptTemplate template = PromptTemplateRegistry.get(PromptScenario.PLAN_ADJUSTMENT);
            assertNotNull(template);

            String systemPrompt = template.systemPrompt();
            assertTrue(systemPrompt.contains("Fogg"), "Should reference Fogg behavior model");

            for (String key : profileKeys)
            {
                MockScenarioData.UserProfile profile = MockScenarioData.getProfile(key);
                PromptInput input = profile.planAdjustmentInput();

                assertEquals(PromptScenario.PLAN_ADJUSTMENT, input.getScenario());

                String userPrompt = template.userPrompt(input);
                assertFalse(userPrompt.isEmpty(), "User prompt should not be empty for " + key);
                assertTrue(userPrompt.contains("当前训练计划"), "Should include current plan");
                assertTrue(userPrompt.contains("调整触发"), "Should include trigger reason");
            }
        }
    }

    // ========== Schema Validation ==========

    @Nested
    @DisplayName("Output Schema Validation")
    class SchemaTests
    {
        @Test
        @DisplayName("All schemas define required JSON fields")
        void test_schemas_define_required_fields()
        {
            PromptTemplate ia = PromptTemplateRegistry.get(PromptScenario.INITIAL_ASSESSMENT);
            assertTrue(ia.expectedOutputSchema().contains("stageAssessment"));
            assertTrue(ia.expectedOutputSchema().contains("trainingRecommendation"));
            assertTrue(ia.expectedOutputSchema().contains("firstWeekPlan"));

            PromptTemplate dt = PromptTemplateRegistry.get(PromptScenario.DAILY_TASK);
            assertTrue(dt.expectedOutputSchema().contains("readinessAssessment"));
            assertTrue(dt.expectedOutputSchema().contains("exercises"));
            assertTrue(dt.expectedOutputSchema().contains("totalDurationMinutes"));

            PromptTemplate pr = PromptTemplateRegistry.get(PromptScenario.PROGRESS_REVIEW);
            assertTrue(pr.expectedOutputSchema().contains("summary"));
            assertTrue(pr.expectedOutputSchema().contains("stageTransition"));

            PromptTemplate pa = PromptTemplateRegistry.get(PromptScenario.PLAN_ADJUSTMENT);
            assertTrue(pa.expectedOutputSchema().contains("adjustmentType"));
            assertTrue(pa.expectedOutputSchema().contains("adjustedPlan"));
        }
    }

    // ========== Output Parser Tests ==========

    @Nested
    @DisplayName("PromptOutputParser")
    class ParserTests
    {
        @Test
        @DisplayName("Parses JSON from LLM output with markdown fences")
        void test_parse_markdown_fenced_json()
        {
            String llmOutput = "```json\n{\"stageAssessment\": {\"recommendedStage\": \"beginner\"}}\n```";
            Map<String, Object> parsed = PromptOutputParser.parseJson(llmOutput);
            assertTrue(parsed.containsKey("stageAssessment"));

            @SuppressWarnings("unchecked")
            Map<String, Object> assessment = (Map<String, Object>) parsed.get("stageAssessment");
            assertEquals("beginner", assessment.get("recommendedStage"));
        }

        @Test
        @DisplayName("Parses pure JSON")
        void test_parse_pure_json()
        {
            String json = "{\"date\": \"2026-04-15\", \"exercises\": []}";
            Map<String, Object> parsed = PromptOutputParser.parseJson(json);
            assertEquals("2026-04-15", parsed.get("date"));
        }

        @Test
        @DisplayName("Returns empty map for unparseable input")
        void test_parse_garbage()
        {
            Map<String, Object> parsed = PromptOutputParser.parseJson("not json at all");
            assertTrue(parsed.isEmpty());
        }

        @Test
        @DisplayName("getField extracts nested values")
        void test_get_field()
        {
            Map<String, Object> data = PromptOutputParser.parseJson(
                "{\"stageAssessment\": {\"recommendedStage\": \"growth\", \"confidence\": 0.85}}");

            assertEquals("growth", PromptOutputParser.getField(data, "stageAssessment.recommendedStage"));
            assertEquals(0.85, PromptOutputParser.getField(data, "stageAssessment.confidence"));
            assertNull(PromptOutputParser.getField(data, "stageAssessment.nonexistent"));
            assertNull(PromptOutputParser.getField(data, "nonexistent.path"));
        }

        @Test
        @DisplayName("validateRequired finds missing fields")
        void test_validate_required()
        {
            Map<String, Object> data = PromptOutputParser.parseJson(
                "{\"stageAssessment\": {}, \"trainingRecommendation\": {}}");

            List<String> missing = PromptOutputParser.validateRequired(data, "stageAssessment", "trainingRecommendation", "firstWeekPlan");
            assertEquals(1, missing.size());
            assertEquals("firstWeekPlan", missing.get(0));
        }
    }

    // ========== Cross-Scenario Profile Consistency ==========

    @Nested
    @DisplayName("Cross-Scenario Consistency")
    class ConsistencyTests
    {
        @Test
        @DisplayName("Same profile produces different prompts for different scenarios")
        void test_different_prompts_per_scenario()
        {
            MockScenarioData.UserProfile profile = MockScenarioData.getProfile(MockScenarioData.BEGINNER);

            PromptTemplate ia = PromptTemplateRegistry.get(PromptScenario.INITIAL_ASSESSMENT);
            PromptTemplate dt = PromptTemplateRegistry.get(PromptScenario.DAILY_TASK);
            PromptTemplate pr = PromptTemplateRegistry.get(PromptScenario.PROGRESS_REVIEW);
            PromptTemplate pa = PromptTemplateRegistry.get(PromptScenario.PLAN_ADJUSTMENT);

            String iaUser = ia.userPrompt(profile.initialAssessmentInput());
            String dtUser = dt.userPrompt(profile.dailyTaskInput());
            String prUser = pr.userPrompt(profile.progressReviewInput());
            String paUser = pa.userPrompt(profile.planAdjustmentInput());

            // All should be non-empty
            assertFalse(iaUser.isEmpty());
            assertFalse(dtUser.isEmpty());
            assertFalse(prUser.isEmpty());
            assertFalse(paUser.isEmpty());

            // System prompts should be different for each scenario
            assertNotEquals(ia.systemPrompt(), dt.systemPrompt());
            assertNotEquals(dt.systemPrompt(), pr.systemPrompt());
            assertNotEquals(pr.systemPrompt(), pa.systemPrompt());
        }

        @Test
        @DisplayName("All profiles have health profiles with required fields")
        void test_health_profiles_complete()
        {
            String[] requiredFields = {"age", "gender", "height", "weight", "bmi", "restingHeartRate",
                "exerciseHistory", "goals", "medicalNotes", "availableEquipment"};

            for (String key : profileKeys)
            {
                MockScenarioData.UserProfile profile = MockScenarioData.getProfile(key);
                Map<String, Object> hp = profile.getHealthProfile();

                for (String field : requiredFields)
                {
                    assertTrue(hp.containsKey(field),
                        "Profile " + key + " should have field " + field);
                }
            }
        }
    }
}
