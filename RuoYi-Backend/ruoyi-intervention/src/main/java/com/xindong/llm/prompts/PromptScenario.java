package com.xindong.llm.prompts;

/**
 * 提示词场景枚举 / Prompt scenario types for the callClaude() engine.
 *
 * <p>Each scenario maps to a distinct system prompt template and output schema.
 * The scenario value is used as the callType parameter in ClaudeEngineService.call().
 *
 * <p>Ticket: XIN-105
 */
public enum PromptScenario
{
    /** 初始评估 / Initial user assessment and training plan creation */
    INITIAL_ASSESSMENT("initial_assessment", "初始评估"),

    /** 每日任务生成 / Daily exercise task generation */
    DAILY_TASK("daily_task", "每日任务生成"),

    /** 进展回顾 / Periodic progress review and analysis */
    PROGRESS_REVIEW("progress_review", "进展回顾"),

    /** 计划调整 / Training plan adjustment based on compliance/feedback */
    PLAN_ADJUSTMENT("plan_adjustment", "计划调整");

    private final String code;
    private final String labelZh;

    PromptScenario(String code, String labelZh)
    {
        this.code = code;
        this.labelZh = labelZh;
    }

    public String getCode() { return code; }
    public String getLabelZh() { return labelZh; }

    /**
     * Parse from string code (case-insensitive).
     */
    public static PromptScenario fromCode(String code)
    {
        if (code == null) return INITIAL_ASSESSMENT;
        for (PromptScenario s : values())
        {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        return INITIAL_ASSESSMENT;
    }
}
