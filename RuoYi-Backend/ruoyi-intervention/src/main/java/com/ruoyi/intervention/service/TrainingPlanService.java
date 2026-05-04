package com.ruoyi.intervention.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ruoyi.intervention.domain.enums.UserStage;
import com.ruoyi.intervention.domain.model.Prescription;
import com.ruoyi.intervention.domain.model.TrainingPlan;

/**
 * 训练方案生成服务 — 基于Claude API的个性化训练方案生成
 * Training plan generation service using Claude API with structured output.
 *
 * <p>Per Board v1.1 Section 2.3 / 3.3:
 * <ul>
 *   <li>User profile input template for plan generation</li>
 *   <li>System Prompt version management (supports A/B testing)</li>
 *   <li>Structured output: 3 recommendations with reasons</li>
 *   <li>Integration with user_stage for stage-appropriate prompting</li>
 * </ul>
 *
 * <p>Prompt template rules / 提示模板规则:
 * <ul>
 *   <li>Chinese output</li>
 *   <li>3 specific recommendations with reasons</li>
 *   <li>Tone: like a personal trainer friend, no platitudes</li>
 *   <li>Structured for parsing and display</li>
 * </ul>
 *
 * Migrated from: intervention-engine/src/algorithms/training_plan.py
 * Ticket: XIN-83
 */
@Service
public class TrainingPlanService
{
    private static final Logger log = LoggerFactory.getLogger(TrainingPlanService.class);

    // ========== Plan Status Constants ==========

    public static final String STATUS_GENERATED = "generated";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_PARTIALLY_COMPLETED = "partially_completed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_EXPIRED = "expired";

    // ========== Default System Prompts ==========

    /** 默认系统提示模板 / Default system prompt templates (version → content) */
    private static final Map<String, SystemPromptTemplate> DEFAULT_SYSTEM_PROMPTS = new LinkedHashMap<>();
    static
    {
        DEFAULT_SYSTEM_PROMPTS.put("v1", new SystemPromptTemplate(
            "v1",
            "你是一位专业的私人健身教练，擅长根据用户的训练数据和身体状况给出个性化训练建议。"
            + "你需要给出3条具体的本周训练建议，每条建议包含具体的训练内容和理由。"
            + "语气要像朋友一样亲切自然，避免使用套话和泛泛的建议。"
            + "所有建议必须基于用户的具体数据，不能编造信息。"
            + "输出格式：每条建议用数字标号（1. 2. 3.），先写建议内容，然后写'理由：'和具体原因。",
            "Baseline system prompt for A/B testing control group",
            true
        ));
        DEFAULT_SYSTEM_PROMPTS.put("v2_encouraging", new SystemPromptTemplate(
            "v2_encouraging",
            "你是一位热情鼓励型的私人健身教练。你的风格是：\n"
            + "1. 总是先肯定用户的努力和进步\n"
            + "2. 用积极的语言描述训练计划\n"
            + "3. 在建议中融入成就感和目标感\n"
            + "给出3条具体的本周训练建议，每条包含训练内容和理由。"
            + "语气要像朋友一样亲切，用鼓励的方式推动用户。"
            + "输出格式：每条建议用数字标号（1. 2. 3.），先写建议内容，然后写'理由：'和具体原因。",
            "Encouraging tone variant — test if positive framing increases compliance",
            true
        ));
        DEFAULT_SYSTEM_PROMPTS.put("v3_data_driven", new SystemPromptTemplate(
            "v3_data_driven",
            "你是一位数据驱动的运动科学教练。你的风格是：\n"
            + "1. 用具体数字量化建议（时长、频率、心率区间等）\n"
            + "2. 引用运动科学原理（如渐进超负荷、FITT原则）\n"
            + "3. 用数据对比展示进步空间\n"
            + "给出3条具体的本周训练建议，每条包含训练内容和科学依据。"
            + "语气专业但易懂，像运动科学专业的朋友在聊天。"
            + "输出格式：每条建议用数字标号（1. 2. 3.），先写建议内容，然后写'理由：'和具体原因。",
            "Data-driven variant — test if scientific framing increases advanced user engagement",
            true
        ));
    }

    /** 阶段→提示版本映射 / Stage-specific prompt version mapping */
    private static final Map<UserStage, String> STAGE_PROMPT_MAP = new LinkedHashMap<>();
    static
    {
        STAGE_PROMPT_MAP.put(UserStage.BEGINNER, "v1");
        STAGE_PROMPT_MAP.put(UserStage.GROWTH, "v2_encouraging");
        STAGE_PROMPT_MAP.put(UserStage.PLATEAU, "v2_encouraging");
        STAGE_PROMPT_MAP.put(UserStage.ADVANCED, "v3_data_driven");
    }

    // ========== Instance State (in-memory, Phase 1) ==========

    /** 系统提示模板注册表 / Registered system prompt templates */
    private final Map<String, SystemPromptTemplate> systemPrompts = new LinkedHashMap<>(DEFAULT_SYSTEM_PROMPTS);

    /** 训练方案存储 / plan_id → TrainingPlanRecord */
    private final Map<String, TrainingPlanRecord> trainingPlans = new HashMap<>();

    /** 用户最新方案 / user_id → plan_id */
    private final Map<String, String> userLatestPlan = new HashMap<>();

    /** Claude引擎服务引用(可选，用于实际API调用) */
    private ClaudeEngineService claudeEngine;

    // ========== Constructors ==========

    public TrainingPlanService()
    {
    }

    /**
     * 注入Claude引擎服务 / Inject Claude engine for actual API calls.
     *
     * @param engine Claude引擎服务 / Claude engine service
     */
    public void setClaudeEngine(ClaudeEngineService engine)
    {
        this.claudeEngine = engine;
    }

    // ========== System Prompt Management ==========

    /**
     * 注册系统提示模板 / Register a system prompt template.
     *
     * @param template 提示模板
     */
    public void registerSystemPrompt(SystemPromptTemplate template)
    {
        systemPrompts.put(template.version, template);
        log.debug("registerSystemPrompt: version={}", template.version);
    }

    /**
     * 获取系统提示模板 / Get a system prompt template by version.
     *
     * @param version 版本号
     * @return 模板，不存在则返回null
     */
    public SystemPromptTemplate getSystemPrompt(String version)
    {
        return systemPrompts.get(version);
    }

    /**
     * 列出所有系统提示模板 / List all registered system prompt templates.
     *
     * @param activeOnly 是否只返回活跃的
     * @return 模板列表
     */
    public List<SystemPromptTemplate> listSystemPrompts(boolean activeOnly)
    {
        List<SystemPromptTemplate> result = new ArrayList<>(systemPrompts.values());
        if (activeOnly)
        {
            result.removeIf(t -> !t.isActive);
        }
        return result;
    }

    /**
     * 选择提示版本 / Select the System Prompt version to use.
     *
     * <p>Priority:
     * <ol>
     *   <li>Explicit A/B test variant override</li>
     *   <li>Stage-specific default mapping</li>
     * </ol>
     *
     * @param stage      用户阶段
     * @param abVariant  A/B测试变体(可选)
     * @return 版本字符串
     */
    public String selectPromptVersion(UserStage stage, String abVariant)
    {
        if (abVariant != null && systemPrompts.containsKey(abVariant))
        {
            return abVariant;
        }
        return STAGE_PROMPT_MAP.getOrDefault(stage, "v1");
    }

    // ========== Plan Generation ==========

    /**
     * 生成个性化训练方案 / Generate a personalized training plan.
     *
     * <p>Uses Claude API (or mock) to generate 3 training recommendations
     * based on the user's profile, device data, and training history.
     *
     * @param profile       用户档案 / User profile data
     * @param promptVersion 指定提示版本(覆盖阶段映射)
     * @param abVariant     A/B测试变体(覆盖promptVersion)
     * @return 生成的训练方案 / Generated TrainingPlanRecord
     */
    public TrainingPlanRecord generateTrainingPlan(UserProfileInput profile, String promptVersion, String abVariant)
    {
        // Determine stage
        UserStage stage = profile.getStage() != null ? profile.getStage() : UserStage.BEGINNER;

        // Select prompt version
        String version;
        if (promptVersion != null)
        {
            version = promptVersion;
        }
        else
        {
            version = selectPromptVersion(stage, abVariant);
        }

        SystemPromptTemplate template = systemPrompts.get(version);
        if (template == null)
        {
            template = systemPrompts.get("v1");
            version = "v1";
        }

        // Format user prompt
        String userPrompt = profile.formatPromptData();
        String systemPrompt = template.content;

        log.info("generateTrainingPlan: userId={}, stage={}, promptVersion={}", profile.getUserId(), stage, version);

        // Call Claude engine (or mock)
        String responseText;
        String callLogId = null;
        if (claudeEngine != null)
        {
            // Use real Claude engine
            Map<String, Object> result = claudeEngine.call(systemPrompt, userPrompt, version, profile.getUserId());
            responseText = (String) result.get("response");
            callLogId = (String) result.get("call_log_id");
        }
        else
        {
            // Mock mode (no Claude engine configured)
            responseText = mockCall(systemPrompt, userPrompt);
        }

        // Parse recommendations
        List<TrainingRecommendation> recommendations = parseRecommendations(responseText);

        // Build plan record
        String planId = UUID.randomUUID().toString();
        TrainingPlanRecord plan = new TrainingPlanRecord();
        plan.setPlanId(planId);
        plan.setUserId(profile.getUserId());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setStage(stage);
        plan.setSystemPromptVersion(version);
        plan.setRecommendations(recommendations);
        plan.setRawResponse(responseText);
        plan.setStatus(STATUS_GENERATED);
        plan.setCallLogId(callLogId);

        // Store
        trainingPlans.put(planId, plan);
        userLatestPlan.put(profile.getUserId(), planId);

        log.info("generateTrainingPlan: planId={}, recommendations={}", planId, recommendations.size());
        return plan;
    }

    /**
     * 生成训练方案(简化版) / Simplified generation with just profile.
     */
    public TrainingPlanRecord generateTrainingPlan(UserProfileInput profile)
    {
        return generateTrainingPlan(profile, null, null);
    }

    // ========== Plan Query ==========

    /**
     * 获取训练方案 / Get a training plan by ID.
     *
     * @param planId 方案ID
     * @return 方案记录，不存在则返回null
     */
    public TrainingPlanRecord getPlan(String planId)
    {
        return trainingPlans.get(planId);
    }

    /**
     * 获取用户最新方案 / Get the latest training plan for a user.
     *
     * @param userId 用户ID
     * @return 最新方案，不存在则返回null
     */
    public TrainingPlanRecord getLatestPlan(String userId)
    {
        String planId = userLatestPlan.get(userId);
        if (planId != null)
        {
            return trainingPlans.get(planId);
        }
        return null;
    }

    /**
     * 获取用户所有方案 / Get all training plans for a user.
     *
     * @param userId 用户ID
     * @param limit  最大返回数量
     * @return 方案列表(按创建时间倒序)
     */
    public List<TrainingPlanRecord> getUserPlans(String userId, int limit)
    {
        List<TrainingPlanRecord> plans = new ArrayList<>();
        for (TrainingPlanRecord p : trainingPlans.values())
        {
            if (userId.equals(p.getUserId()))
            {
                plans.add(p);
            }
        }
        plans.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        if (plans.size() > limit)
        {
            return plans.subList(0, limit);
        }
        return plans;
    }

    /**
     * 更新方案状态 / Update the status of a training plan.
     *
     * @param planId 方案ID
     * @param status 新状态
     * @return 更新后的方案，不存在则返回null
     */
    public TrainingPlanRecord updatePlanStatus(String planId, String status)
    {
        TrainingPlanRecord plan = trainingPlans.get(planId);
        if (plan != null)
        {
            plan.setStatus(status);
            log.debug("updatePlanStatus: planId={}, status={}", planId, status);
        }
        return plan;
    }

    // ========== Response Parsing ==========

    /**
     * 解析Claude API响应为结构化建议 / Parse Claude API response into structured recommendations.
     *
     * <p>Expected format:
     * <pre>
     *   1. 建议内容... 理由：原因...
     *   2. 建议内容... 理由：原因...
     *   3. 建议内容... 理由：原因...
     * </pre>
     *
     * @param text Claude API原始响应 / Raw API response text
     * @return 解析后的建议列表 / Parsed recommendations
     */
    static List<TrainingRecommendation> parseRecommendations(String text)
    {
        List<TrainingRecommendation> recommendations = new ArrayList<>();
        if (text == null || text.trim().isEmpty())
        {
            return recommendations;
        }

        String[] lines = text.split("\\n");
        int currentIdx = 0;
        StringBuilder currentContent = new StringBuilder();
        StringBuilder currentReason = new StringBuilder();

        Pattern numPattern = Pattern.compile("^([1-9])[.、)）]\\s*(.*)");

        for (String line : lines)
        {
            String stripped = line.trim();
            if (stripped.isEmpty())
            {
                continue;
            }

            Matcher m = numPattern.matcher(stripped);
            if (m.find())
            {
                // Save previous recommendation
                if (currentIdx > 0)
                {
                    recommendations.add(new TrainingRecommendation(
                        currentIdx,
                        currentContent.toString().trim(),
                        currentReason.toString().trim()
                    ));
                }

                currentIdx = Integer.parseInt(m.group(1));
                String remaining = m.group(2);

                // Split on 理由
                int reasonIdx = remaining.indexOf("理由");
                if (reasonIdx >= 0)
                {
                    String contentPart = remaining.substring(0, reasonIdx).trim();
                    contentPart = contentPart.replaceAll("[：:]$", "");
                    currentContent = new StringBuilder(contentPart);

                    String reasonPart = remaining.substring(reasonIdx + 2).trim();
                    reasonPart = reasonPart.replaceFirst("^[：:]", "").trim();
                    currentReason = new StringBuilder(reasonPart);
                }
                else
                {
                    currentContent = new StringBuilder(remaining);
                    currentReason = new StringBuilder();
                }
            }
            else if (currentIdx > 0)
            {
                // Continuation of current recommendation
                int reasonIdx = stripped.indexOf("理由");
                if (reasonIdx >= 0)
                {
                    String contentPart = stripped.substring(0, reasonIdx).trim();
                    if (!contentPart.isEmpty())
                    {
                        currentContent.append(" ").append(contentPart.replaceAll("[：:]$", ""));
                    }
                    String reasonPart = stripped.substring(reasonIdx + 2).trim();
                    reasonPart = reasonPart.replaceFirst("^[：:]", "").trim();
                    currentReason.append(" ").append(reasonPart);
                }
                else if (currentReason.length() > 0)
                {
                    currentReason.append(" ").append(stripped);
                }
                else
                {
                    currentContent.append(" ").append(stripped);
                }
            }
        }

        // Save last
        if (currentIdx > 0)
        {
            recommendations.add(new TrainingRecommendation(
                currentIdx,
                currentContent.toString().trim(),
                currentReason.toString().trim()
            ));
        }

        return recommendations;
    }

    // ========== Mock ==========

    /**
     * 模拟API调用 / Mock API call for testing/development.
     */
    private static String mockCall(String systemPrompt, String userPrompt)
    {
        return "1. 建议本周进行3次中等强度有氧训练，每次30分钟。理由：您目前处于成长期，"
             + "心肺功能稳步提升，适当增加训练频率可以持续改善。\n"
             + "2. 尝试加入1次力量训练，重点关注上肢肌群。理由：您主要使用跑步机，"
             + "上肢锻炼不足，平衡训练有助于全面发展。\n"
             + "3. 训练后增加5分钟拉伸放松。理由：近两周恢复指标偏低，"
             + "拉伸有助于减少肌肉酸痛，提高下次训练质量。";
    }

    // ========== Session Formatting ==========

    /**
     * 格式化训练会话列表 / Format a list of training sessions for display.
     *
     * @param sessions 训练会话列表 (nullable)
     * @return 格式化字符串
     */
    public String formatSessionList(List<TrainingPlan.TrainingSession> sessions)
    {
        if (sessions == null || sessions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (TrainingPlan.TrainingSession s : sessions)
        {
            sb.append(String.format("Day %d: %s%n", s.getDay(), s.getStatus()));
        }
        return sb.toString().trim();
    }

    /**
     * 获取系统提示 (3参数重载) / Get system prompt for prescription context.
     * Generates a contextual prompt based on prescription, age, and goal.
     *
     * @param rx 运动处方
     * @param age 用户年龄
     * @param goal 运动目标
     * @return 系统提示字符串
     */
    public String getSystemPrompt(Prescription rx, int age, String goal)
    {
        String effectiveGoal = (goal != null && !goal.isEmpty()) ? goal : "GENERAL_FITNESS";
        String version = selectPromptVersion(UserStage.BEGINNER, null);
        SystemPromptTemplate tmpl = systemPrompts.get(version);
        String base = (tmpl != null) ? tmpl.content : "你是一个专业的健身教练。";

        return base + String.format(
            "%n用户年龄: %d, 目标: %s, 干预类型: %s",
            age, effectiveGoal,
            rx != null ? rx.getInterventionType() : "EXERCISE");
    }

    // ========== Reset ==========

    /**
     * 重置所有内存存储(测试用) / Reset all in-memory storage (for testing).
     */
    public void resetAll()
    {
        systemPrompts.clear();
        systemPrompts.putAll(DEFAULT_SYSTEM_PROMPTS);
        trainingPlans.clear();
        userLatestPlan.clear();
    }

    // ========== Inner Classes ==========

    /**
     * 系统提示模板 / A versioned System Prompt template.
     */
    public static class SystemPromptTemplate
    {
        /** 版本标识 / Version identifier (e.g. "v1", "v2_encouraging") */
        public final String version;
        /** 提示内容 / System prompt content */
        public final String content;
        /** 描述说明 / What this prompt variant tests */
        public final String description;
        /** 是否活跃 / Whether this version is available for use */
        public final boolean isActive;

        public SystemPromptTemplate(String version, String content, String description, boolean isActive)
        {
            this.version = version;
            this.content = content;
            this.description = description;
            this.isActive = isActive;
        }
    }

    /**
     * 训练建议 / A single training recommendation.
     */
    public static class TrainingRecommendation
    {
        private final int index;
        private final String content;
        private final String reason;

        public TrainingRecommendation(int index, String content, String reason)
        {
            this.index = index;
            this.content = content;
            this.reason = reason;
        }

        public int getIndex() { return index; }
        public String getContent() { return content; }
        public String getReason() { return reason; }

        @Override
        public String toString()
        {
            return "TrainingRecommendation{index=" + index + ", content='" + content + "', reason='" + reason + "'}";
        }
    }

    /**
     * 训练方案记录 / A generated training plan record.
     */
    public static class TrainingPlanRecord
    {
        private String planId;
        private String userId;
        private LocalDateTime createdAt;
        private UserStage stage;
        private String systemPromptVersion;
        private List<TrainingRecommendation> recommendations = new ArrayList<>();
        private String rawResponse;
        private String status;
        private String callLogId;

        // --- Getters & Setters ---

        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public UserStage getStage() { return stage; }
        public void setStage(UserStage stage) { this.stage = stage; }

        public String getSystemPromptVersion() { return systemPromptVersion; }
        public void setSystemPromptVersion(String v) { this.systemPromptVersion = v; }

        public List<TrainingRecommendation> getRecommendations() { return recommendations; }
        public void setRecommendations(List<TrainingRecommendation> r) { this.recommendations = r; }

        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCallLogId() { return callLogId; }
        public void setCallLogId(String callLogId) { this.callLogId = callLogId; }
    }

    /**
     * 用户档案输入 / User profile data for plan generation (Board v1.1 Section 2.3 template).
     */
    public static class UserProfileInput
    {
        private String userId;
        private String name = "";
        private int age = 30;
        private String gender = "未指定";
        private String city = "";
        private String deviceType = "跑步机";
        private String deviceModel = "";
        private String goal = "增强体质";
        private int sessionsLast30Days = 0;
        private double avgDurationMinutes = 0.0;
        private int sessionsLastWeek = 0;
        private int sessionsPreviousWeek = 0;
        private String lastSessionDate;
        private double lastCompletionRate = 0.0;
        private UserStage stage;

        /**
         * 格式化为提示数据 / Format user data into the Board v1.1 prompt template.
         *
         * @return 格式化后的用户数据字符串
         */
        public String formatPromptData()
        {
            UserStage effectiveStage = stage != null ? stage : UserStage.BEGINNER;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("用户：%s，%d岁，%s，%s\n", name, age, gender, city));
            sb.append(String.format("设备：%s，型号：%s\n", deviceType, deviceModel));
            sb.append(String.format("目标：%s\n", goal));
            sb.append(String.format("过去30天：训练%d次，平均%.0f分钟/次\n", sessionsLast30Days, avgDurationMinutes));
            sb.append(String.format("上周训练：%d次（vs 上上周%d次）\n", sessionsLastWeek, sessionsPreviousWeek));

            if (lastSessionDate != null && !lastSessionDate.isEmpty())
            {
                sb.append(String.format("最近一次：%s，完成率%.0f%%\n", lastSessionDate, lastCompletionRate * 100));
            }
            else
            {
                sb.append("最近一次：无记录\n");
            }

            sb.append(String.format("当前阶段：%s\n", effectiveStage.getLabelZh()));
            sb.append("\n");
            sb.append("请用中文给出本周训练建议（3条，每条含具体理由，语气像私教朋友，不要套话）。");

            return sb.toString();
        }

        // --- Getters & Setters ---

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getDeviceModel() { return deviceModel; }
        public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

        public String getGoal() { return goal; }
        public void setGoal(String goal) { this.goal = goal; }

        public int getSessionsLast30Days() { return sessionsLast30Days; }
        public void setSessionsLast30Days(int v) { this.sessionsLast30Days = v; }

        public double getAvgDurationMinutes() { return avgDurationMinutes; }
        public void setAvgDurationMinutes(double v) { this.avgDurationMinutes = v; }

        public int getSessionsLastWeek() { return sessionsLastWeek; }
        public void setSessionsLastWeek(int v) { this.sessionsLastWeek = v; }

        public int getSessionsPreviousWeek() { return sessionsPreviousWeek; }
        public void setSessionsPreviousWeek(int v) { this.sessionsPreviousWeek = v; }

        public String getLastSessionDate() { return lastSessionDate; }
        public void setLastSessionDate(String v) { this.lastSessionDate = v; }

        public double getLastCompletionRate() { return lastCompletionRate; }
        public void setLastCompletionRate(double v) { this.lastCompletionRate = v; }

        public UserStage getStage() { return stage; }
        public void setStage(UserStage stage) { this.stage = stage; }
    }
}
