package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.enums.ComplianceLevel;
import com.ruoyi.intervention.domain.enums.FeedbackType;
import com.ruoyi.intervention.domain.model.FeedbackEntry;
import com.ruoyi.intervention.domain.model.LowComplianceAlert;
import com.ruoyi.intervention.domain.model.UserComplianceStatus;

/**
 * JUnit 5 tests for ComplianceTrackingService.
 * Migrated from: intervention-engine/src/algorithms/compliance_tracking.py
 *
 * Covers:
 *   - recordInterventionExecution() execution tracking
 *   - getUserCompliance() compliance level determination
 *   - getLowComplianceAlerts() batch alert generation
 *   - getComplianceSummary() aggregate statistics
 *   - recordFeedback() + getFeedbackHistory() feedback lifecycle
 *   - 7-day compliance rate calculation
 *
 * Evidence: klasnja_2015_jitai, nahum_shani_2018_jitai_design
 */
class ComplianceTrackingServiceTest
{
    private ComplianceTrackingService service;

    @BeforeEach
    void setUp()
    {
        service = new ComplianceTrackingService();
    }

    // ========== recordInterventionExecution() ==========

    @Nested
    @DisplayName("RecordInterventionExecution — Execution recording")
    class TestRecordExecution
    {
        @Test
        @DisplayName("First execution records timestamp")
        void test_first_execution()
        {
            LocalDateTime ts = LocalDateTime.now();
            service.recordInterventionExecution("u1", "rx-001", ts);

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(0, status.getDaysWithoutIntervention());
            assertNotNull(status.getLastInterventionDate());
        }

        @Test
        @DisplayName("Later execution updates lastExecution")
        void test_later_execution_updates()
        {
            LocalDateTime earlier = LocalDateTime.now().minusDays(2);
            LocalDateTime later = LocalDateTime.now().minusDays(1);
            service.recordInterventionExecution("u1", "rx-001", earlier);
            service.recordInterventionExecution("u1", "rx-002", later);

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(1, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("Earlier execution does not overwrite later one")
        void test_earlier_no_overwrite()
        {
            LocalDateTime later = LocalDateTime.now().minusDays(1);
            LocalDateTime earlier = LocalDateTime.now().minusDays(5);
            service.recordInterventionExecution("u1", "rx-001", later);
            service.recordInterventionExecution("u1", "rx-002", earlier);

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(1, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("Null performedAt defaults to now")
        void test_null_timestamp()
        {
            service.recordInterventionExecution("u1", "rx-001", null);

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(0, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("Multiple executions on same day count as one day")
        void test_multiple_same_day()
        {
            LocalDateTime today = LocalDateTime.now();
            service.recordInterventionExecution("u1", "rx-001", today.minusHours(3));
            service.recordInterventionExecution("u1", "rx-002", today.minusHours(1));
            service.recordInterventionExecution("u1", "rx-003", today);

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(0, status.getDaysWithoutIntervention());
        }
    }

    // ========== getUserCompliance() ==========

    @Nested
    @DisplayName("GetUserCompliance — Compliance level determination")
    class TestGetUserCompliance
    {
        @Test
        @DisplayName("No data, no prescription → NONE")
        void test_no_data_none()
        {
            UserComplianceStatus status = service.getUserCompliance("unknown");
            assertEquals(ComplianceLevel.NONE, status.getComplianceLevel());
            assertEquals(0, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("Active prescription, no execution → LOW (daysWithout=999)")
        void test_active_rx_no_execution()
        {
            service.setActivePrescriptionCount("u1", 1);
            UserComplianceStatus status = service.getUserCompliance("u1");
            // daysWithout = 999 (never executed), which is >= 3 → LOW
            assertEquals(ComplianceLevel.LOW, status.getComplianceLevel());
            assertTrue(status.getDaysWithoutIntervention() >= 3);
        }

        @Test
        @DisplayName("Executed today → HIGH")
        void test_executed_today_high()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(ComplianceLevel.HIGH, status.getComplianceLevel());
            assertEquals(0, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("3+ days without → LOW")
        void test_three_days_low()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(3));

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(ComplianceLevel.LOW, status.getComplianceLevel());
        }

        @Test
        @DisplayName("7 days without → LOW")
        void test_seven_days_low()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(7));

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(ComplianceLevel.LOW, status.getComplianceLevel());
            assertEquals(7, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("1 day ago, low 7d rate → MEDIUM (rate < 0.6)")
        void test_one_day_high()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(1));

            UserComplianceStatus status = service.getUserCompliance("u1");
            // 1 execution yesterday: elapsedDays=2, rate=1/2=0.5 < 0.6 → MEDIUM
            assertEquals(ComplianceLevel.MEDIUM, status.getComplianceLevel());
            assertEquals(1, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("Status has userId and computedAt")
        void test_status_fields()
        {
            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals("u1", status.getUserId());
            assertNotNull(status.getComputedAt());
            assertEquals("klasnja_2015_jitai", status.getEvidenceRef());
        }

        @Test
        @DisplayName("Status has level reason")
        void test_level_reason()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertNotNull(status.getLevelReason());
            assertFalse(status.getLevelReason().isEmpty());
        }

        @Test
        @DisplayName("Prescription count is tracked")
        void test_prescription_count()
        {
            service.setActivePrescriptionCount("u1", 3);
            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(3, status.getActivePrescriptionCount());
        }
    }

    // ========== 7-day Compliance Rate ==========

    @Nested
    @DisplayName("ComplianceRate7d — 7-day rate calculation")
    class TestComplianceRate7d
    {
        @Test
        @DisplayName("Single execution today → rate reflects same-day execution")
        void test_single_today()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertNotNull(status.getComplianceRate7d());
            assertTrue(status.getComplianceRate7d() > 0);
        }

        @Test
        @DisplayName("No executions → null rate")
        void test_no_executions_null_rate()
        {
            service.setActivePrescriptionCount("u1", 1);
            UserComplianceStatus status = service.getUserCompliance("u1");
            assertNull(status.getComplianceRate7d());
        }

        @Test
        @DisplayName("Executions on 5 of 7 days → high rate")
        void test_five_of_seven()
        {
            service.setActivePrescriptionCount("u1", 1);
            for (int i = 0; i < 5; i++)
            {
                service.recordInterventionExecution("u1", "rx-" + i,
                    LocalDateTime.now().minusDays(i));
            }

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertNotNull(status.getComplianceRate7d());
            assertTrue(status.getComplianceRate7d() >= 0.7);
        }

        @Test
        @DisplayName("Low 7d rate → MEDIUM compliance")
        void test_low_rate_medium()
        {
            service.setActivePrescriptionCount("u1", 1);
            // Only 1 execution 6 days ago → rate is very low, but daysWithout < 3
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(2));

            // Also record an old execution 10 days ago that is outside window
            // so the rate within window is from 2 days ago only
            // daysWithout=2 < 3, rate from 1 execution in 3 elapsed days = 1/3 ≈ 0.33 < 0.6 → MEDIUM
            UserComplianceStatus status = service.getUserCompliance("u1");
            // With 2 days without and 1 execution day / 3 elapsed = 0.33 → MEDIUM
            assertEquals(ComplianceLevel.MEDIUM, status.getComplianceLevel());
            assertNotNull(status.getComplianceRate7d());
            assertTrue(status.getComplianceRate7d() < 0.6);
        }
    }

    // ========== getLowComplianceAlerts() ==========

    @Nested
    @DisplayName("GetLowComplianceAlerts — Batch alert generation")
    class TestGetLowComplianceAlerts
    {
        @Test
        @DisplayName("No low users → empty list")
        void test_no_low_users()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            List<LowComplianceAlert> alerts = service.getLowComplianceAlerts(
                Arrays.asList("u1"));
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Low user generates alert")
        void test_low_user_alert()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(5));

            List<LowComplianceAlert> alerts = service.getLowComplianceAlerts(
                Arrays.asList("u1"));
            assertEquals(1, alerts.size());
            assertEquals("u1", alerts.get(0).getUserId());
            assertEquals(ComplianceLevel.LOW, alerts.get(0).getComplianceLevel());
            assertEquals("warning", alerts.get(0).getSeverity());
        }

        @Test
        @DisplayName("Mixed users → only LOW get alerts")
        void test_mixed_users()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.setActivePrescriptionCount("u2", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());
            service.recordInterventionExecution("u2", "rx-002",
                LocalDateTime.now().minusDays(5));

            List<LowComplianceAlert> alerts = service.getLowComplianceAlerts(
                Arrays.asList("u1", "u2"));
            assertEquals(1, alerts.size());
            assertEquals("u2", alerts.get(0).getUserId());
        }

        @Test
        @DisplayName("Alert has populated fields")
        void test_alert_fields()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(4));

            List<LowComplianceAlert> alerts = service.getLowComplianceAlerts(
                Arrays.asList("u1"));
            LowComplianceAlert alert = alerts.get(0);
            assertNotNull(alert.getAlertId());
            assertNotNull(alert.getTitle());
            assertNotNull(alert.getMessage());
            assertTrue(alert.getDaysWithoutIntervention() >= 3);
        }
    }

    // ========== getComplianceSummary() ==========

    @Nested
    @DisplayName("GetComplianceSummary — Aggregate statistics")
    class TestGetComplianceSummary
    {
        @Test
        @DisplayName("Empty user list → zero counts")
        void test_empty()
        {
            Map<String, Object> summary = service.getComplianceSummary(new ArrayList<>());
            assertEquals(0, summary.get("total_users"));
            assertEquals(0, summary.get("low_compliance_count"));
        }

        @Test
        @DisplayName("Summary counts by level")
        void test_counts_by_level()
        {
            // u1: HIGH (executed today)
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            // u2: LOW (5 days ago)
            service.setActivePrescriptionCount("u2", 1);
            service.recordInterventionExecution("u2", "rx-002",
                LocalDateTime.now().minusDays(5));

            Map<String, Object> summary = service.getComplianceSummary(
                Arrays.asList("u1", "u2"));

            assertEquals(2, summary.get("total_users"));
            assertEquals(1, summary.get("low_compliance_count"));

            @SuppressWarnings("unchecked")
            Map<String, Integer> byLevel = (Map<String, Integer>) summary.get("by_compliance_level");
            assertTrue(byLevel.get("high") >= 1);
            assertTrue(byLevel.get("low") >= 1);
        }

        @Test
        @DisplayName("Low compliance users list populated")
        void test_low_users_list()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001",
                LocalDateTime.now().minusDays(5));

            Map<String, Object> summary = service.getComplianceSummary(
                Arrays.asList("u1"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lowUsers =
                (List<Map<String, Object>>) summary.get("low_compliance_users");
            assertFalse(lowUsers.isEmpty());
            assertEquals("u1", lowUsers.get(0).get("user_id"));
        }

        @Test
        @DisplayName("Average compliance rate computed")
        void test_avg_rate()
        {
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            Map<String, Object> summary = service.getComplianceSummary(
                Arrays.asList("u1"));

            double avgRate = ((Number) summary.get("compliance_rate_avg")).doubleValue();
            assertTrue(avgRate >= 0.0 && avgRate <= 1.0);
        }
    }

    // ========== Feedback Lifecycle ==========

    @Nested
    @DisplayName("FeedbackLifecycle — recordFeedback + getFeedbackHistory")
    class TestFeedbackLifecycle
    {
        @Test
        @DisplayName("Record and retrieve feedback")
        void test_record_and_retrieve()
        {
            FeedbackEntry entry = new FeedbackEntry();
            entry.setUserId("u1");
            entry.setInterventionId("rx-001");
            entry.setFeedbackType(FeedbackType.SUBJECTIVE_EXERTION);
            entry.setRpe(7);
            service.recordFeedback(entry);

            List<FeedbackEntry> history = service.getFeedbackHistory("u1", null, null, 10);
            assertEquals(1, history.size());
            assertEquals(7, history.get(0).getRpe());
        }

        @Test
        @DisplayName("COMPLIANCE feedback also records execution")
        void test_compliance_feedback_records_execution()
        {
            FeedbackEntry entry = new FeedbackEntry();
            entry.setUserId("u1");
            entry.setInterventionId("rx-001");
            entry.setFeedbackType(FeedbackType.COMPLIANCE);
            entry.setTimestamp(new Date());
            service.recordFeedback(entry);

            // Should have recorded an execution
            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(0, status.getDaysWithoutIntervention());
        }

        @Test
        @DisplayName("Filter by feedback type")
        void test_filter_by_type()
        {
            FeedbackEntry e1 = new FeedbackEntry();
            e1.setUserId("u1");
            e1.setInterventionId("rx-001");
            e1.setFeedbackType(FeedbackType.SUBJECTIVE_EXERTION);
            e1.setRpe(5);

            FeedbackEntry e2 = new FeedbackEntry();
            e2.setUserId("u1");
            e2.setInterventionId("rx-002");
            e2.setFeedbackType(FeedbackType.ADVERSE_EVENT);
            e2.setAdverseEvent(true);

            service.recordFeedback(e1);
            service.recordFeedback(e2);

            List<FeedbackEntry> exertionOnly = service.getFeedbackHistory(
                "u1", FeedbackType.SUBJECTIVE_EXERTION, null, 10);
            assertEquals(1, exertionOnly.size());
            assertEquals(FeedbackType.SUBJECTIVE_EXERTION, exertionOnly.get(0).getFeedbackType());
        }

        @Test
        @DisplayName("Limit applied to results")
        void test_limit()
        {
            for (int i = 0; i < 10; i++)
            {
                FeedbackEntry entry = new FeedbackEntry();
                entry.setUserId("u1");
                entry.setInterventionId("rx-" + i);
                entry.setFeedbackType(FeedbackType.SUBJECTIVE_OUTCOME);
                service.recordFeedback(entry);
            }

            List<FeedbackEntry> limited = service.getFeedbackHistory("u1", null, null, 3);
            assertEquals(3, limited.size());
        }

        @Test
        @DisplayName("Empty history for unknown user")
        void test_unknown_user()
        {
            List<FeedbackEntry> history = service.getFeedbackHistory(
                "unknown", null, null, 10);
            assertTrue(history.isEmpty());
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
            service.setActivePrescriptionCount("u1", 1);
            service.recordInterventionExecution("u1", "rx-001", LocalDateTime.now());

            service.resetAll();

            UserComplianceStatus status = service.getUserCompliance("u1");
            assertEquals(ComplianceLevel.NONE, status.getComplianceLevel());
            assertEquals(0, status.getActivePrescriptionCount());
        }
    }
}
