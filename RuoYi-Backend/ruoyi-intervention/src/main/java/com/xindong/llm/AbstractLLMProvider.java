package com.xindong.llm;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM提供者抽象基类 / Abstract base class for LLM providers.
 *
 * <p>Provides shared functionality:
 * <ul>
 *   <li>Cumulative cost tracking (thread-safe)</li>
 *   <li>Call counting</li>
 *   <li>Health state management with auto-unhealthy on consecutive failures</li>
 * </ul>
 *
 * <p>Subclasses only need to implement {@link #doCall(String, String, int)}.
 *
 * <p>Ticket: XIN-93
 */
public abstract class AbstractLLMProvider implements LLMProvider
{
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** 提供者配置 / Provider configuration */
    private final ProviderConfig config;

    /** 健康状态 / Health state */
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    /** 累计成本(美分) / Cumulative cost in US cents */
    private final AtomicReference<Double> cumulativeCostCents = new AtomicReference<>(0.0);

    /** 调用次数 / Call count */
    private final AtomicLong callCount = new AtomicLong(0);

    /** 连续失败次数 / Consecutive failure count */
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    /** 最大连续失败后标记不健康 / Max consecutive failures before marking unhealthy */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    protected AbstractLLMProvider(ProviderConfig config)
    {
        this.config = config;
    }

    /**
     * 子类实现的调用逻辑 / Subclass call implementation.
     * Must return an LLMResponse (success or error).
     */
    protected abstract LLMResponse doCall(String systemPrompt, String userPrompt, int maxTokens);

    @Override
    public LLMResponse call(String systemPrompt, String userPrompt, int maxTokens)
    {
        callCount.incrementAndGet();

        try
        {
            LLMResponse response = doCall(systemPrompt, userPrompt, maxTokens);

            // Override provider name and model from config if not set
            if (response.getProvider() == null)
            {
                response.setProvider(name());
            }
            if (response.getModel() == null)
            {
                response.setModel(getModel());
            }

            // Track cost (thread-safe accumulate)
            cumulativeCostCents.accumulateAndGet(response.getCostCents(),
                (current, delta) -> current + delta);

            // Health tracking
            if (response.isSuccess())
            {
                consecutiveFailures.set(0);
                healthy.set(true);
            }
            else
            {
                long failures = consecutiveFailures.incrementAndGet();
                if (failures >= MAX_CONSECUTIVE_FAILURES)
                {
                    healthy.set(false);
                    log.warn("{}: marked unhealthy after {} consecutive failures",
                             name(), failures);
                }
            }

            return response;
        }
        catch (Exception e)
        {
            long failures = consecutiveFailures.incrementAndGet();
            if (failures >= MAX_CONSECUTIVE_FAILURES)
            {
                healthy.set(false);
            }
            return LLMResponse.error(name(), getModel(), e.getMessage(), 0);
        }
    }

    @Override
    public boolean isHealthy()
    {
        return healthy.get();
    }

    @Override
    public void healthCheck()
    {
        healthy.set(true);
        consecutiveFailures.set(0);
    }

    @Override
    public double getCumulativeCostCents()
    {
        return cumulativeCostCents.get();
    }

    @Override
    public long getCallCount()
    {
        return callCount.get();
    }

    @Override
    public void resetStats()
    {
        cumulativeCostCents.set(0.0);
        callCount.set(0);
        consecutiveFailures.set(0);
        healthy.set(true);
    }

    public ProviderConfig getConfig()
    {
        return config;
    }
}
