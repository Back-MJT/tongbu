package com.xindong.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for LLMConfigLoader.
 * Ticket: XIN-93
 */
class LLMConfigLoaderTest
{
    @Nested
    @DisplayName("parseConfig — YAML config parsing")
    class TestParseConfig
    {
        @Test
        @DisplayName("Parse valid config with two providers")
        void test_parse_valid()
        {
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> providers = new HashMap<>();

            Map<String, Object> claude = new HashMap<>();
            claude.put("name", "claude");
            claude.put("model", "claude-sonnet-4");
            claude.put("base_url", "https://api.anthropic.com");
            claude.put("temperature", 0.0);
            claude.put("max_tokens", 1024);
            claude.put("timeout_seconds", 30);
            claude.put("enabled", true);
            claude.put("priority", 10);
            claude.put("api_key_env", "ANTHROPIC_API_KEY");

            Map<String, Object> pricing = new HashMap<>();
            pricing.put("input_per_mtok_cents", 300.0);
            pricing.put("output_per_mtok_cents", 1500.0);
            claude.put("pricing", pricing);
            claude.put("daily_cost_cap_cents", 500000.0);

            providers.put("claude", claude);
            root.put("providers", providers);

            List<ProviderConfig> configs = LLMConfigLoader.parseConfig(root);
            assertEquals(1, configs.size());
            ProviderConfig cfg = configs.get(0);
            assertEquals("claude", cfg.getName());
            assertEquals("claude-sonnet-4", cfg.getModel());
            assertEquals(300.0, cfg.getInputPricePerMtokCents(), 0.01);
            assertEquals(1500.0, cfg.getOutputPricePerMtokCents(), 0.01);
            assertEquals(10, cfg.getPriority());
        }

        @Test
        @DisplayName("Disabled providers are excluded")
        void test_parse_disabled_excluded()
        {
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> providers = new HashMap<>();

            Map<String, Object> mock = new HashMap<>();
            mock.put("name", "mock");
            mock.put("model", "mock-model");
            mock.put("enabled", false);
            mock.put("priority", 50);

            providers.put("mock", mock);
            root.put("providers", providers);

            List<ProviderConfig> configs = LLMConfigLoader.parseConfig(root);
            assertTrue(configs.isEmpty());
        }

        @Test
        @DisplayName("Parse empty providers map")
        void test_parse_empty()
        {
            Map<String, Object> root = new HashMap<>();
            root.put("providers", new HashMap<>());
            List<ProviderConfig> configs = LLMConfigLoader.parseConfig(root);
            assertTrue(configs.isEmpty());
        }

        @Test
        @DisplayName("Parse config with missing providers key")
        void test_parse_no_providers_key()
        {
            Map<String, Object> root = new HashMap<>();
            List<ProviderConfig> configs = LLMConfigLoader.parseConfig(root);
            assertTrue(configs.isEmpty());
        }

        @Test
        @DisplayName("Priority sorting works")
        void test_priority_sorting()
        {
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> providers = new HashMap<>();

            Map<String, Object> p1 = new HashMap<>();
            p1.put("name", "slow");
            p1.put("model", "m1");
            p1.put("priority", 50);
            p1.put("enabled", true);

            Map<String, Object> p2 = new HashMap<>();
            p2.put("name", "fast");
            p2.put("model", "m2");
            p2.put("priority", 5);
            p2.put("enabled", true);

            providers.put("slow", p1);
            providers.put("fast", p2);
            root.put("providers", providers);

            List<ProviderConfig> configs = LLMConfigLoader.parseConfig(root);
            assertEquals(2, configs.size());
            assertEquals("fast", configs.get(0).getName());
            assertEquals("slow", configs.get(1).getName());
        }

        @Test
        @DisplayName("Default values used when fields missing")
        void test_defaults()
        {
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> providers = new HashMap<>();

            Map<String, Object> minimal = new HashMap<>();
            minimal.put("name", "minimal");
            minimal.put("enabled", true);

            providers.put("minimal", minimal);
            root.put("providers", providers);

            List<ProviderConfig> configs = LLMConfigLoader.parseConfig(root);
            assertEquals(1, configs.size());
            ProviderConfig cfg = configs.get(0);
            assertEquals(0.0, cfg.getTemperature(), 0.001);
            assertEquals(1024, cfg.getMaxTokens());
            assertEquals(30, cfg.getTimeoutSeconds());
            assertEquals(100, cfg.getPriority());
        }
    }

    @Nested
    @DisplayName("createProvider — Provider factory")
    class TestCreateProvider
    {
        @Test
        @DisplayName("Creates ClaudeProvider for 'claude'")
        void test_create_claude()
        {
            ProviderConfig cfg = new ProviderConfig("claude", "key", "claude-sonnet-4");
            LLMProvider prov = LLMConfigLoader.createProvider(cfg);
            assertNotNull(prov);
            assertTrue(prov instanceof ClaudeProvider);
            assertEquals("claude", prov.name());
        }

        @Test
        @DisplayName("Creates OpenAIProvider for 'openai'")
        void test_create_openai()
        {
            ProviderConfig cfg = new ProviderConfig("openai", "key", "gpt-4o");
            LLMProvider prov = LLMConfigLoader.createProvider(cfg);
            assertNotNull(prov);
            assertTrue(prov instanceof OpenAIProvider);
        }

        @Test
        @DisplayName("Creates ZaiProvider for 'zai'")
        void test_create_zai()
        {
            ProviderConfig cfg = new ProviderConfig("zai", "key", "glm-4-flash");
            LLMProvider prov = LLMConfigLoader.createProvider(cfg);
            assertNotNull(prov);
            assertTrue(prov instanceof ZaiProvider);
        }

        @Test
        @DisplayName("Creates MockProvider for 'mock'")
        void test_create_mock()
        {
            ProviderConfig cfg = new ProviderConfig("mock", "", "mock");
            LLMProvider prov = LLMConfigLoader.createProvider(cfg);
            assertNotNull(prov);
            assertTrue(prov instanceof MockProvider);
        }

        @Test
        @DisplayName("Returns null for unknown provider")
        void test_create_unknown()
        {
            ProviderConfig cfg = new ProviderConfig("unknown", "key", "model");
            LLMProvider prov = LLMConfigLoader.createProvider(cfg);
            assertNull(prov);
        }
    }

    @Nested
    @DisplayName("createEngine — Full engine assembly")
    class TestCreateEngine
    {
        @Test
        @DisplayName("Creates engine with providers and sets active")
        void test_create_engine()
        {
            ProviderConfig cfgA = new ProviderConfig("mock", "", "mock-model");
            cfgA.setPriority(10);
            cfgA.setEnabled(true);

            List<ProviderConfig> configs = List.of(cfgA);
            LLMEngine engine = LLMConfigLoader.createEngine(configs);

            assertNotNull(engine);
            assertEquals("mock", engine.getActiveProviderName());
            assertTrue(engine.getProviderNames().contains("mock"));
        }
    }

    @Nested
    @DisplayName("loadFromClasspath — Classpath YAML loading")
    class TestClasspathLoad
    {
        @Test
        @DisplayName("Loads config from classpath without error")
        void test_classpath_load()
        {
            // Should not throw — the YAML file exists in resources
            List<ProviderConfig> configs = LLMConfigLoader.loadFromClasspath();
            // At least claude and zai should be loaded (they're enabled)
            assertTrue(configs.size() >= 1, "Should load at least one provider from YAML");
        }
    }
}
