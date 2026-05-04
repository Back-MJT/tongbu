package com.xindong.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for LLMEngine — hot-swap, fallback, cache, cost tracking.
 * Ticket: XIN-93
 */
class LLMEngineTest
{
    private LLMEngine engine;
    private MockProvider mockA;
    private MockProvider mockB;

    @BeforeEach
    void setUp()
    {
        engine = new LLMEngine();

        ProviderConfig cfgA = new ProviderConfig("mockA", "key-a", "mock-model-a");
        cfgA.setPriority(10);
        cfgA.setInputPricePerMtokCents(100.0);
        cfgA.setOutputPricePerMtokCents(500.0);
        mockA = new MockProvider(cfgA);
        mockA.setFixedResponse("Response from A");

        ProviderConfig cfgB = new ProviderConfig("mockB", "key-b", "mock-model-b");
        cfgB.setPriority(20);
        cfgB.setInputPricePerMtokCents(50.0);
        cfgB.setOutputPricePerMtokCents(200.0);
        mockB = new MockProvider(cfgB);
        mockB.setFixedResponse("Response from B");

        engine.registerProvider(mockA);
        engine.registerProvider(mockB);
        engine.setActiveProvider("mockA");
        engine.setCacheTtlMinutes(60); // Default TTL
    }

    // ========== Hot-Swap ==========

    @Nested
    @DisplayName("HotSwap — Runtime provider switching")
    class TestHotSwap
    {
        @Test
        @DisplayName("Active provider returns its response")
        void test_active_provider_responds()
        {
            LLMResponse resp = engine.call("sys", "user");
            assertTrue(resp.isSuccess());
            assertEquals("Response from A", resp.getContent());
            assertEquals("mockA", resp.getProvider());
        }

        @Test
        @DisplayName("Switch active provider at runtime")
        void test_switch_provider()
        {
            engine.setActiveProvider("mockB");
            LLMResponse resp = engine.call("sys", "user");
            assertEquals("Response from B", resp.getContent());
            assertEquals("mockB", resp.getProvider());
        }

        @Test
        @DisplayName("Switch back to original provider")
        void test_switch_back()
        {
            engine.setActiveProvider("mockB");
            LLMResponse respB = engine.call("sys", "user_for_B");

            engine.setActiveProvider("mockA");
            LLMResponse respA = engine.call("sys", "user_for_A");
            assertEquals("Response from A", respA.getContent());
        }

        @Test
        @DisplayName("Switch to non-existent provider throws")
        void test_switch_nonexistent_throws()
        {
            assertThrows(IllegalArgumentException.class,
                () -> engine.setActiveProvider("nonexistent"));
        }

        @Test
        @DisplayName("getActiveProviderName returns current active")
        void test_get_active_name()
        {
            assertEquals("mockA", engine.getActiveProviderName());
            engine.setActiveProvider("mockB");
            assertEquals("mockB", engine.getActiveProviderName());
        }
    }

    // ========== Fallback Chain ==========

    @Nested
    @DisplayName("Fallback — Auto-failover on provider failure")
    class TestFallback
    {
        @Test
        @DisplayName("Falls back when active provider fails")
        void test_fallback_on_failure()
        {
            mockA.setSimulateFailure(true);
            LLMResponse resp = engine.call("sys", "user");
            assertTrue(resp.isSuccess());
            assertEquals("Response from B", resp.getContent());
        }

        @Test
        @DisplayName("Falls back when active provider is unhealthy")
        void test_fallback_on_unhealthy()
        {
            // Trigger 3 consecutive failures to mark unhealthy
            mockA.setSimulateFailure(true);
            engine.call("sys", "user1");
            engine.call("sys", "user2");
            engine.call("sys", "user3");

            assertFalse(mockA.isHealthy());

            // Now recover mockA but it's still unhealthy — should use B
            mockA.setSimulateFailure(false);
            LLMResponse resp = engine.call("sys", "user4");
            // B is fallback and healthy, so should succeed
            assertTrue(resp.isSuccess());
        }

        @Test
        @DisplayName("Returns error when all providers fail")
        void test_all_providers_fail()
        {
            mockA.setSimulateFailure(true);
            mockB.setSimulateFailure(true);
            LLMResponse resp = engine.call("sys", "user");
            assertFalse(resp.isSuccess());
            assertNotNull(resp.getErrorMessage());
        }

        @Test
        @DisplayName("Fallback order respects priority")
        void test_fallback_order()
        {
            List<String> order = engine.getFallbackOrder();
            // mockA has priority 10, mockB has priority 20
            assertEquals("mockA", order.get(0));
            assertEquals("mockB", order.get(1));
        }

        @Test
        @DisplayName("Health check restores provider")
        void test_health_check_restores()
        {
            mockA.setSimulateFailure(true);
            for (int i = 0; i < 3; i++) engine.call("sys", "u" + i);
            assertFalse(mockA.isHealthy());

            mockA.setSimulateFailure(false);
            mockA.healthCheck();
            assertTrue(mockA.isHealthy());
        }
    }

    // ========== Caching ==========

    @Nested
    @DisplayName("Cache — Response caching and TTL")
    class TestCache
    {
        @Test
        @DisplayName("Second identical call returns cached response")
        void test_cache_hit()
        {
            LLMResponse first = engine.call("sys", "same_user_data", 1024);
            LLMResponse second = engine.call("sys", "same_user_data", 1024);

            assertEquals(first.getContent(), second.getContent());
            // Check stats for cache hit
            Map<String, Object> stats = engine.getStats();
            assertTrue((Long) stats.get("total_cache_hits") >= 1);
        }

        @Test
        @DisplayName("Different prompt bypasses cache")
        void test_cache_miss()
        {
            engine.call("sys", "data1", 1024);
            engine.call("sys", "data2", 1024);

            Map<String, Object> stats = engine.getStats();
            assertEquals(0L, stats.get("total_cache_hits"));
        }

        @Test
        @DisplayName("Different maxTokens bypasses cache")
        void test_cache_miss_different_tokens()
        {
            engine.call("sys", "data", 512);
            engine.call("sys", "data", 1024);

            Map<String, Object> stats = engine.getStats();
            assertEquals(0L, stats.get("total_cache_hits"));
        }

        @Test
        @DisplayName("invalidateCache clears all entries")
        void test_invalidate_cache()
        {
            engine.call("sys", "data", 1024);
            int cleared = engine.invalidateCache();
            assertTrue(cleared > 0);

            // Next call should not be cached
            engine.call("sys", "data", 1024);
            Map<String, Object> stats = engine.getStats();
            assertEquals(0L, stats.get("total_cache_hits"));
        }

        @Test
        @DisplayName("Cache TTL expiry")
        void test_cache_ttl_expiry()
        {
            engine.setCacheTtlMinutes(0); // 0 = immediate expiry
            engine.call("sys", "data", 1024);
            // With TTL=0, entry is immediately expired
            engine.call("sys", "data", 1024);

            // Both calls should go to provider (no cache hits)
            Map<String, Object> stats = engine.getStats();
            assertEquals(0L, stats.get("total_cache_hits"));
        }
    }

    // ========== Cost Tracking ==========

    @Nested
    @DisplayName("Cost — Per-provider and engine-wide cost tracking")
    class TestCost
    {
        @Test
        @DisplayName("Engine tracks total cost")
        void test_total_cost()
        {
            engine.call("sys", "user_data_here", 1024);
            Map<String, Object> stats = engine.getStats();
            double totalCost = (Double) stats.get("total_cost_cents");
            assertTrue(totalCost > 0, "Total cost should be > 0 after a call");
        }

        @Test
        @DisplayName("Per-provider cost tracking")
        void test_per_provider_cost()
        {
            engine.call("sys", "user_data", 1024);
            assertTrue(mockA.getCumulativeCostCents() > 0);
            assertEquals(0.0, mockB.getCumulativeCostCents(), 0.01);
        }

        @Test
        @DisplayName("Call count tracked per provider")
        void test_call_count()
        {
            engine.call("sys", "u1", 1024);
            engine.call("sys", "u2", 1024);
            assertEquals(2, mockA.getCallCount());
            assertEquals(0, mockB.getCallCount());
        }

        @Test
        @DisplayName("Fallback cost goes to fallback provider")
        void test_fallback_cost()
        {
            mockA.setSimulateFailure(true);
            engine.call("sys", "user_data", 1024);

            assertTrue(mockB.getCumulativeCostCents() > 0);
        }
    }

    // ========== Provider Management ==========

    @Nested
    @DisplayName("Registry — Provider registration and removal")
    class TestRegistry
    {
        @Test
        @DisplayName("getProviderNames lists all providers")
        void test_list_providers()
        {
            List<String> names = engine.getProviderNames();
            assertTrue(names.contains("mockA"));
            assertTrue(names.contains("mockB"));
        }

        @Test
        @DisplayName("Unregister removes provider")
        void test_unregister()
        {
            engine.unregisterProvider("mockB");
            List<String> names = engine.getProviderNames();
            assertFalse(names.contains("mockB"));
        }

        @Test
        @DisplayName("Unregister active provider switches to next")
        void test_unregister_active()
        {
            engine.unregisterProvider("mockA");
            assertEquals("mockB", engine.getActiveProviderName());
        }

        @Test
        @DisplayName("Register new provider adds to fallback order")
        void test_register_new()
        {
            ProviderConfig cfgC = new ProviderConfig("mockC", "key-c", "model-c");
            cfgC.setPriority(5); // Higher priority than A
            MockProvider mockC = new MockProvider(cfgC);
            mockC.setFixedResponse("Response from C");
            engine.registerProvider(mockC);

            List<String> order = engine.getFallbackOrder();
            assertEquals("mockC", order.get(0)); // Priority 5 = highest
        }
    }

    // ========== Statistics ==========

    @Nested
    @DisplayName("Stats — Engine-wide statistics")
    class TestStats
    {
        @Test
        @DisplayName("Stats structure has all fields")
        void test_stats_structure()
        {
            engine.call("sys", "user", 1024);
            Map<String, Object> stats = engine.getStats();

            assertTrue(stats.containsKey("total_calls"));
            assertTrue(stats.containsKey("total_cost_cents"));
            assertTrue(stats.containsKey("total_cache_hits"));
            assertTrue(stats.containsKey("cache_hit_rate"));
            assertTrue(stats.containsKey("cache_entries"));
            assertTrue(stats.containsKey("active_provider"));
            assertTrue(stats.containsKey("registered_providers"));
            assertTrue(stats.containsKey("providers"));
        }

        @Test
        @DisplayName("Stats increment with calls")
        void test_stats_increment()
        {
            engine.call("sys", "u1", 1024);
            engine.call("sys", "u2", 1024);

            Map<String, Object> stats = engine.getStats();
            assertEquals(2L, stats.get("total_calls"));
        }

        @Test
        @DisplayName("Per-provider stats in engine stats")
        void test_per_provider_in_stats()
        {
            engine.call("sys", "user", 1024);
            Map<String, Object> stats = engine.getStats();

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> provStats =
                (Map<String, Map<String, Object>>) stats.get("providers");

            assertTrue(provStats.containsKey("mockA"));
            assertTrue(provStats.containsKey("mockB"));
            assertEquals("mock-model-a", provStats.get("mockA").get("model"));
        }
    }

    // ========== Reset ==========

    @Nested
    @DisplayName("Reset — State reset")
    class TestReset
    {
        @Test
        @DisplayName("resetAll clears all counters and cache")
        void test_reset_all()
        {
            engine.call("sys", "user", 1024);
            engine.resetAll();

            Map<String, Object> stats = engine.getStats();
            assertEquals(0L, stats.get("total_calls"));
            assertEquals(0.0, stats.get("total_cost_cents"));
            assertEquals(0L, stats.get("total_cache_hits"));
        }
    }

    // ========== Empty Engine ==========

    @Nested
    @DisplayName("EmptyEngine — Engine with no providers")
    class TestEmptyEngine
    {
        @Test
        @DisplayName("Call with no providers returns error")
        void test_no_providers()
        {
            LLMEngine emptyEngine = new LLMEngine();
            LLMResponse resp = emptyEngine.call("sys", "user");
            assertFalse(resp.isSuccess());
        }
    }

    // ========== Health Check All ==========

    @Nested
    @DisplayName("HealthCheckAll — Bulk health checks")
    class TestHealthCheckAll
    {
        @Test
        @DisplayName("healthCheckAll resets all providers")
        void test_health_check_all()
        {
            mockA.setSimulateFailure(true);
            for (int i = 0; i < 3; i++) engine.call("sys", "u" + i);
            assertFalse(mockA.isHealthy());

            mockA.setSimulateFailure(false);
            engine.healthCheckAll();
            assertTrue(mockA.isHealthy());
            assertTrue(mockB.isHealthy());
        }
    }
}
