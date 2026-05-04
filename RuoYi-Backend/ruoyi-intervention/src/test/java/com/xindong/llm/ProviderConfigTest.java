package com.xindong.llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for ProviderConfig.
 * Ticket: XIN-93
 */
class ProviderConfigTest
{
    @Nested
    @DisplayName("Constructor & Defaults")
    class TestDefaults
    {
        @Test
        @DisplayName("Default constructor sets sensible defaults")
        void test_defaults()
        {
            ProviderConfig cfg = new ProviderConfig();
            assertEquals(0.0, cfg.getTemperature(), 0.001);
            assertEquals(1024, cfg.getMaxTokens());
            assertEquals(30, cfg.getTimeoutSeconds());
            assertTrue(cfg.isEnabled());
            assertEquals(100, cfg.getPriority());
        }

        @Test
        @DisplayName("3-arg constructor sets name, apiKey, model")
        void test_3arg()
        {
            ProviderConfig cfg = new ProviderConfig("claude", "sk-123", "claude-sonnet-4");
            assertEquals("claude", cfg.getName());
            assertEquals("sk-123", cfg.getApiKey());
            assertEquals("claude-sonnet-4", cfg.getModel());
        }
    }

    @Nested
    @DisplayName("Cost Calculation")
    class TestCostCalc
    {
        @Test
        @DisplayName("calculateCostCents computes input + output cost")
        void test_cost_calc()
        {
            ProviderConfig cfg = new ProviderConfig();
            cfg.setInputPricePerMtokCents(300.0);   // $3/Mtok
            cfg.setOutputPricePerMtokCents(1500.0);  // $15/Mtok

            // 1000 input tokens + 500 output tokens
            double cost = cfg.calculateCostCents(1000, 500);
            // Input: (1000/1M) * 300 = 0.3 cents
            // Output: (500/1M) * 1500 = 0.75 cents
            assertEquals(1.05, cost, 0.001);
        }

        @Test
        @DisplayName("Zero tokens = zero cost")
        void test_zero_tokens()
        {
            ProviderConfig cfg = new ProviderConfig();
            cfg.setInputPricePerMtokCents(300.0);
            cfg.setOutputPricePerMtokCents(1500.0);
            assertEquals(0.0, cfg.calculateCostCents(0, 0), 0.001);
        }

        @Test
        @DisplayName("Large token count calculates correctly")
        void test_large_tokens()
        {
            ProviderConfig cfg = new ProviderConfig();
            cfg.setInputPricePerMtokCents(300.0);
            cfg.setOutputPricePerMtokCents(1500.0);

            // 1M input + 1M output = 300 + 1500 = 1800 cents ($18)
            double cost = cfg.calculateCostCents(1_000_000, 1_000_000);
            assertEquals(1800.0, cost, 0.01);
        }
    }

    @Nested
    @DisplayName("Setters & Getters")
    class TestSetters
    {
        @Test
        @DisplayName("All setters work correctly")
        void test_all_setters()
        {
            ProviderConfig cfg = new ProviderConfig();
            cfg.setName("test");
            cfg.setBaseUrl("https://api.test.com");
            cfg.setApiKey("key-123");
            cfg.setModel("test-model");
            cfg.setTemperature(0.7);
            cfg.setMaxTokens(2048);
            cfg.setTimeoutSeconds(60);
            cfg.setEnabled(false);
            cfg.setPriority(5);

            assertEquals("test", cfg.getName());
            assertEquals("https://api.test.com", cfg.getBaseUrl());
            assertEquals("key-123", cfg.getApiKey());
            assertEquals("test-model", cfg.getModel());
            assertEquals(0.7, cfg.getTemperature(), 0.001);
            assertEquals(2048, cfg.getMaxTokens());
            assertEquals(60, cfg.getTimeoutSeconds());
            assertFalse(cfg.isEnabled());
            assertEquals(5, cfg.getPriority());
        }

        @Test
        @DisplayName("toString includes key fields")
        void test_to_string()
        {
            ProviderConfig cfg = new ProviderConfig("claude", "key", "claude-sonnet-4");
            String str = cfg.toString();
            assertTrue(str.contains("claude"));
            assertTrue(str.contains("claude-sonnet-4"));
        }
    }

    @Nested
    @DisplayName("Daily Cost Cap")
    class TestCostCap
    {
        @Test
        @DisplayName("Default daily cost cap is 0 (unlimited)")
        void test_default_cap()
        {
            ProviderConfig cfg = new ProviderConfig();
            assertEquals(0.0, cfg.getDailyCostCapCents(), 0.001);
        }

        @Test
        @DisplayName("setDailyCostCapCents sets the cap")
        void test_set_cap()
        {
            ProviderConfig cfg = new ProviderConfig();
            cfg.setDailyCostCapCents(50000.0);
            assertEquals(50000.0, cfg.getDailyCostCapCents(), 0.001);
        }
    }
}
