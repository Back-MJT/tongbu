package com.xindong.llm;

/**
 * 模拟提供者(测试/开发) / Mock LLM provider for testing and development.
 *
 * <p>Returns deterministic responses without making any API calls.
 * Useful for unit testing, integration testing, and offline development.
 *
 * <p>Ticket: XIN-93
 */
public class MockProvider extends AbstractLLMProvider
{
    /** 可配置的固定响应 / Configurable fixed response */
    private String fixedResponse;

    /** 是否模拟失败 / Whether to simulate failure */
    private boolean simulateFailure = false;

    /** 模拟延迟(毫秒) / Simulated latency in ms */
    private long simulatedLatencyMs = 0;

    public MockProvider()
    {
        super(new ProviderConfig("mock", "", "mock-model"));
        this.fixedResponse = "1. 建议本周进行3次中等强度有氧训练，每次30分钟。理由：您目前处于成长期，"
            + "心肺功能稳步提升，适当增加训练频率可以持续改善。\n"
            + "2. 尝试加入1次力量训练，重点关注上肢肌群。理由：您主要使用跑步机，"
            + "上肢锻炼不足，平衡训练有助于全面发展。\n"
            + "3. 训练后增加5分钟拉伸放松。理由：近两周恢复指标偏低，"
            + "拉伸有助于减少肌肉酸痛，提高下次训练质量。";
    }

    public MockProvider(ProviderConfig config)
    {
        super(config);
    }

    @Override
    public String name()
    {
        ProviderConfig cfg = getConfig();
        return cfg != null && cfg.getName() != null ? cfg.getName() : "mock";
    }

    @Override
    public String getModel()
    {
        ProviderConfig cfg = getConfig();
        return cfg != null && cfg.getModel() != null && !cfg.getModel().isEmpty()
            ? cfg.getModel() : "mock-model";
    }

    @Override
    protected LLMResponse doCall(String systemPrompt, String userPrompt, int maxTokens)
    {
        // Simulate latency if configured
        if (simulatedLatencyMs > 0)
        {
            try
            {
                Thread.sleep(simulatedLatencyMs);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        if (simulateFailure)
        {
            return LLMResponse.error(name(), getModel(), "Simulated failure", simulatedLatencyMs);
        }

        // Estimate tokens (rough: 1 token ≈ 3 chars for mixed Chinese/English)
        int tokensIn = (systemPrompt.length() + userPrompt.length()) / 3;
        int tokensOut = fixedResponse.length() / 3;

        ProviderConfig cfg = getConfig();
        double costCents = cfg != null ? cfg.calculateCostCents(tokensIn, tokensOut) : 0;

        return LLMResponse.ok(fixedResponse, getModel(), name(),
                              tokensIn, tokensOut, simulatedLatencyMs, costCents);
    }

    // ========== Test Configuration ==========

    /**
     * 设置固定响应 / Set the fixed response text.
     */
    public MockProvider setFixedResponse(String response)
    {
        this.fixedResponse = response;
        return this;
    }

    /**
     * 设置是否模拟失败 / Set whether to simulate failure.
     */
    public MockProvider setSimulateFailure(boolean fail)
    {
        this.simulateFailure = fail;
        return this;
    }

    /**
     * 设置模拟延迟 / Set simulated latency in milliseconds.
     */
    public MockProvider setSimulatedLatencyMs(long ms)
    {
        this.simulatedLatencyMs = ms;
        return this;
    }
}
