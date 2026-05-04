package com.xindong.llm;

/**
 * 提供者配置 / Configuration for a single LLM provider.
 *
 * <p>Loaded from llm-providers.yaml or constructed programmatically.
 * Each provider entry maps to one set of credentials and parameters.
 *
 * <p>Ticket: XIN-93
 */
public class ProviderConfig
{
    /** 提供者标识: claude, openai, zai, mock */
    private String name;

    /** API基础URL / API base URL (overrides default endpoint) */
    private String baseUrl;

    /** API密钥 / API key (read from env var or config) */
    private String apiKey;

    /** 默认模型 / Default model identifier */
    private String model;

    /** 采样温度 / Sampling temperature */
    private double temperature = 0.0;

    /** 默认最大输出token / Default max output tokens */
    private int maxTokens = 1024;

    /** 超时(秒) / Request timeout in seconds */
    private int timeoutSeconds = 30;

    /** 是否启用 / Whether this provider is enabled */
    private boolean enabled = true;

    /** 优先级(越小越高) / Priority in fallback chain (lower = higher priority) */
    private int priority = 100;

    /** 每百万输入token价格(美分) / US cents per million input tokens */
    private double inputPricePerMtokCents = 300.0;

    /** 每百万输出token价格(美分) / US cents per million output tokens */
    private double outputPricePerMtokCents = 1500.0;

    /** 每日成本上限(美分) / Daily cost cap in US cents (0 = unlimited) */
    private double dailyCostCapCents = 0;

    public ProviderConfig() {}

    public ProviderConfig(String name, String apiKey, String model)
    {
        this.name = name;
        this.apiKey = apiKey;
        this.model = model;
    }

    // ========== Getters & Setters ==========

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public double getInputPricePerMtokCents() { return inputPricePerMtokCents; }
    public void setInputPricePerMtokCents(double v) { this.inputPricePerMtokCents = v; }

    public double getOutputPricePerMtokCents() { return outputPricePerMtokCents; }
    public void setOutputPricePerMtokCents(double v) { this.outputPricePerMtokCents = v; }

    public double getDailyCostCapCents() { return dailyCostCapCents; }
    public void setDailyCostCapCents(double v) { this.dailyCostCapCents = v; }

    /**
     * 计算单次调用成本(美分) / Calculate cost for a single call in US cents.
     */
    public double calculateCostCents(int tokensIn, int tokensOut)
    {
        double inCost = (tokensIn / 1_000_000.0) * inputPricePerMtokCents;
        double outCost = (tokensOut / 1_000_000.0) * outputPricePerMtokCents;
        return inCost + outCost;
    }

    @Override
    public String toString()
    {
        return String.format("ProviderConfig{name=%s, model=%s, enabled=%s, priority=%d}",
                             name, model, enabled, priority);
    }
}
