package com.xindong.llm;

/**
 * LLM提供者统一接口 / Unified interface for LLM providers.
 *
 * <p>Every provider (Claude, OpenAI, Zai, Mock) implements this interface
 * so that {@link LLMEngine} can route calls uniformly with hot-swap and fallback.
 *
 * <p>Ticket: XIN-93
 */
public interface LLMProvider
{
    /**
     * 提供者名称 / Unique provider identifier.
     * Examples: "claude", "openai", "zai", "mock"
     */
    String name();

    /**
     * 调用LLM / Call the LLM with system + user prompts.
     *
     * @param systemPrompt  系统提示
     * @param userPrompt    用户提示/数据
     * @param maxTokens     最大输出token数
     * @return LLMResponse with content and metadata
     */
    LLMResponse call(String systemPrompt, String userPrompt, int maxTokens);

    /**
     * 该提供者是否健康/可用 / Whether this provider is healthy and available.
     * Used by fallback chain to skip down providers.
     */
    boolean isHealthy();

    /**
     * 健康检查 / Run a health check (e.g., ping the API).
     * Updates the internal health state.
     */
    void healthCheck();

    /**
     * 获取当前使用模型 / Get the currently configured model identifier.
     */
    String getModel();

    /**
     * 获取提供者累计成本(美分) / Get cumulative cost for this provider in US cents.
     */
    double getCumulativeCostCents();

    /**
     * 获取提供者累计调用次数 / Get total call count for this provider.
     */
    long getCallCount();

    /**
     * 重置累计统计 / Reset cumulative statistics.
     */
    void resetStats();
}
