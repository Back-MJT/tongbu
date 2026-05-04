package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.enums.TransitionTrigger;
import com.ruoyi.intervention.domain.enums.UserStage;
import com.ruoyi.intervention.domain.model.StageBehavior;
import com.ruoyi.intervention.domain.model.StageThresholds;
import com.ruoyi.intervention.domain.model.StageTransition;
import com.ruoyi.intervention.domain.model.UserTrainingData;

/**
 * JUnit 5 tests for UserStageService.
 * Tests user lifecycle stage identification and transitions.
 *
 * Migrated from: intervention-engine/src/algorithms/user_stage.py
 * Ticket: XIN-83
 *
 * Evaluation priority (first match wins):
 *   1. Plateau — inactivity (7+ days) or intensity decline
 *   2. Advanced — tenure >= 12w, freq >= 3, intensity trend >= 0.02
 *   3. Growth — tenure > 4w, freq >= 2, completion >= 70%
 *   4. Beginner — default
 */
class UserStageServiceTest
{
    private UserStageService service;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 13, 12, 0, 0);

    @BeforeEach
    void setUp()
    {
        service = new UserStageService();
    }

    // ========== identifyStage() ==========

    @Nested
    @DisplayName("IdentifyStage — Stage determination")
    class TestIdentifyStage
    {
        @Test
        @DisplayName("Default user → BEGINNER")
        void test_default_beginner()
        {
            UserTrainingData data = new UserTrainingData();
            data.setUserId("u1");
            // All defaults: tenure=0, freq=0, completion=0, no last session

            UserStage stage = service.identifyStage(data, null, NOW);
            assertEquals(UserStage.BEGINNER, stage);
        }

        @Test
        @DisplayName("Short tenure, low freq → BEGINNER")
        void test_early_beginner()
        {
            UserTrainingData data = newUser("u1", 2, 1, 0.3, NOW, 0.0);
            assertEquals(UserStage.BEGINNER, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Growth: tenure > 4w, freq >= 2, completion >= 0.7")
        void test_growth()
        {
            UserTrainingData data = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            assertEquals(UserStage.GROWTH, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Growth: exactly at thresholds (tenure=5, freq=2, comp=0.7)")
        void test_growth_boundary()
        {
            UserTrainingData data = newUser("u1", 5, 2.0, 0.7, NOW, 0.01);
            assertEquals(UserStage.GROWTH, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Advanced: tenure >= 12w, freq >= 3, intensity >= 0.02")
        void test_advanced()
        {
            UserTrainingData data = newUser("u1", 14, 3.5, 0.9, NOW, 0.05);
            assertEquals(UserStage.ADVANCED, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Advanced: exactly at thresholds")
        void test_advanced_boundary()
        {
            UserTrainingData data = newUser("u1", 12, 3.0, 0.5, NOW, 0.02);
            assertEquals(UserStage.ADVANCED, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Plateau: 7+ days inactive")
        void test_plateau_inactivity()
        {
            LocalDateTime lastSession = NOW.minusDays(8);
            UserTrainingData data = newUser("u1", 10, 3.0, 0.9, lastSession, 0.05);
            assertEquals(UserStage.PLATEAU, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Plateau: exactly 7 days inactive")
        void test_plateau_7_days()
        {
            LocalDateTime lastSession = NOW.minusDays(7);
            UserTrainingData data = newUser("u1", 10, 3.0, 0.9, lastSession, 0.05);
            assertEquals(UserStage.PLATEAU, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Not plateau: 6 days inactive")
        void test_not_plateau_6_days()
        {
            LocalDateTime lastSession = NOW.minusDays(6);
            UserTrainingData data = newUser("u1", 6, 2.5, 0.8, lastSession, 0.01);
            // Should be GROWTH, not PLATEAU
            assertEquals(UserStage.GROWTH, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Plateau: intensity declining")
        void test_plateau_intensity_decline()
        {
            // intensityTrend = -0.1 < -0.05 threshold, tenure > 4 weeks
            UserTrainingData data = newUser("u1", 8, 2.0, 0.7, NOW, -0.1);
            assertEquals(UserStage.PLATEAU, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Not plateau: intensity decline but tenure <= 4 weeks")
        void test_not_plateau_intensity_young()
        {
            UserTrainingData data = newUser("u1", 3, 1.0, 0.3, NOW, -0.1);
            // tenure=3 <= beginnerMaxWeeks(4), so plateau intensity check won't trigger
            assertEquals(UserStage.BEGINNER, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Plateau priority over advanced")
        void test_plateau_over_advanced()
        {
            // Would be advanced but inactive
            LocalDateTime lastSession = NOW.minusDays(10);
            UserTrainingData data = newUser("u1", 20, 4.0, 0.95, lastSession, 0.1);
            assertEquals(UserStage.PLATEAU, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Plateau priority over growth")
        void test_plateau_over_growth()
        {
            LocalDateTime lastSession = NOW.minusDays(10);
            UserTrainingData data = newUser("u1", 8, 3.0, 0.8, lastSession, 0.01);
            assertEquals(UserStage.PLATEAU, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("No last session date → plateau by inactivity skipped")
        void test_no_last_session()
        {
            UserTrainingData data = new UserTrainingData();
            data.setUserId("u1");
            data.setTenureWeeks(6);
            data.setWeeklyFrequency(2.5);
            data.setCompletionRate(0.8);
            data.setIntensityTrend(0.01);
            // lastSessionDate is null, so inactivity check is skipped

            assertEquals(UserStage.GROWTH, service.identifyStage(data, null, NOW));
        }

        @Test
        @DisplayName("Active today → last session is today")
        void test_active_today()
        {
            UserTrainingData data = newUser("u1", 14, 3.5, 0.9, NOW, 0.05);
            assertEquals(UserStage.ADVANCED, service.identifyStage(data, null, NOW));
        }
    }

    // ========== Custom Thresholds ==========

    @Nested
    @DisplayName("CustomThresholds — Custom threshold overrides")
    class TestCustomThresholds
    {
        @Test
        @DisplayName("Custom plateau inactivity: 14 days")
        void test_custom_plateau_days()
        {
            StageThresholds t = new StageThresholds();
            t.setPlateauInactivityDays(14);

            // 10 days → not plateau with custom threshold
            UserTrainingData data = newUser("u1", 8, 2.5, 0.8, NOW.minusDays(10), 0.01);
            assertEquals(UserStage.GROWTH, service.identifyStage(data, t, NOW));

            // 15 days → plateau
            UserTrainingData data2 = newUser("u1", 8, 2.5, 0.8, NOW.minusDays(15), 0.01);
            assertEquals(UserStage.PLATEAU, service.identifyStage(data2, t, NOW));
        }

        @Test
        @DisplayName("Custom advanced thresholds: higher requirements")
        void test_custom_advanced()
        {
            StageThresholds t = new StageThresholds();
            t.setAdvancedMinWeeks(20);
            t.setAdvancedMinFrequency(4.0);
            t.setAdvancedMinIntensityTrend(0.05);

            // Default would be advanced, custom is not
            UserTrainingData data = newUser("u1", 14, 3.5, 0.9, NOW, 0.03);
            UserStage stage = service.identifyStage(data, t, NOW);
            // tenure 14 < 20 → not advanced → likely GROWTH
            assertEquals(UserStage.GROWTH, stage);
        }

        @Test
        @DisplayName("Custom growth thresholds")
        void test_custom_growth()
        {
            StageThresholds t = new StageThresholds();
            t.setGrowthMinCompletion(0.9);
            t.setGrowthMinFrequency(3.0);

            // completion=0.8 < 0.9 → not growth with custom
            UserTrainingData data = newUser("u1", 6, 3.0, 0.8, NOW, 0.01);
            assertEquals(UserStage.BEGINNER, service.identifyStage(data, t, NOW));
        }
    }

    // ========== determineTransition() ==========

    @Nested
    @DisplayName("DetermineTransition — Stage transition tracking")
    class TestDetermineTransition
    {
        @Test
        @DisplayName("First transition from null to BEGINNER")
        void test_first_transition_beginner()
        {
            UserTrainingData data = new UserTrainingData();
            data.setUserId("u1");

            StageTransition t = service.determineTransition("u1", data, null, NOW);
            assertNotNull(t);
            assertNull(t.getFromStage());
            assertEquals(UserStage.BEGINNER, t.getToStage());
        }

        @Test
        @DisplayName("Transition BEGINNER → GROWTH")
        void test_beginner_to_growth()
        {
            // First: BEGINNER
            UserTrainingData d1 = new UserTrainingData();
            d1.setUserId("u1");
            service.determineTransition("u1", d1, null, NOW);

            // Then: GROWTH
            UserTrainingData d2 = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            StageTransition t = service.determineTransition("u1", d2, null, NOW);

            assertNotNull(t);
            assertEquals(UserStage.BEGINNER, t.getFromStage());
            assertEquals(UserStage.GROWTH, t.getToStage());
            assertEquals(TransitionTrigger.COMPLETION_RATE, t.getTrigger());
        }

        @Test
        @DisplayName("Transition GROWTH → PLATEAU (inactivity)")
        void test_growth_to_plateau()
        {
            // First: GROWTH
            UserTrainingData d1 = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            service.determineTransition("u1", d1, null, NOW);

            // Then: inactive → PLATEAU
            UserTrainingData d2 = newUser("u1", 8, 2.5, 0.8, NOW.minusDays(10), 0.01);
            StageTransition t = service.determineTransition("u1", d2, null, NOW);

            assertNotNull(t);
            assertEquals(UserStage.GROWTH, t.getFromStage());
            assertEquals(UserStage.PLATEAU, t.getToStage());
            assertEquals(TransitionTrigger.INACTIVITY_DAYS, t.getTrigger());
        }

        @Test
        @DisplayName("Transition GROWTH → ADVANCED")
        void test_growth_to_advanced()
        {
            // First: GROWTH
            UserTrainingData d1 = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            service.determineTransition("u1", d1, null, NOW);

            // Then: ADVANCED
            UserTrainingData d2 = newUser("u1", 14, 3.5, 0.9, NOW, 0.05);
            StageTransition t = service.determineTransition("u1", d2, null, NOW);

            assertNotNull(t);
            assertEquals(UserStage.GROWTH, t.getFromStage());
            assertEquals(UserStage.ADVANCED, t.getToStage());
            assertEquals(TransitionTrigger.TENURE_WEEKS, t.getTrigger());
        }

        @Test
        @DisplayName("No transition when stage unchanged")
        void test_no_transition_same_stage()
        {
            UserTrainingData d1 = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            service.determineTransition("u1", d1, null, NOW);

            UserTrainingData d2 = newUser("u1", 7, 2.6, 0.82, NOW, 0.01);
            StageTransition t = service.determineTransition("u1", d2, null, NOW);

            assertNull(t); // Still GROWTH
        }

        @Test
        @DisplayName("Transition record has evidence map")
        void test_transition_evidence()
        {
            UserTrainingData data = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            StageTransition t = service.determineTransition("u1", data, null, NOW);

            Map<String, Object> evidence = t.getEvidence();
            assertNotNull(evidence);
            assertEquals(6.0, evidence.get("tenure_weeks"));
            assertEquals(2.5, evidence.get("weekly_frequency"));
            assertEquals(0.8, evidence.get("completion_rate"));
            assertEquals(0.01, evidence.get("intensity_trend"));
        }

        @Test
        @DisplayName("Transition record has timestamp")
        void test_transition_timestamp()
        {
            UserTrainingData data = new UserTrainingData();
            data.setUserId("u1");
            StageTransition t = service.determineTransition("u1", data, null, NOW);
            assertEquals(NOW, t.getTimestamp());
        }
    }

    // ========== setUserStage() ==========

    @Nested
    @DisplayName("SetUserStage — Manual stage override")
    class TestSetUserStage
    {
        @Test
        @DisplayName("Manual override sets stage")
        void test_manual_override()
        {
            StageTransition t = service.setUserStage("u1", UserStage.ADVANCED, "Admin promotion");

            assertNotNull(t);
            assertEquals(UserStage.ADVANCED, t.getToStage());
            assertEquals(TransitionTrigger.MANUAL_OVERRIDE, t.getTrigger());
            assertEquals(UserStage.ADVANCED, service.getUserStage("u1"));
        }

        @Test
        @DisplayName("Override from existing stage")
        void test_override_from_existing()
        {
            // First: natural BEGINNER
            UserTrainingData data = new UserTrainingData();
            data.setUserId("u1");
            service.determineTransition("u1", data, null, NOW);

            // Manual override to PLATEAU
            StageTransition t = service.setUserStage("u1", UserStage.PLATEAU, "Injury recovery");

            assertEquals(UserStage.BEGINNER, t.getFromStage());
            assertEquals(UserStage.PLATEAU, t.getToStage());
        }

        @Test
        @DisplayName("Override stores reason in evidence")
        void test_override_reason()
        {
            StageTransition t = service.setUserStage("u1", UserStage.PLATEAU, "Test reason");
            assertEquals("Test reason", t.getEvidence().get("reason"));
        }
    }

    // ========== getStageBehavior() ==========

    @Nested
    @DisplayName("GetStageBehavior — Stage behavior config")
    class TestGetStageBehavior
    {
        @Test
        @DisplayName("BEGINNER behavior: difficulty 0.6, encouraging")
        void test_beginner_behavior()
        {
            StageBehavior b = service.getStageBehavior(UserStage.BEGINNER);
            assertNotNull(b);
            assertEquals(0.6, b.getDifficultyModifier(), 0.01);
            assertEquals("encouraging", b.getFeedbackStyle());
            assertEquals("achievement_unlock", b.getIncentiveType());
            assertEquals("beginner_coach", b.getPromptTemplateKey());
        }

        @Test
        @DisplayName("GROWTH behavior: difficulty 1.0, progressive")
        void test_growth_behavior()
        {
            StageBehavior b = service.getStageBehavior(UserStage.GROWTH);
            assertNotNull(b);
            assertEquals(1.0, b.getDifficultyModifier(), 0.01);
            assertEquals("progressive", b.getFeedbackStyle());
        }

        @Test
        @DisplayName("PLATEAU behavior: difficulty 0.8, reengagement")
        void test_plateau_behavior()
        {
            StageBehavior b = service.getStageBehavior(UserStage.PLATEAU);
            assertNotNull(b);
            assertEquals(0.8, b.getDifficultyModifier(), 0.01);
            assertEquals("reengagement", b.getFeedbackStyle());
            assertEquals("churn_prevention", b.getIncentiveType());
        }

        @Test
        @DisplayName("ADVANCED behavior: difficulty 1.3, competitive")
        void test_advanced_behavior()
        {
            StageBehavior b = service.getStageBehavior(UserStage.ADVANCED);
            assertNotNull(b);
            assertEquals(1.3, b.getDifficultyModifier(), 0.01);
            assertEquals("competitive", b.getFeedbackStyle());
        }
    }

    // ========== getTransitionHistory() ==========

    @Nested
    @DisplayName("TransitionHistory — History tracking")
    class TestTransitionHistory
    {
        @Test
        @DisplayName("Empty history for unknown user")
        void test_empty_history()
        {
            List<StageTransition> history = service.getTransitionHistory("unknown");
            assertTrue(history.isEmpty());
        }

        @Test
        @DisplayName("History records all transitions")
        void test_history_records()
        {
            // BEGINNER
            UserTrainingData d1 = new UserTrainingData();
            d1.setUserId("u1");
            service.determineTransition("u1", d1, null, NOW);

            // → GROWTH
            UserTrainingData d2 = newUser("u1", 6, 2.5, 0.8, NOW, 0.01);
            service.determineTransition("u1", d2, null, NOW);

            // → PLATEAU
            UserTrainingData d3 = newUser("u1", 8, 2.5, 0.8, NOW.minusDays(10), 0.01);
            service.determineTransition("u1", d3, null, NOW);

            List<StageTransition> history = service.getTransitionHistory("u1");
            assertEquals(3, history.size());
        }
    }

    // ========== getStageDistribution() ==========

    @Nested
    @DisplayName("StageDistribution — Dashboard metrics")
    class TestStageDistribution
    {
        @Test
        @DisplayName("Empty distribution all zeros")
        void test_empty_distribution()
        {
            Map<String, Integer> dist = service.getStageDistribution();
            for (UserStage s : UserStage.values())
            {
                assertEquals(0, dist.get(s.getValue()));
            }
        }

        @Test
        @DisplayName("Distribution counts correctly")
        void test_distribution_counts()
        {
            service.setUserStage("u1", UserStage.BEGINNER, "test");
            service.setUserStage("u2", UserStage.GROWTH, "test");
            service.setUserStage("u3", UserStage.ADVANCED, "test");
            service.setUserStage("u4", UserStage.PLATEAU, "test");
            service.setUserStage("u5", UserStage.BEGINNER, "test");

            Map<String, Integer> dist = service.getStageDistribution();
            assertEquals(2, dist.get("beginner"));
            assertEquals(1, dist.get("growth"));
            assertEquals(1, dist.get("advanced"));
            assertEquals(1, dist.get("plateau"));
        }
    }

    // ========== resetAll() ==========

    @Nested
    @DisplayName("ResetAll — State reset")
    class TestResetAll
    {
        @Test
        @DisplayName("Reset clears all data")
        void test_reset()
        {
            service.setUserStage("u1", UserStage.ADVANCED, "test");

            service.resetAll();

            assertNull(service.getUserStage("u1"));
            assertTrue(service.getTransitionHistory("u1").isEmpty());
        }
    }

    // ========== Helper ==========

    private static UserTrainingData newUser(String userId, double tenureWeeks,
                                             double weeklyFreq, double completionRate,
                                             LocalDateTime lastSession, double intensityTrend)
    {
        UserTrainingData data = new UserTrainingData();
        data.setUserId(userId);
        data.setTenureWeeks(tenureWeeks);
        data.setWeeklyFrequency(weeklyFreq);
        data.setCompletionRate(completionRate);
        data.setLastSessionDate(lastSession);
        data.setIntensityTrend(intensityTrend);
        return data;
    }
}
