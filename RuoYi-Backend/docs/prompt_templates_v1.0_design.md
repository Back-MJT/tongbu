# Prompt Templates v1.0 设计文档 / Prompt Template Design Document

**Ticket:** XIN-105  
**Author:** Algorithm Engineer  
**Date:** 2026-04-15  
**Version:** v1.0

---

## 1. 设计目标 / Design Goals

为 callClaude() 引擎（LLMEngine → ClaudeEngineService）提供结构化的提示词模板系统：

- **4个核心场景**：初始评估、每日任务生成、进展回顾、计划调整
- **输入参数化**：用户阶段、训练历史、IMU数据以结构化JSON注入
- **输出规范**：每个场景有明确的JSON输出schema，便于下游解析
- **版本管理**：每个模板有版本号，支持A/B测试和回滚
- **可测试**：Mock Provider可返回预定义的模板响应

## 2. 场景定义 / Scenario Definitions

### 2.1 INITIAL_ASSESSMENT — 初始评估
- **触发**：新用户首次使用或重新评估
- **输入**：健康画像（HealthProfile）、基础分数（BaselineScores）、风险因素
- **输出**：训练建议（初始阶段判定、推荐运动类型、周频次、强度区间）
- **callType**: `initial_assessment`

### 2.2 DAILY_TASK_GENERATION — 每日任务生成
- **触发**：每日自动或用户主动请求
- **输入**：当前阶段、历史7天训练数据、今日IMU数据摘要
- **输出**：当日运动处方（运动类型×N、组数/次数、心率区间、预估时长）
- **callType**: `daily_task`

### 2.3 PROGRESS_REVIEW — 进展回顾
- **触发**：每周/每两周自动或用户请求
- **输入**：训练历史（4周）、健康分数趋势、依从性数据
- **输出**：进展报告（分数变化、达标率、阶段转换建议、鼓励语）
- **callType**: `progress_review`

### 2.4 PLAN_ADJUSTMENT — 计划调整
- **触发**：依从性低于阈值、停滞期检测、用户反馈
- **输入**：当前计划、失败原因分析、用户偏好
- **输出**：调整后的计划（修改后的运动处方、调整规则、过渡方案）
- **callType**: `plan_adjustment`

## 3. 输入参数Schema / Input Parameter Schemas

### 3.1 通用字段（所有场景共享）
```json
{
  "userId": "string",
  "timestamp": "ISO-8601",
  "scenario": "initial_assessment|daily_task|progress_review|plan_adjustment"
}
```

### 3.2 INITIAL_ASSESSMENT 输入
```json
{
  "healthProfile": {
    "age": 35,
    "gender": "male",
    "healthGoals": ["weight_loss", "cardiovascular"],
    "baselineScores": {
      "cardio": 55,
      "strength": 40,
      "flexibility": 60,
      "bodyComp": 50,
      "overall": 51
    },
    "riskFactors": {
      "hasCardiovascularRisk": false,
      "hasMetabolicRisk": true,
      "hasMusculoskeletalRisk": false,
      "riskLevel": "moderate"
    },
    "preferences": {
      "preferredTime": "morning",
      "availableDaysPerWeek": 4,
      "sessionDurationMinutes": 45,
      "equipmentAccess": ["treadmill", "stationary_bike"]
    }
  }
}
```

### 3.3 DAILY_TASK_GENERATION 输入
```json
{
  "userStage": "growth",
  "currentPlan": {
    "planId": "string",
    "weekNumber": 3,
    "targetExercisesPerWeek": 4
  },
  "recentHistory": [
    {
      "date": "2026-04-14",
      "exerciseType": "running",
      "durationMinutes": 30,
      "avgHeartRate": 135,
      "intensity": "moderate",
      "completed": true
    }
  ],
  "todayImuSummary": {
    "restingHeartRate": 62,
    "sleepQuality": "good",
    "readinessScore": 78,
    "stepsToday": 3200
  },
  "feedbackLastSession": {
    "perceivedExertion": 6,
    "enjoyment": 4,
    "soreness": ["quadriceps"]
  }
}
```

### 3.4 PROGRESS_REVIEW 输入
```json
{
  "userStage": "growth",
  "reviewPeriod": {
    "startDate": "2026-03-18",
    "endDate": "2026-04-14",
    "weeks": 4
  },
  "healthScoreTrend": {
    "start": 51,
    "current": 58,
    "delta": 7,
    "dimensions": {
      "cardio": { "start": 55, "current": 62 },
      "strength": { "start": 40, "current": 48 },
      "flexibility": { "start": 60, "current": 63 },
      "bodyComp": { "start": 50, "current": 55 }
    }
  },
  "compliance": {
    "sessionsPlanned": 16,
    "sessionsCompleted": 13,
    "completionRate": 0.8125,
    "avgPerceivedExertion": 5.8
  },
  "stageTransitionCandidate": false
}
```

### 3.5 PLAN_ADJUSTMENT 输入
```json
{
  "userStage": "plateau",
  "currentPlan": {
    "planId": "string",
    "durationDays": 28,
    "exercisesPerWeek": 3,
    "avgDuration": 30
  },
  "adjustmentTrigger": {
    "type": "low_compliance|plateau_detected|user_feedback",
    "details": "连续7天未训练，依从率降至0.3"
  },
  "failureAnalysis": {
    "missedSessions": 5,
    "commonSkipReason": "evening_fatigue",
    "intensityDrop": true
  },
  "userPreferences": {
    "preferredTime": "morning",
    "preferredTypes": ["walking", "cycling"],
    "maxDuration": 25
  }
}
```

## 4. 输出JSON Schema / Output JSON Schemas

### 4.1 INITIAL_ASSESSMENT 输出
```json
{
  "stageAssessment": {
    "recommendedStage": "beginner",
    "confidence": 0.85,
    "reasoning": "..."
  },
  "trainingRecommendation": {
    "weeklyFrequency": 3,
    "sessionDurationMinutes": 30,
    "intensity": "light",
    "exerciseTypes": ["walking", "cycling"],
    "targetHeartRateZone": { "min": 100, "max": 120 },
    "warmupMinutes": 5,
    "cooldownMinutes": 5
  },
  "precautions": ["..."],
  "firstWeekPlan": {
    "days": [
      {
        "day": 1,
        "exercises": [
          { "type": "walking", "duration": 20, "intensity": "light" }
        ]
      }
    ]
  },
  "evidenceBasis": "ACSM 2021 Ch.7, 初学者推荐低强度起步..."
}
```

### 4.2 DAILY_TASK_GENERATION 输出
```json
{
  "date": "2026-04-15",
  "readinessAssessment": {
    "score": 78,
    "recommendation": "proceed|reduce_intensity|rest"
  },
  "exercises": [
    {
      "order": 1,
      "type": "running",
      "intensity": "moderate",
      "durationMinutes": 25,
      "heartRateZone": { "min": 120, "max": 140 },
      "sets": null,
      "repsPerSet": null,
      "paceGuidance": "5:30-6:00/km",
      "notes": "前5分钟慢跑热身"
    }
  ],
  "totalDurationMinutes": 35,
  "calorieEstimate": 280,
  "motivationalNote": "..."
}
```

### 4.3 PROGRESS_REVIEW 输出
```json
{
  "summary": {
    "overallTrend": "improving|stable|declining",
    "scoreDelta": 7,
    "gradeLetter": "C+"
  },
  "highlights": ["心肺功能提升7分", "力量训练完成率最高"],
  "areasForImprovement": ["柔韧性训练频率不足"],
  "stageTransition": {
    "recommended": false,
    "targetStage": null,
    "reasoning": "..."
  },
  "nextPeriodRecommendations": [
    "保持当前有氧频率",
    "增加1次力量训练"
  ],
  "encouragementMessage": "..."
}
```

### 4.4 PLAN_ADJUSTMENT 输出
```json
{
  "adjustmentType": "reduce_load|increase_variety|reschedule|change_intensity",
  "reasoning": "...",
  "adjustedPlan": {
    "weeklyFrequency": 3,
    "sessionDurationMinutes": 20,
    "intensity": "light",
    "exerciseTypes": ["walking", "yoga"]
  },
  "transitionStrategy": {
    "steps": ["第1-2周: 恢复性训练", "第3-4周: 逐步提升"],
    "targetWeek": 4
  },
  "newAdjustmentRules": [
    { "trigger": "completion_rate < 0.5", "action": "reduce_duration_20pct" }
  ],
  "expectedComplianceImprovement": 0.15
}
```

## 5. Java类设计 / Java Class Design

### 5.1 类结构
```
com.xindong.llm.prompts/
├── PromptScenario.java           — 场景枚举
├── PromptTemplate.java           — 模板接口
├── AbstractPromptTemplate.java   — 抽象基类（通用逻辑）
├── InitialAssessmentPrompt.java  — 初始评估模板
├── DailyTaskPrompt.java          — 每日任务生成模板
├── ProgressReviewPrompt.java     — 进展回顾模板
├── PlanAdjustmentPrompt.java     — 计划调整模板
├── PromptTemplateRegistry.java   — 模板注册表（按scenario查找）
├── PromptInput.java              — 输入数据包装
└── PromptOutputParser.java       — 输出JSON解析器
```

### 5.2 PromptTemplate 接口
```java
public interface PromptTemplate {
    PromptScenario scenario();
    String version();
    String systemPrompt();
    String userPrompt(PromptInput input);
    String expectedOutputSchema();
}
```

### 5.3 与现有系统集成
- `ClaudeEngineService.call()` 的 systemPrompt/userPrompt 参数由模板提供
- callType 映射到 PromptScenario 枚举值
- Mock Provider 根据 scenario 返回预定义的 JSON 响应
- 输出由 `PromptOutputParser` 解析为 domain model

## 6. Mock测试数据 / Mock Test Scenarios

| # | 场景 | 用户画像 | 预期输出重点 |
|---|------|----------|-------------|
| 1 | INITIAL_ASSESSMENT | 45岁男性，BMI 28，有代谢风险 | 低强度起步，3次/周 |
| 2 | DAILY_TASK | 成长期用户， readiness=78 | 中等强度跑步+拉伸 |
| 3 | PROGRESS_REVIEW | 4周后，分数+7，依从81% | 趋势改善，增加力量 |
| 4 | PLAN_ADJUSTMENT | 停滞期用户，7天未训练 | 降频+换运动类型 |
| 5 | DAILY_TASK | 初学期女性， readiness=45 | 休息建议，轻度瑜伽 |
