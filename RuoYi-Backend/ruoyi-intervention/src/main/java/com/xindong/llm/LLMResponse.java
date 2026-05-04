package com.xindong.llm;

/**
 * LLM调用统一响应 / Unified response from any LLM provider.
 *
 * <p>Every provider returns this standard structure regardless of the underlying API.
 * Contains the content, usage metadata, cost, and latency information.
 *
 * <p>Ticket: XIN-93
 */
public class LLMResponse
{
    /** 生成内容 / Generated text content */
    private String content;

    /** 使用的模型标识 / Model identifier used for this call */
    private String model;

    /** 提供者名称 / Provider name (claude, openai, zai, mock) */
    private String provider;

    /** 输入token数 / Number of input tokens consumed */
    private int tokensIn;

    /** 输出token数 / Number of output tokens generated */
    private int tokensOut;

    /** 调用延迟(毫秒) / Call latency in milliseconds */
    private long latencyMs;

    /** 调用成本(美分) / Cost in US cents for this call */
    private double costCents;

    /** 调用是否成功 / Whether the call succeeded */
    private boolean success;

    /** 错误信息(失败时) / Error message if call failed */
    private String errorMessage;

    /** 是否来自缓存 / Whether this response was served from cache */
    private boolean cached;

    // ========== Constructors ==========

    public LLMResponse() {}

    public LLMResponse(String content, String model, String provider,
                       int tokensIn, int tokensOut, long latencyMs,
                       double costCents, boolean success)
    {
        this.content = content;
        this.model = model;
        this.provider = provider;
        this.tokensIn = tokensIn;
        this.tokensOut = tokensOut;
        this.latencyMs = latencyMs;
        this.costCents = costCents;
        this.success = success;
    }

    /** 创建成功响应 / Create a successful response */
    public static LLMResponse ok(String content, String model, String provider,
                                  int tokensIn, int tokensOut, long latencyMs,
                                  double costCents)
    {
        LLMResponse r = new LLMResponse(content, model, provider,
                                         tokensIn, tokensOut, latencyMs,
                                         costCents, true);
        return r;
    }

    /** 创建失败响应 / Create an error response */
    public static LLMResponse error(String provider, String model, String errorMessage,
                                     long latencyMs)
    {
        LLMResponse r = new LLMResponse();
        r.provider = provider;
        r.model = model;
        r.success = false;
        r.errorMessage = errorMessage;
        r.latencyMs = latencyMs;
        return r;
    }

    /** 创建缓存响应 / Create a cached response */
    public static LLMResponse cached(String content, String model, String provider,
                                      int tokensIn, int tokensOut, double costCents)
    {
        LLMResponse r = ok(content, model, provider, tokensIn, tokensOut, 0, costCents);
        r.cached = true;
        return r;
    }

    // ========== Utility ==========

    public int getTotalTokens()
    {
        return tokensIn + tokensOut;
    }

    @Override
    public String toString()
    {
        return String.format("LLMResponse{provider=%s, model=%s, success=%s, "
                             + "tokens=%d+%d, latency=%dms, cost=%.2f cents, cached=%s}",
                             provider, model, success, tokensIn, tokensOut,
                             latencyMs, costCents, cached);
    }

    // ========== Getters & Setters ==========

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getTokensIn() { return tokensIn; }
    public void setTokensIn(int tokensIn) { this.tokensIn = tokensIn; }

    public int getTokensOut() { return tokensOut; }
    public void setTokensOut(int tokensOut) { this.tokensOut = tokensOut; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public double getCostCents() { return costCents; }
    public void setCostCents(double costCents) { this.costCents = costCents; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
}
