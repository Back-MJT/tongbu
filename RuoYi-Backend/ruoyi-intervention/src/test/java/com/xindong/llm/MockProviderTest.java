package com.xindong.llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for MockProvider.
 * Ticket: XIN-93
 */
class MockProviderTest
{
    private MockProvider mock;

    @BeforeEach
    void setUp()
    {
        mock = new MockProvider();
    }

    @Nested
    @DisplayName("Basic Operations")
    class TestBasic
    {
        @Test
        @DisplayName("name() returns config name")
        void test_name()
        {
            assertEquals("mock", mock.name());
        }

        @Test
        @DisplayName("getModel() returns config model or 'mock-model'")
        void test_model()
        {
            assertEquals("mock-model", mock.getModel());
        }

        @Test
        @DisplayName("Default call returns Chinese fitness advice")
        void test_default_response()
        {
            LLMResponse resp = mock.call("sys", "user", 1024);
            assertTrue(resp.isSuccess());
            assertTrue(resp.getContent().contains("建议"));
        }

        @Test
        @DisplayName("Starts healthy")
        void test_healthy()
        {
            assertTrue(mock.isHealthy());
        }

        @Test
        @DisplayName("Cost and call count start at zero")
        void test_initial_stats()
        {
            assertEquals(0.0, mock.getCumulativeCostCents(), 0.01);
            assertEquals(0, mock.getCallCount());
        }
    }

    @Nested
    @DisplayName("Fixed Response")
    class TestFixedResponse
    {
        @Test
        @DisplayName("Custom fixed response")
        void test_custom_response()
        {
            mock.setFixedResponse("Custom output");
            LLMResponse resp = mock.call("sys", "user", 1024);
            assertEquals("Custom output", resp.getContent());
        }

        @Test
        @DisplayName("Fluent API returns same instance")
        void test_fluent()
        {
            MockProvider same = mock.setFixedResponse("x");
            assertSame(mock, same);
        }
    }

    @Nested
    @DisplayName("Failure Simulation")
    class TestFailure
    {
        @Test
        @DisplayName("Simulated failure returns error response")
        void test_simulate_failure()
        {
            mock.setSimulateFailure(true);
            LLMResponse resp = mock.call("sys", "user", 1024);
            assertFalse(resp.isSuccess());
            assertEquals("Simulated failure", resp.getErrorMessage());
        }

        @Test
        @DisplayName("3 consecutive failures marks unhealthy")
        void test_auto_unhealthy()
        {
            mock.setSimulateFailure(true);
            mock.call("sys", "u1", 1024);
            mock.call("sys", "u2", 1024);
            assertTrue(mock.isHealthy()); // 2 failures, still healthy
            mock.call("sys", "u3", 1024);
            assertFalse(mock.isHealthy()); // 3 failures, now unhealthy
        }

        @Test
        @DisplayName("Recovery resets consecutive failures")
        void test_recovery()
        {
            mock.setSimulateFailure(true);
            mock.call("sys", "u1", 1024);
            mock.call("sys", "u2", 1024);

            mock.setSimulateFailure(false);
            mock.call("sys", "u3", 1024); // Success resets counter
            assertTrue(mock.isHealthy());

            // Now 2 more failures won't trigger unhealthy (counter was reset)
            mock.setSimulateFailure(true);
            mock.call("sys", "u4", 1024);
            mock.call("sys", "u5", 1024);
            assertTrue(mock.isHealthy()); // Only 2 consecutive
        }
    }

    @Nested
    @DisplayName("Statistics")
    class TestStats
    {
        @Test
        @DisplayName("Call count increments")
        void test_call_count()
        {
            mock.call("sys", "u1", 1024);
            mock.call("sys", "u2", 1024);
            mock.call("sys", "u3", 1024);
            assertEquals(3, mock.getCallCount());
        }

        @Test
        @DisplayName("Cost accumulates")
        void test_cost_accumulates()
        {
            mock.call("sys", "user data here", 1024);
            double cost1 = mock.getCumulativeCostCents();

            mock.call("sys", "more user data here", 1024);
            double cost2 = mock.getCumulativeCostCents();
            assertTrue(cost2 > cost1);
        }

        @Test
        @DisplayName("resetStats clears everything")
        void test_reset()
        {
            mock.call("sys", "user", 1024);
            mock.resetStats();
            assertEquals(0.0, mock.getCumulativeCostCents(), 0.01);
            assertEquals(0, mock.getCallCount());
            assertTrue(mock.isHealthy());
        }
    }

    @Nested
    @DisplayName("Simulated Latency")
    class TestLatency
    {
        @Test
        @DisplayName("Simulated latency is reflected in response")
        void test_simulated_latency()
        {
            mock.setSimulatedLatencyMs(50);
            long start = System.currentTimeMillis();
            LLMResponse resp = mock.call("sys", "user", 1024);
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed >= 40, "Should take at least 40ms with 50ms simulated latency");
        }
    }

    @Nested
    @DisplayName("Token Estimation")
    class TestTokens
    {
        @Test
        @DisplayName("Tokens are estimated from prompt length")
        void test_token_estimation()
        {
            String longPrompt = "a".repeat(300);
            LLMResponse resp = mock.call("system prompt " + longPrompt, "user prompt", 1024);
            assertTrue(resp.getTokensIn() > 0);
            assertTrue(resp.getTokensOut() > 0);
        }
    }
}
