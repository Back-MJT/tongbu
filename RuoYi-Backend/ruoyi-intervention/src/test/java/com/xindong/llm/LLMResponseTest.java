package com.xindong.llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for LLMResponse value object.
 * Ticket: XIN-93
 */
class LLMResponseTest
{
    @Nested
    @DisplayName("LLMResponse.ok — Success responses")
    class TestOk
    {
        @Test
        @DisplayName("ok() creates successful response")
        void test_ok_basic()
        {
            LLMResponse resp = LLMResponse.ok("hello", "claude-sonnet-4", "claude",
                                               100, 50, 250, 3.5);
            assertTrue(resp.isSuccess());
            assertEquals("hello", resp.getContent());
            assertEquals("claude-sonnet-4", resp.getModel());
            assertEquals("claude", resp.getProvider());
            assertEquals(100, resp.getTokensIn());
            assertEquals(50, resp.getTokensOut());
            assertEquals(250, resp.getLatencyMs());
            assertEquals(3.5, resp.getCostCents(), 0.001);
        }

        @Test
        @DisplayName("ok() with empty content is still success")
        void test_ok_empty_content()
        {
            LLMResponse resp = LLMResponse.ok("", "model", "prov", 0, 0, 0, 0);
            assertTrue(resp.isSuccess());
            assertEquals("", resp.getContent());
        }

        @Test
        @DisplayName("ok() has no error message")
        void test_ok_no_error()
        {
            LLMResponse resp = LLMResponse.ok("x", "m", "p", 1, 1, 1, 0);
            assertNull(resp.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("LLMResponse.error — Error responses")
    class TestError
    {
        @Test
        @DisplayName("error() creates error response")
        void test_error_basic()
        {
            LLMResponse resp = LLMResponse.error("claude", "claude-sonnet-4",
                                                  "Rate limit exceeded", 1500);
            assertFalse(resp.isSuccess());
            assertEquals("Rate limit exceeded", resp.getErrorMessage());
            assertEquals("claude", resp.getProvider());
            assertEquals("claude-sonnet-4", resp.getModel());
            assertEquals(1500, resp.getLatencyMs());
        }

        @Test
        @DisplayName("error() has no content")
        void test_error_no_content()
        {
            LLMResponse resp = LLMResponse.error("p", "m", "err", 0);
            assertNull(resp.getContent());
        }

        @Test
        @DisplayName("error() has zero tokens and cost")
        void test_error_zero_stats()
        {
            LLMResponse resp = LLMResponse.error("p", "m", "err", 0);
            assertEquals(0, resp.getTokensIn());
            assertEquals(0, resp.getTokensOut());
            assertEquals(0.0, resp.getCostCents(), 0.001);
        }
    }

    @Nested
    @DisplayName("Setters — Mutable fields for post-hoc override")
    class TestSetters
    {
        @Test
        @DisplayName("setProvider overrides provider name")
        void test_set_provider()
        {
            LLMResponse resp = LLMResponse.ok("x", "m", null, 1, 1, 1, 0);
            assertNull(resp.getProvider());
            resp.setProvider("new-provider");
            assertEquals("new-provider", resp.getProvider());
        }

        @Test
        @DisplayName("setModel overrides model name")
        void test_set_model()
        {
            LLMResponse resp = LLMResponse.ok("x", "old-model", "p", 1, 1, 1, 0);
            resp.setModel("new-model");
            assertEquals("new-model", resp.getModel());
        }
    }
}
