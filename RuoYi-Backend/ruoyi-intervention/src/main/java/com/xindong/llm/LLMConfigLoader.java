package com.xindong.llm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * LLM配置加载器 / Loads LLM provider configuration from YAML.
 *
 * <p>Reads llm-providers.yaml from classpath and creates {@link ProviderConfig} instances.
 * API keys are resolved from environment variables (api_key_env field).
 *
 * <p>Ticket: XIN-93
 */
public class LLMConfigLoader
{
    private static final Logger log = LoggerFactory.getLogger(LLMConfigLoader.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 从classpath加载配置 / Load provider configs from classpath YAML.
     *
     * @return list of ProviderConfig, or empty list if file not found
     */
    @SuppressWarnings("unchecked")
    public static List<ProviderConfig> loadFromClasspath()
    {
        try (InputStream is = LLMConfigLoader.class.getClassLoader()
                .getResourceAsStream("llm-providers.yaml"))
        {
            if (is == null)
            {
                log.info("loadFromClasspath: llm-providers.yaml not found, returning empty");
                return new ArrayList<>();
            }

            Map<String, Object> root = yamlMapper.readValue(is, Map.class);
            return parseConfig(root);
        }
        catch (Exception e)
        {
            log.error("loadFromClasspath: failed to load config: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析YAML配置 / Parse YAML config map into ProviderConfig list.
     */
    @SuppressWarnings("unchecked")
    public static List<ProviderConfig> parseConfig(Map<String, Object> root)
    {
        List<ProviderConfig> configs = new ArrayList<>();

        Object providersObj = root.get("providers");
        if (!(providersObj instanceof Map))
        {
            return configs;
        }

        Map<String, Object> providers = (Map<String, Object>) providersObj;
        for (Map.Entry<String, Object> entry : providers.entrySet())
        {
            try
            {
                Map<String, Object> provMap = (Map<String, Object>) entry.getValue();
                ProviderConfig cfg = new ProviderConfig();
                cfg.setName(getString(provMap, "name", entry.getKey()));
                cfg.setModel(getString(provMap, "model", ""));
                cfg.setBaseUrl(getString(provMap, "base_url", null));
                cfg.setTemperature(getDouble(provMap, "temperature", 0.0));
                cfg.setMaxTokens(getInt(provMap, "max_tokens", 1024));
                cfg.setTimeoutSeconds(getInt(provMap, "timeout_seconds", 30));
                cfg.setEnabled(getBool(provMap, "enabled", true));
                cfg.setPriority(getInt(provMap, "priority", 100));

                // Resolve API key from env var
                String apiKeyEnv = getString(provMap, "api_key_env", null);
                if (apiKeyEnv != null)
                {
                    String apiKey = System.getenv(apiKeyEnv);
                    cfg.setApiKey(apiKey != null ? apiKey : "");
                }

                // Pricing
                Object pricingObj = provMap.get("pricing");
                if (pricingObj instanceof Map)
                {
                    Map<String, Object> pricing = (Map<String, Object>) pricingObj;
                    cfg.setInputPricePerMtokCents(getDouble(pricing, "input_per_mtok_cents", 300.0));
                    cfg.setOutputPricePerMtokCents(getDouble(pricing, "output_per_mtok_cents", 1500.0));
                }

                // Daily cost cap
                cfg.setDailyCostCapCents(getDouble(provMap, "daily_cost_cap_cents", 0));

                if (cfg.isEnabled())
                {
                    configs.add(cfg);
                    log.debug("parseConfig: loaded {}", cfg);
                }
            }
            catch (Exception e)
            {
                log.warn("parseConfig: skipping provider {}: {}", entry.getKey(), e.getMessage());
            }
        }

        // Sort by priority
        configs.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        return configs;
    }

    /**
     * 从配置创建引擎并注册提供者 / Create an LLMEngine from loaded configs.
     */
    public static LLMEngine createEngine(List<ProviderConfig> configs)
    {
        LLMEngine engine = new LLMEngine();
        for (ProviderConfig cfg : configs)
        {
            LLMProvider provider = createProvider(cfg);
            if (provider != null)
            {
                engine.registerProvider(provider);
            }
        }
        // Set first (highest priority) as active
        if (!configs.isEmpty())
        {
            engine.setActiveProvider(configs.get(0).getName());
        }
        return engine;
    }

    /**
     * 根据配置创建提供者实例 / Create a provider instance from config.
     */
    public static LLMProvider createProvider(ProviderConfig cfg)
    {
        switch (cfg.getName())
        {
            case "claude":
                return new ClaudeProvider(cfg);
            case "openai":
                return new OpenAIProvider(cfg);
            case "zai":
                return new ZaiProvider(cfg);
            case "mock":
                return new MockProvider(cfg);
            default:
                log.warn("createProvider: unknown provider '{}', skipping", cfg.getName());
                return null;
        }
    }

    // ========== Helpers ==========

    private static String getString(Map<String, Object> map, String key, String defaultVal)
    {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultVal)
    {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultVal;
    }

    private static double getDouble(Map<String, Object> map, String key, double defaultVal)
    {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultVal;
    }

    private static boolean getBool(Map<String, Object> map, String key, boolean defaultVal)
    {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultVal;
    }
}
