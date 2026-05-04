package com.xindong.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM引擎 — 统一多模型调用网关 / Unified multi-model LLM gateway.
 *
 * <p>Central routing engine that manages multiple LLM providers with:
 * <ul>
 *   <li>Hot-swap: switch active provider at runtime without restart</li>
 *   <li>Fallback chain: auto-failover to next provider on error/unhealthy</li>
 *   <li>Cost tracking: per-provider cumulative cost in US cents</li>
 *   <li>Cache: optional response cache to reduce API spend</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   LLMEngine engine = new LLMEngine();
 *   engine.registerProvider(new ClaudeProvider(config));
 *   engine.registerProvider(new ZaiProvider(config));
 *   engine.setActiveProvider("claude");
 *
 *   LLMResponse resp = engine.call("You are a coach.", "User data...", 1024);
 * </pre>
 *
 * <p>Board v1.1 Layer 2 (Claude API Training Engine) — now provider-agnostic.
 * Ticket: XIN-93
 */
public class LLMEngine
{
    private static final Logger log = LoggerFactory.getLogger(LLMEngine.class);

    // ========== Provider Registry ==========

    /** 已注册提供者 / Registered providers by name */
    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();

    /** 当前活跃提供者名称 / Active provider name */
    private volatile String activeProviderName;

    /** 回退顺序 / Fallback order (provider names, priority-sorted) */
    private final List<String> fallbackOrder = new ArrayList<>();

    // ========== Cache ==========

    /** 响应缓存 / Simple hash-based response cache */
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    /** 缓存TTL(分钟) / Cache TTL in minutes */
    private int cacheTtlMinutes = 60;

    // ========== Cumulative Stats ==========

    /** 总调用次数 / Total call count across all providers */
    private volatile long totalCalls = 0;

    /** 总成本(美分) / Total cost in US cents across all providers */
    private volatile double totalCostCents = 0;

    /** 总缓存命中 / Total cache hits */
    private volatile long totalCacheHits = 0;

    // ========== Core API ==========

    /**
     * 注册提供者 / Register an LLM provider.
     * Provider is added to the fallback chain based on its config priority.
     */
    public void registerProvider(LLMProvider provider)
    {
        providers.put(provider.name(), provider);
        rebuildFallbackOrder();
        log.info("registerProvider: {} registered (model={})", provider.name(), provider.getModel());
    }

    /**
     * 注销提供者 / Unregister an LLM provider.
     */
    public void unregisterProvider(String name)
    {
        providers.remove(name);
        fallbackOrder.remove(name);
        if (activeProviderName != null && activeProviderName.equals(name))
        {
            activeProviderName = fallbackOrder.isEmpty() ? null : fallbackOrder.get(0);
            log.warn("unregisterProvider: {} was active, switched to {}", name, activeProviderName);
        }
    }

    /**
     * 设置活跃提供者(热切换) / Set the active provider (hot-swap).
     * Takes effect immediately for subsequent calls.
     *
     * @param name 提供者名称
     * @throws IllegalArgumentException if provider not registered
     */
    public void setActiveProvider(String name)
    {
        if (!providers.containsKey(name))
        {
            throw new IllegalArgumentException("Provider not registered: " + name);
        }
        this.activeProviderName = name;
        log.info("setActiveProvider: switched to {}", name);
    }

    /**
     * 核心调用 — 路由到活跃提供者 + 回退 / Core call with fallback chain.
     *
     * <p>Flow:
     * <ol>
     *   <li>Check cache → return if hit</li>
     *   <li>Call active provider</li>
     *   <li>If fails, try fallback providers in priority order</li>
     *   <li>Record stats and return</li>
     * </ol>
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @param maxTokens    最大输出token
     * @return LLMResponse
     */
    public LLMResponse call(String systemPrompt, String userPrompt, int maxTokens)
    {
        totalCalls++;

        // 1. Cache check
        String cacheKey = computeCacheKey(systemPrompt, userPrompt, maxTokens);
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes))
        {
            totalCacheHits++;
            log.debug("call: cache hit");
            return cached.response;
        }

        // 2. Build try-order: active first, then fallbacks
        List<String> tryOrder = new ArrayList<>();
        if (activeProviderName != null && providers.containsKey(activeProviderName))
        {
            tryOrder.add(activeProviderName);
        }
        for (String fb : fallbackOrder)
        {
            if (!tryOrder.contains(fb))
            {
                tryOrder.add(fb);
            }
        }

        // 3. Try providers in order
        LLMResponse lastError = null;
        for (String provName : tryOrder)
        {
            LLMProvider provider = providers.get(provName);
            if (provider == null || !provider.isHealthy())
            {
                log.debug("call: skipping {} (not healthy)", provName);
                continue;
            }

            try
            {
                LLMResponse response = provider.call(systemPrompt, userPrompt, maxTokens);
                if (response.isSuccess())
                {
                    totalCostCents += response.getCostCents();

                    // Cache the successful response
                    cache.put(cacheKey, new CachedResponse(response));

                    log.debug("call: {} succeeded, latency={}ms, cost={:.2f} cents",
                              provName, response.getLatencyMs(), response.getCostCents());
                    return response;
                }
                else
                {
                    lastError = response;
                    log.warn("call: {} returned error: {}", provName, response.getErrorMessage());
                }
            }
            catch (Exception e)
            {
                lastError = LLMResponse.error(provName, provider.getModel(),
                                              e.getMessage(), 0);
                log.error("call: {} threw exception: {}", provName, e.getMessage());
            }
        }

        // All providers failed
        if (lastError == null)
        {
            lastError = LLMResponse.error("none", "none",
                                          "No providers registered or all unhealthy", 0);
        }
        return lastError;
    }

    /**
     * 简化调用(使用默认maxTokens) / Simplified call with default maxTokens.
     */
    public LLMResponse call(String systemPrompt, String userPrompt)
    {
        return call(systemPrompt, userPrompt, 1024);
    }

    // ========== Cache Management ==========

    /**
     * 失效全部缓存 / Invalidate all cache entries.
     * @return number of entries cleared
     */
    public int invalidateCache()
    {
        int count = cache.size();
        cache.clear();
        return count;
    }

    /**
     * 失效指定key的缓存 / Invalidate cache for a specific prompt combination.
     */
    public void invalidateCache(String systemPrompt, String userPrompt, int maxTokens)
    {
        String key = computeCacheKey(systemPrompt, userPrompt, maxTokens);
        cache.remove(key);
    }

    /**
     * 设置缓存TTL / Set cache time-to-live in minutes.
     */
    public void setCacheTtlMinutes(int minutes)
    {
        this.cacheTtlMinutes = minutes;
    }

    // ========== Health & Monitoring ==========

    /**
     * 运行所有提供者健康检查 / Run health checks on all providers.
     */
    public void healthCheckAll()
    {
        for (LLMProvider provider : providers.values())
        {
            try
            {
                provider.healthCheck();
            }
            catch (Exception e)
            {
                log.warn("healthCheckAll: {} failed: {}", provider.name(), e.getMessage());
            }
        }
    }

    /**
     * 获取引擎统计 / Get engine-wide statistics.
     */
    public Map<String, Object> getStats()
    {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_calls", totalCalls);
        stats.put("total_cost_cents", totalCostCents);
        stats.put("total_cache_hits", totalCacheHits);
        stats.put("cache_hit_rate", totalCalls > 0 ? (double) totalCacheHits / totalCalls : 0.0);
        stats.put("cache_entries", cache.size());
        stats.put("active_provider", activeProviderName);
        stats.put("registered_providers", providers.keySet());

        // Per-provider stats
        Map<String, Map<String, Object>> perProvider = new LinkedHashMap<>();
        for (Map.Entry<String, LLMProvider> entry : providers.entrySet())
        {
            Map<String, Object> pStats = new LinkedHashMap<>();
            LLMProvider p = entry.getValue();
            pStats.put("model", p.getModel());
            pStats.put("healthy", p.isHealthy());
            pStats.put("calls", p.getCallCount());
            pStats.put("cost_cents", p.getCumulativeCostCents());
            perProvider.put(entry.getKey(), pStats);
        }
        stats.put("providers", perProvider);

        return stats;
    }

    /**
     * 获取活跃提供者名称 / Get the active provider name.
     */
    public String getActiveProviderName()
    {
        return activeProviderName;
    }

    /**
     * 获取已注册提供者名称列表 / Get list of registered provider names.
     */
    public List<String> getProviderNames()
    {
        return new ArrayList<>(providers.keySet());
    }

    /**
     * 获取回退顺序 / Get the current fallback order.
     */
    public List<String> getFallbackOrder()
    {
        return new ArrayList<>(fallbackOrder);
    }

    // ========== Internal ==========

    /**
     * 重建回退顺序 / Rebuild fallback order from provider priorities.
     */
    private void rebuildFallbackOrder()
    {
        fallbackOrder.clear();
        List<LLMProvider> sorted = new ArrayList<>(providers.values());
        sorted.sort(Comparator.comparingInt(p ->
        {
            // Providers without ProviderConfig get default priority
            if (p instanceof AbstractLLMProvider)
            {
                AbstractLLMProvider ap = (AbstractLLMProvider) p;
                return ap.getConfig() != null ? ap.getConfig().getPriority() : 100;
            }
            return 100;
        }));
        for (LLMProvider p : sorted)
        {
            fallbackOrder.add(p.name());
        }
    }

    /**
     * 计算缓存键 / Compute cache key from prompt content.
     */
    private static String computeCacheKey(String systemPrompt, String userPrompt, int maxTokens)
    {
        // Simple but effective: hash the content
        int hash = 31 * systemPrompt.hashCode() + userPrompt.hashCode();
        hash = 31 * hash + maxTokens;
        return String.valueOf(hash);
    }

    /**
     * 重置所有状态 / Reset all state (testing only).
     */
    public void resetAll()
    {
        totalCalls = 0;
        totalCostCents = 0;
        totalCacheHits = 0;
        cache.clear();
        for (LLMProvider p : providers.values())
        {
            p.resetStats();
        }
    }

    // ========== Inner Classes ==========

    /**
     * 缓存响应包装 / Cached response with TTL.
     */
    private static class CachedResponse
    {
        final LLMResponse response;
        final long cachedAtMs;

        CachedResponse(LLMResponse response)
        {
            this.response = response;
            this.cachedAtMs = System.currentTimeMillis();
        }

        boolean isExpired(int ttlMinutes)
        {
            long ttlMs = ttlMinutes * 60_000L;
            if (ttlMs <= 0) return true; // 0 or negative TTL = always expired
            return System.currentTimeMillis() - cachedAtMs > ttlMs;
        }
    }
}
