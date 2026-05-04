package com.ruoyi.intervention.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Claude API引擎 — AI训练引擎核心(callClaude()引擎)
 * Unified call routing for all Claude API interactions with:
 * <ul>
 *   <li>Deterministic output (temperature=0)</li>
 *   <li>Complete logging (prompt, response, tokens, cost, latency)</li>
 *   <li>Response caching (same user data → cached result, target >80% hit rate)</li>
 *   <li>Cost alerting (daily spend >¥500 triggers alert)</li>
 * </ul>
 *
 * <p>Per Board v1.1 Section 2.3 / 3.3:
 * <ul>
 *   <li>AI call logs table: ai_call_logs</li>
 *   <li>Model: claude-sonnet-4-20250514</li>
 *   <li>Temperature: 0</li>
 * </ul>
 *
 * <p>Phase 1 uses in-memory storage. Phase 2 will migrate to PostgreSQL + Redis.
 *
 * Migrated from: intervention-engine/src/algorithms/claude_engine.py
 * Ticket: XIN-83
 */
@Service
public class ClaudeEngineService
{
    private static final Logger log = LoggerFactory.getLogger(ClaudeEngineService.class);

    // ========== Call Status Constants ==========

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_CACHED = "cached";

    // ========== Alert Level Constants ==========

    public static final String ALERT_NONE = "none";
    public static final String ALERT_WARNING = "warning";
    public static final String ALERT_CRITICAL = "critical";

    // ========== Configuration ==========

    /** Claude模型标识 / Claude model identifier */
    private String model = "claude-sonnet-4-20250514";

    /** 采样温度(0=确定性) / Sampling temperature (0 = deterministic) */
    private double temperature = 0.0;

    /** 默认最大输出token / Default max output tokens */
    private int defaultMaxTokens = 1024;

    /** 日花费限制(USD) / Daily cost limit in USD (~¥500 at ~7.1 CNY/USD) */
    private double dailyCostLimitUsd = 70.0;

    /** 告警阈值比例 / Fraction of daily limit that triggers warning */
    private double warningThreshold = 0.8;

    /** 缓存TTL(分钟) / Cache time-to-live in minutes */
    private int cacheTtlMinutes = 60;

    /** 每百万输入token价格(USD) / USD per million input tokens */
    private double inputPricePerMtok = 3.0;

    /** 每百万输出token价格(USD) / USD per million output tokens */
    private double outputPricePerMtok = 15.0;

    // ========== In-Memory Storage ==========

    /** API调用日志 / List of all call logs */
    private final List<CallLogRecord> callLogs = new ArrayList<>();

    /** 响应缓存 / cache_key → CacheEntry */
    private final Map<String, CacheEntry> cache = new HashMap<>();

    /** 费用告警 / List of cost alerts */
    private final List<CostAlertRecord> costAlerts = new ArrayList<>();

    /** 告警回调 / Registered alert callbacks */
    private final List<Consumer<CostAlertRecord>> alertCallbacks = new ArrayList<>();

    /** 外部API客户端(可选) / External API client interface */
    private ApiClient apiClient;

    // ========== API Client Interface ==========

    /**
     * 外部API客户端接口 / Interface for external Claude API client.
     *
     * <p>Implement this to connect to the real Claude API.
     */
    @FunctionalInterface
    public interface ApiClient
    {
        /**
         * 调用Claude API / Call Claude API.
         *
         * @param model       模型标识
         * @param system      系统提示
         * @param user        用户提示
         * @param temperature 温度
         * @param maxTokens   最大token数
         * @return API结果 / API result with response, input_tokens, output_tokens
         */
        Map<String, Object> call(String model, String system, String user,
                                 double temperature, int maxTokens);
    }

    // ========== Core API ==========

    /**
     * 核心API调用函数 — 所有AI调用必须经过此函数
     * Core routing function — all AI calls must go through here.
     *
     * <p>Flow:
     * <ol>
     *   <li>Check cache → return cached response if hit</li>
     *   <li>Call external API client (or mock)</li>
     *   <li>Log the call (prompt, response, tokens, cost, latency)</li>
     *   <li>Check cost alerts</li>
     *   <li>Store response in cache</li>
     *   <li>Return the log record</li>
     * </ol>
     *
     * @param systemPrompt  系统提示 / System prompt (version-managed)
     * @param userPrompt    用户提示 / User context / data prompt
     * @param promptVersion 提示版本 / Version tag of the system prompt
     * @param userId        用户ID / User identifier
     * @return 调用日志记录 / CallLogRecord with response and metadata
     */
    public Map<String, Object> call(String systemPrompt, String userPrompt,
                                     String promptVersion, String userId)
    {
        return call(systemPrompt, userPrompt, promptVersion, userId,
                    "general", defaultMaxTokens, null);
    }

    /**
     * 完整参数的API调用 / Full-parameter API call.
     *
     * @param systemPrompt  系统提示
     * @param userPrompt    用户提示
     * @param promptVersion 提示版本
     * @param userId        用户ID
     * @param callType      调用类型 (training_plan, recovery, alert, stage_assessment, general)
     * @param maxTokens     最大输出token
     * @param metadata      额外元数据
     * @return Map with "response" and "call_log_id"
     */
    public Map<String, Object> call(String systemPrompt, String userPrompt,
                                     String promptVersion, String userId,
                                     String callType, int maxTokens,
                                     Map<String, Object> metadata)
    {
        // 1. Cache check
        String cacheKey = computeCacheKey(userId, systemPrompt, userPrompt, promptVersion, callType);
        String cachedResponse = checkCache(cacheKey);
        if (cachedResponse != null)
        {
            CallLogRecord logRecord = new CallLogRecord();
            logRecord.setId(UUID.randomUUID().toString());
            logRecord.setCreatedAt(LocalDateTime.now());
            logRecord.setUserId(userId);
            logRecord.setCallType(callType);
            logRecord.setSystemPrompt(systemPrompt);
            logRecord.setUserPrompt(userPrompt);
            logRecord.setResponseText(cachedResponse);
            logRecord.setStatus(STATUS_CACHED);
            logRecord.setCacheHit(true);
            logRecord.setSystemPromptVersion(promptVersion);
            callLogs.add(logRecord);

            Map<String, Object> result = new HashMap<>();
            result.put("response", cachedResponse);
            result.put("call_log_id", logRecord.getId());
            log.debug("call: cache hit for userId={}, version={}", userId, promptVersion);
            return result;
        }

        // 2. API call
        long startTime = System.currentTimeMillis();
        CallLogRecord logRecord = new CallLogRecord();
        logRecord.setId(UUID.randomUUID().toString());
        logRecord.setCreatedAt(LocalDateTime.now());
        logRecord.setUserId(userId);
        logRecord.setCallType(callType);
        logRecord.setSystemPrompt(systemPrompt);
        logRecord.setUserPrompt(userPrompt);
        logRecord.setSystemPromptVersion(promptVersion);

        Map<String, Object> result = new HashMap<>();

        try
        {
            String responseText;
            int inputTokens;
            int outputTokens;

            if (apiClient != null)
            {
                Map<String, Object> apiResult = apiClient.call(model, systemPrompt, userPrompt,
                                                                temperature, maxTokens);
                responseText = (String) apiResult.get("response");
                inputTokens = apiResult.containsKey("input_tokens") ?
                              ((Number) apiResult.get("input_tokens")).intValue() : 0;
                outputTokens = apiResult.containsKey("output_tokens") ?
                               ((Number) apiResult.get("output_tokens")).intValue() : 0;
            }
            else
            {
                // Mock mode (testing/development)
                String[] mockResult = mockCall(systemPrompt, userPrompt);
                responseText = mockResult[0];
                inputTokens = Integer.parseInt(mockResult[1]);
                outputTokens = Integer.parseInt(mockResult[2]);
            }

            long latencyMs = System.currentTimeMillis() - startTime;
            double cost = calculateCost(inputTokens, outputTokens);

            logRecord.setResponseText(responseText);
            logRecord.setInputTokens(inputTokens);
            logRecord.setOutputTokens(outputTokens);
            logRecord.setCostUsd(cost);
            logRecord.setLatencyMs(latencyMs);
            logRecord.setStatus(STATUS_SUCCESS);

            // 4. Cost alert check
            double dailyCost = getTodayCost() + cost;
            CostAlertRecord alert = checkCostAlert(dailyCost);
            if (alert != null)
            {
                fireAlert(alert);
            }

            // 5. Cache store
            storeCache(cacheKey, responseText);

            result.put("response", responseText);
            result.put("call_log_id", logRecord.getId());
            log.info("call: userId={}, type={}, latency={}ms, cost=${}",
                     userId, callType, latencyMs, String.format("%.4f", cost));
        }
        catch (Exception e)
        {
            long latencyMs = System.currentTimeMillis() - startTime;
            logRecord.setStatus(STATUS_ERROR);
            logRecord.setErrorMessage(e.getMessage());
            logRecord.setLatencyMs(latencyMs);

            result.put("response", "");
            result.put("call_log_id", logRecord.getId());
            result.put("error", e.getMessage());
            log.error("call: error for userId={}: {}", userId, e.getMessage());
        }

        // 3. Log
        callLogs.add(logRecord);
        return result;
    }

    // ========== Cache Functions ==========

    /**
     * 计算缓存键 / Compute cache key from request content.
     *
     * <p>Key includes: user_id, system_prompt, user_prompt, system_prompt_version, call_type.
     */
    private String computeCacheKey(String userId, String systemPrompt, String userPrompt,
                                    String promptVersion, String callType)
    {
        try
        {
            String raw = String.format("{\"ct\":\"%s\",\"sp\":\"%s\",\"sv\":\"%s\",\"uid\":\"%s\",\"up\":\"%s\"}",
                                       callType, systemPrompt, promptVersion, userId, userPrompt);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash)
            {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }
        catch (Exception e)
        {
            // Fallback to simple hash
            return String.valueOf(rawHashCode(userId, systemPrompt, userPrompt, promptVersion, callType));
        }
    }

    private static int rawHashCode(String... parts)
    {
        int h = 0;
        for (String p : parts)
        {
            h = 31 * h + (p == null ? 0 : p.hashCode());
        }
        return h;
    }

    /**
     * 检查缓存 / Check if a cached response exists and is still valid.
     */
    private String checkCache(String key)
    {
        CacheEntry entry = cache.get(key);
        if (entry == null)
        {
            return null;
        }
        long ttlSeconds = cacheTtlMinutes * 60L;
        if (System.currentTimeMillis() - entry.timestamp > ttlSeconds * 1000)
        {
            cache.remove(key);
            return null;
        }
        return entry.response;
    }

    /**
     * 存储缓存 / Store a response in cache.
     */
    private void storeCache(String key, String response)
    {
        cache.put(key, new CacheEntry(response, System.currentTimeMillis()));
    }

    /**
     * 失效缓存 / Invalidate cache entries.
     *
     * @param userId 用户ID(可选，null则清除全部)
     * @return 失效的条目数
     */
    public int invalidateCache(String userId)
    {
        int count = cache.size();
        cache.clear();
        log.debug("invalidateCache: cleared {} entries", count);
        return count;
    }

    // ========== Cost Tracking ==========

    /**
     * 计算API调用费用 / Calculate API call cost in USD.
     */
    private double calculateCost(int inputTokens, int outputTokens)
    {
        double inputCost = (inputTokens / 1_000_000.0) * inputPricePerMtok;
        double outputCost = (outputTokens / 1_000_000.0) * outputPricePerMtok;
        return inputCost + outputCost;
    }

    /**
     * 获取今日总费用 / Get total cost for today.
     */
    private double getTodayCost()
    {
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return callLogs.stream()
            .filter(l -> l.getCreatedAt() != null
                      && l.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE).equals(today)
                      && (STATUS_SUCCESS.equals(l.getStatus()) || STATUS_CACHED.equals(l.getStatus())))
            .mapToDouble(CallLogRecord::getCostUsd)
            .sum();
    }

    /**
     * 检查费用告警 / Check if current daily cost triggers an alert.
     */
    private CostAlertRecord checkCostAlert(double currentCost)
    {
        if (currentCost >= dailyCostLimitUsd)
        {
            return new CostAlertRecord(
                ALERT_CRITICAL, currentCost, dailyCostLimitUsd,
                String.format("Daily cost $%.2f exceeded limit $%.2f", currentCost, dailyCostLimitUsd)
            );
        }
        else if (currentCost >= dailyCostLimitUsd * warningThreshold)
        {
            return new CostAlertRecord(
                ALERT_WARNING, currentCost, dailyCostLimitUsd,
                String.format("Daily cost $%.2f approaching limit $%.2f", currentCost, dailyCostLimitUsd)
            );
        }
        return null;
    }

    /**
     * 触发告警 / Fire a cost alert to all registered callbacks.
     */
    private void fireAlert(CostAlertRecord alert)
    {
        costAlerts.add(alert);
        for (Consumer<CostAlertRecord> callback : alertCallbacks)
        {
            try
            {
                callback.accept(alert);
            }
            catch (Exception e)
            {
                // Don't let alert callbacks break the engine
            }
        }
        log.warn("fireAlert: level={}, cost=${}", alert.level, String.format("%.2f", alert.dailyCostUsd));
    }

    // ========== Mock Call ==========

    /**
     * 模拟API调用(测试/开发) / Mock API call for testing/development.
     */
    private static String[] mockCall(String systemPrompt, String userPrompt)
    {
        // Estimate tokens roughly (1 token ≈ 3 chars for mixed Chinese/English)
        int inputEstimate = systemPrompt.length() / 3 + userPrompt.length() / 3;
        int outputEstimate = 200;
        String response =
            "1. 建议本周进行3次中等强度有氧训练，每次30分钟。理由：您目前处于成长期，"
            + "心肺功能稳步提升，适当增加训练频率可以持续改善。\n"
            + "2. 尝试加入1次力量训练，重点关注上肢肌群。理由：您主要使用跑步机，"
            + "上肢锻炼不足，平衡训练有助于全面发展。\n"
            + "3. 训练后增加5分钟拉伸放松。理由：近两周恢复指标偏低，"
            + "拉伸有助于减少肌肉酸痛，提高下次训练质量。";
        return new String[]{response, String.valueOf(inputEstimate), String.valueOf(outputEstimate)};
    }

    // ========== Query Functions ==========

    /**
     * 查询调用日志 / Query call logs with optional filters.
     *
     * @param userId    用户ID过滤
     * @param callType  调用类型过滤
     * @param status    状态过滤
     * @param limit     最大返回数量
     * @return 调用日志列表
     */
    public List<CallLogRecord> getCallLogs(String userId, String callType, String status, int limit)
    {
        List<CallLogRecord> filtered = new ArrayList<>(callLogs);
        if (userId != null)
        {
            filtered.removeIf(l -> !userId.equals(l.getUserId()));
        }
        if (callType != null)
        {
            filtered.removeIf(l -> !callType.equals(l.getCallType()));
        }
        if (status != null)
        {
            filtered.removeIf(l -> !status.equals(l.getStatus()));
        }
        if (filtered.size() > limit)
        {
            return filtered.subList(filtered.size() - limit, filtered.size());
        }
        return filtered;
    }

    /**
     * 获取每日费用汇总 / Get cost summary for a specific date.
     *
     * @param date 日期字符串(YYYY-MM-DD)，null=今天
     * @return 费用汇总Map
     */
    public Map<String, Object> getDailyCostSummary(String date)
    {
        String targetDate = date != null ? date :
                           LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<CallLogRecord> dayLogs = new ArrayList<>();
        for (CallLogRecord l : callLogs)
        {
            if (l.getCreatedAt() != null
                && l.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE).equals(targetDate))
            {
                dayLogs.add(l);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("date", targetDate);

        if (dayLogs.isEmpty())
        {
            summary.put("total_cost_usd", 0.0);
            summary.put("total_calls", 0);
            summary.put("cache_hits", 0);
            summary.put("cache_hit_rate", 0.0);
            summary.put("total_input_tokens", 0);
            summary.put("total_output_tokens", 0);
            summary.put("avg_latency_ms", 0.0);
            summary.put("by_call_type", new HashMap<>());
            return summary;
        }

        double totalCost = dayLogs.stream().mapToDouble(CallLogRecord::getCostUsd).sum();
        int totalCalls = dayLogs.size();
        long cacheHits = dayLogs.stream().filter(CallLogRecord::isCacheHit).count();
        int totalInput = dayLogs.stream().mapToInt(CallLogRecord::getInputTokens).sum();
        int totalOutput = dayLogs.stream().mapToInt(CallLogRecord::getOutputTokens).sum();
        List<Long> latencies = dayLogs.stream()
            .mapToLong(CallLogRecord::getLatencyMs)
            .filter(l -> l > 0)
            .boxed()
            .collect(java.util.stream.Collectors.toList());
        double avgLatency = latencies.isEmpty() ? 0.0 :
                           latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // By call type
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();
        for (CallLogRecord l : dayLogs)
        {
            String key = l.getCallType();
            if (!byType.containsKey(key))
            {
                Map<String, Object> typeInfo = new LinkedHashMap<>();
                typeInfo.put("cost", 0.0);
                typeInfo.put("calls", 0);
                byType.put(key, typeInfo);
            }
            Map<String, Object> typeInfo = byType.get(key);
            typeInfo.put("cost", (Double) typeInfo.get("cost") + l.getCostUsd());
            typeInfo.put("calls", (Integer) typeInfo.get("calls") + 1);
        }

        summary.put("total_cost_usd", totalCost);
        summary.put("total_calls", totalCalls);
        summary.put("cache_hits", cacheHits);
        summary.put("cache_hit_rate", totalCalls > 0 ? (double) cacheHits / totalCalls : 0.0);
        summary.put("total_input_tokens", totalInput);
        summary.put("total_output_tokens", totalOutput);
        summary.put("avg_latency_ms", avgLatency);
        summary.put("by_call_type", byType);

        return summary;
    }

    /**
     * 获取费用告警 / Get recent cost alerts.
     *
     * @param limit 最大返回数量
     * @return 告警列表
     */
    public List<CostAlertRecord> getCostAlerts(int limit)
    {
        if (costAlerts.size() <= limit)
        {
            return new ArrayList<>(costAlerts);
        }
        return new ArrayList<>(costAlerts.subList(costAlerts.size() - limit, costAlerts.size()));
    }

    /**
     * 获取缓存统计 / Get cache statistics.
     *
     * @return 缓存统计Map
     */
    public Map<String, Object> getCacheStats()
    {
        long cacheHits = callLogs.stream().filter(CallLogRecord::isCacheHit).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_entries", cache.size());
        stats.put("total_logs", callLogs.size());
        stats.put("cache_hits", cacheHits);
        stats.put("hit_rate", callLogs.isEmpty() ? 0.0 : (double) cacheHits / callLogs.size());
        return stats;
    }

    // ========== Configuration ==========

    /**
     * 设置外部API客户端 / Set the external API client.
     */
    public void setApiClient(ApiClient client)
    {
        this.apiClient = client;
    }

    /**
     * 注册费用告警回调 / Register a callback for cost alerts.
     */
    public void registerAlertCallback(Consumer<CostAlertRecord> callback)
    {
        alertCallbacks.add(callback);
    }

    /**
     * 更新引擎配置 / Update engine configuration.
     */
    public void configure(String model, double temperature, int defaultMaxTokens,
                          double dailyCostLimitUsd, double warningThreshold,
                          int cacheTtlMinutes, double inputPricePerMtok,
                          double outputPricePerMtok)
    {
        if (model != null) this.model = model;
        this.temperature = temperature;
        this.defaultMaxTokens = defaultMaxTokens;
        this.dailyCostLimitUsd = dailyCostLimitUsd;
        this.warningThreshold = warningThreshold;
        this.cacheTtlMinutes = cacheTtlMinutes;
        this.inputPricePerMtok = inputPricePerMtok;
        this.outputPricePerMtok = outputPricePerMtok;
    }

    // ========== Reset ==========

    /**
     * 重置所有内存存储(测试用) / Reset all in-memory storage (for testing).
     */
    public void resetAll()
    {
        callLogs.clear();
        cache.clear();
        costAlerts.clear();
        alertCallbacks.clear();
        apiClient = null;
    }

    // ========== Inner Classes ==========

    /**
     * API调用日志记录 / Complete record of a Claude API call.
     *
     * <p>Mirrors Board v1.1 Section 3.3 ai_call_logs table.
     */
    public static class CallLogRecord
    {
        private String id;
        private LocalDateTime createdAt;
        private String userId;
        private String callType;
        private String systemPrompt;
        private String userPrompt;
        private String responseText = "";
        private int inputTokens;
        private int outputTokens;
        private double costUsd;
        private long latencyMs;
        private String status = STATUS_SUCCESS;
        private String errorMessage;
        private String systemPromptVersion = "v1";
        private boolean cacheHit;

        public int getTotalTokens()
        {
            return inputTokens + outputTokens;
        }

        // --- Getters & Setters ---

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getCallType() { return callType; }
        public void setCallType(String callType) { this.callType = callType; }

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }

        public String getResponseText() { return responseText; }
        public void setResponseText(String responseText) { this.responseText = responseText; }

        public int getInputTokens() { return inputTokens; }
        public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

        public int getOutputTokens() { return outputTokens; }
        public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }

        public double getCostUsd() { return costUsd; }
        public void setCostUsd(double costUsd) { this.costUsd = costUsd; }

        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getSystemPromptVersion() { return systemPromptVersion; }
        public void setSystemPromptVersion(String v) { this.systemPromptVersion = v; }

        public boolean isCacheHit() { return cacheHit; }
        public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    }

    /**
     * 费用告警记录 / A cost alert event.
     */
    public static class CostAlertRecord
    {
        public final String id;
        public final LocalDateTime timestamp;
        public final String level;
        public final double dailyCostUsd;
        public final double dailyLimitUsd;
        public final String message;

        public CostAlertRecord(String level, double dailyCostUsd, double dailyLimitUsd, String message)
        {
            this.id = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.dailyCostUsd = dailyCostUsd;
            this.dailyLimitUsd = dailyLimitUsd;
            this.message = message;
        }
    }

    /**
     * 缓存条目 / Internal cache entry.
     */
    private static class CacheEntry
    {
        final String response;
        final long timestamp;

        CacheEntry(String response, long timestamp)
        {
            this.response = response;
            this.timestamp = timestamp;
        }
    }
}
