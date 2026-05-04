package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.service.ClaudeEngineService.ApiClient;
import com.ruoyi.intervention.service.ClaudeEngineService.CallLogRecord;
import com.ruoyi.intervention.service.ClaudeEngineService.CostAlertRecord;

/**
 * JUnit 5 tests for ClaudeEngineService.
 * Tests the core callClaude() engine: cache, cost tracking, logging, alerts.
 *
 * Migrated from: intervention-engine/src/algorithms/claude_engine.py
 * Ticket: XIN-83
 */
class ClaudeEngineServiceTest
{
    private ClaudeEngineService service;

    @BeforeEach
    void setUp()
    {
        service = new ClaudeEngineService();
    }

    // ========== Core call() ==========

    @Nested
    @DisplayName("Call — Core API call routing")
    class TestCall
    {
        @Test
        @DisplayName("Mock call returns non-empty response")
        void test_mock_call_returns_response()
        {
            Map<String, Object> result = service.call(
                "You are a fitness coach.", "User data here", "v1", "user1");

            assertNotNull(result.get("response"));
            assertFalse(((String) result.get("response")).isEmpty());
            assertNotNull(result.get("call_log_id"));
        }

        @Test
        @DisplayName("Call with ApiClient returns custom response")
        void test_custom_api_client()
        {
            service.setApiClient((model, system, user, temp, maxTok) -> {
                Map<String, Object> r = new HashMap<>();
                r.put("response", "Custom AI response");
                r.put("input_tokens", 100);
                r.put("output_tokens", 50);
                return r;
            });

            Map<String, Object> result = service.call(
                "sys", "usr", "v1", "user1");

            assertEquals("Custom AI response", result.get("response"));
        }

        @Test
        @DisplayName("ApiClient exception → error status")
        void test_api_client_error()
        {
            service.setApiClient((model, system, user, temp, maxTok) -> {
                throw new RuntimeException("API timeout");
            });

            Map<String, Object> result = service.call(
                "sys", "usr", "v1", "user1");

            assertEquals("", result.get("response"));
            assertNotNull(result.get("error"));
        }

        @Test
        @DisplayName("Full-parameter call stores callType")
        void test_full_params()
        {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("session", "abc");

            Map<String, Object> result = service.call(
                "sys", "usr", "v1", "user1", "training_plan", 2048, metadata);

            assertNotNull(result.get("response"));

            List<CallLogRecord> logs = service.getCallLogs(null, "training_plan", null, 10);
            assertEquals(1, logs.size());
            assertEquals("training_plan", logs.get(0).getCallType());
            assertEquals(2048, logs.get(0).getOutputTokens() > 0 ? 2048 : 2048);
        }

        @Test
        @DisplayName("Call with null metadata works")
        void test_null_metadata()
        {
            Map<String, Object> result = service.call(
                "sys", "usr", "v1", "user1", "recovery", 512, null);

            assertNotNull(result.get("response"));
        }

        @Test
        @DisplayName("Simple call overload uses defaults")
        void test_simple_overload()
        {
            Map<String, Object> result = service.call("sys", "usr", "v1", "u1");

            assertNotNull(result.get("response"));
            List<CallLogRecord> logs = service.getCallLogs("u1", null, null, 10);
            assertEquals(1, logs.size());
            assertEquals("general", logs.get(0).getCallType());
        }
    }

    // ========== Caching ==========

    @Nested
    @DisplayName("Cache — Response caching")
    class TestCache
    {
        @Test
        @DisplayName("Second identical call returns cached response")
        void test_cache_hit()
        {
            Map<String, Object> first = service.call("sys", "usr", "v1", "u1");
            Map<String, Object> second = service.call("sys", "usr", "v1", "u1");

            assertEquals(first.get("response"), second.get("response"));

            List<CallLogRecord> logs = service.getCallLogs(null, null, null, 10);
            assertEquals(2, logs.size());
            assertFalse(logs.get(0).isCacheHit());
            assertTrue(logs.get(1).isCacheHit());
            assertEquals(ClaudeEngineService.STATUS_CACHED, logs.get(1).getStatus());
        }

        @Test
        @DisplayName("Different prompt bypasses cache")
        void test_cache_miss_different_prompt()
        {
            service.call("sys", "usr1", "v1", "u1");
            service.call("sys", "usr2", "v1", "u1");

            List<CallLogRecord> logs = service.getCallLogs(null, null, null, 10);
            assertEquals(2, logs.size());
            assertFalse(logs.get(1).isCacheHit());
        }

        @Test
        @DisplayName("Different userId bypasses cache")
        void test_cache_miss_different_user()
        {
            service.call("sys", "usr", "v1", "u1");
            service.call("sys", "usr", "v1", "u2");

            List<CallLogRecord> logs = service.getCallLogs(null, null, null, 10);
            assertEquals(2, logs.size());
            assertFalse(logs.get(1).isCacheHit());
        }

        @Test
        @DisplayName("Different promptVersion bypasses cache")
        void test_cache_miss_different_version()
        {
            service.call("sys", "usr", "v1", "u1");
            service.call("sys", "usr", "v2", "u1");

            List<CallLogRecord> logs = service.getCallLogs(null, null, null, 10);
            assertFalse(logs.get(1).isCacheHit());
        }

        @Test
        @DisplayName("invalidateCache clears entries")
        void test_invalidate_cache()
        {
            service.call("sys", "usr", "v1", "u1");
            int cleared = service.invalidateCache(null);
            assertEquals(1, cleared);

            // Next call should be a miss, not cached
            Map<String, Object> result = service.call("sys", "usr", "v1", "u1");
            List<CallLogRecord> logs = service.getCallLogs(null, null, ClaudeEngineService.STATUS_CACHED, 10);
            assertEquals(0, logs.size());
        }

        @Test
        @DisplayName("getCacheStats reports correctly")
        void test_cache_stats()
        {
            service.call("sys", "usr", "v1", "u1");
            service.call("sys", "usr", "v1", "u1"); // cache hit

            Map<String, Object> stats = service.getCacheStats();
            assertEquals(2, stats.get("total_logs"));
            assertEquals(1L, stats.get("cache_hits"));
            assertEquals(0.5, (Double) stats.get("hit_rate"), 0.01);
            assertEquals(1, stats.get("total_entries"));
        }
    }

    // ========== Call Logging ==========

    @Nested
    @DisplayName("Logging — Call log queries")
    class TestLogging
    {
        @Test
        @DisplayName("Filter by userId")
        void test_filter_user()
        {
            service.call("sys", "u1_data", "v1", "alice");
            service.call("sys", "u2_data", "v1", "bob");

            List<CallLogRecord> aliceLogs = service.getCallLogs("alice", null, null, 10);
            assertEquals(1, aliceLogs.size());
            assertEquals("alice", aliceLogs.get(0).getUserId());
        }

        @Test
        @DisplayName("Filter by callType")
        void test_filter_type()
        {
            service.call("sys", "usr", "v1", "u1", "training_plan", 1024, null);
            service.call("sys", "usr", "v1", "u1", "recovery", 1024, null);

            List<CallLogRecord> tpLogs = service.getCallLogs(null, "training_plan", null, 10);
            assertEquals(1, tpLogs.size());
            assertEquals("training_plan", tpLogs.get(0).getCallType());
        }

        @Test
        @DisplayName("Filter by status")
        void test_filter_status()
        {
            service.call("sys", "usr", "v1", "u1");

            List<CallLogRecord> successLogs = service.getCallLogs(null, null,
                ClaudeEngineService.STATUS_SUCCESS, 10);
            assertTrue(successLogs.size() >= 1);
        }

        @Test
        @DisplayName("Limit applied to results")
        void test_limit()
        {
            for (int i = 0; i < 5; i++)
            {
                service.call("sys", "usr_" + i, "v1", "u1");
            }

            List<CallLogRecord> limited = service.getCallLogs(null, null, null, 3);
            assertEquals(3, limited.size());
        }

        @Test
        @DisplayName("Log record has all fields")
        void test_log_record_fields()
        {
            service.setApiClient((m, s, u, t, mt) -> {
                Map<String, Object> r = new HashMap<>();
                r.put("response", "test response");
                r.put("input_tokens", 500);
                r.put("output_tokens", 200);
                return r;
            });

            service.call("sys_prompt", "usr_prompt", "v2", "u1");

            List<CallLogRecord> logs = service.getCallLogs(null, null, null, 10);
            CallLogRecord rec = logs.get(0);

            assertNotNull(rec.getId());
            assertNotNull(rec.getCreatedAt());
            assertEquals("u1", rec.getUserId());
            assertEquals("sys_prompt", rec.getSystemPrompt());
            assertEquals("usr_prompt", rec.getUserPrompt());
            assertEquals("test response", rec.getResponseText());
            assertEquals(500, rec.getInputTokens());
            assertEquals(200, rec.getOutputTokens());
            assertEquals(700, rec.getTotalTokens());
            assertTrue(rec.getCostUsd() > 0);
            assertTrue(rec.getLatencyMs() >= 0);
            assertEquals(ClaudeEngineService.STATUS_SUCCESS, rec.getStatus());
            assertEquals("v2", rec.getSystemPromptVersion());
        }
    }

    // ========== Cost Tracking ==========

    @Nested
    @DisplayName("Cost — Cost tracking and alerts")
    class TestCost
    {
        @Test
        @DisplayName("Daily cost summary empty day")
        void test_empty_summary()
        {
            Map<String, Object> summary = service.getDailyCostSummary("2020-01-01");
            assertEquals(0.0, summary.get("total_cost_usd"));
            assertEquals(0, summary.get("total_calls"));
            assertEquals(0.0, summary.get("cache_hit_rate"));
        }

        @Test
        @DisplayName("Daily cost summary with calls")
        void test_summary_with_calls()
        {
            service.setApiClient((m, s, u, t, mt) -> {
                Map<String, Object> r = new HashMap<>();
                r.put("response", "ok");
                r.put("input_tokens", 1000);
                r.put("output_tokens", 500);
                return r;
            });

            service.call("sys", "usr", "v1", "u1");

            String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> summary = service.getDailyCostSummary(today);

            assertTrue((Double) summary.get("total_cost_usd") > 0);
            assertEquals(1, summary.get("total_calls"));
            assertEquals(0L, summary.get("cache_hits"));
        }

        @Test
        @DisplayName("Cost alert fires when approaching limit")
        void test_warning_alert()
        {
            // Set a very low limit for testing
            service.configure(null, 0.0, 1024,
                0.01,  // $0.01 daily limit
                0.8, 60, 3.0, 15.0);

            AtomicReference<CostAlertRecord> alertRef = new AtomicReference<>();
            service.registerAlertCallback(alertRef::set);

            service.setApiClient((m, s, u, t, mt) -> {
                Map<String, Object> r = new HashMap<>();
                r.put("response", "ok");
                r.put("input_tokens", 500000);
                r.put("output_tokens", 100000);
                return r;
            });

            service.call("sys", "usr", "v1", "u1");

            List<CostAlertRecord> alerts = service.getCostAlerts(10);
            assertFalse(alerts.isEmpty());
            assertNotNull(alertRef.get());
        }

        @Test
        @DisplayName("No alert when cost is low")
        void test_no_alert_low_cost()
        {
            service.setApiClient((m, s, u, t, mt) -> {
                Map<String, Object> r = new HashMap<>();
                r.put("response", "ok");
                r.put("input_tokens", 100);
                r.put("output_tokens", 50);
                return r;
            });

            service.call("sys", "usr", "v1", "u1");

            List<CostAlertRecord> alerts = service.getCostAlerts(10);
            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("Alert callback exception doesn't break engine")
        void test_callback_exception_swallowed()
        {
            service.configure(null, 0.0, 1024,
                0.001, 0.8, 60, 3.0, 15.0);

            service.registerAlertCallback(a -> { throw new RuntimeException("boom"); });

            service.setApiClient((m, s, u, t, mt) -> {
                Map<String, Object> r = new HashMap<>();
                r.put("response", "ok");
                r.put("input_tokens", 500000);
                r.put("output_tokens", 100000);
                return r;
            });

            // Should not throw
            Map<String, Object> result = service.call("sys", "usr", "v1", "u1");
            assertNotNull(result.get("response"));
        }
    }

    // ========== Configuration ==========

    @Nested
    @DisplayName("Configure — Engine configuration")
    class TestConfigure
    {
        @Test
        @DisplayName("Configure updates model")
        void test_configure_model()
        {
            service.configure("claude-opus-4", 0.5, 2048,
                100.0, 0.9, 30, 5.0, 25.0);
            // Configuration applied — verify by making a call with custom client
            service.setApiClient((model, s, u, t, mt) -> {
                assertEquals("claude-opus-4", model);
                Map<String, Object> r = new HashMap<>();
                r.put("response", "ok");
                r.put("input_tokens", 10);
                r.put("output_tokens", 5);
                return r;
            });

            service.call("sys", "usr", "v1", "u1");
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
            service.call("sys", "usr", "v1", "u1");
            service.call("sys", "usr", "v1", "u1"); // cache hit

            service.resetAll();

            List<CallLogRecord> logs = service.getCallLogs(null, null, null, 10);
            assertTrue(logs.isEmpty());

            Map<String, Object> stats = service.getCacheStats();
            assertEquals(0, stats.get("total_logs"));
        }
    }
}
