# Phase 2.1 算法升级规格说明书

**日期**: 2026-04-28
**状态**: Draft
**工程师**: Algorithm Engineer

## 目标

从静态健康画像 + 固定处方，升级为：

1. **动态健康画像 v2** — 时序建模、多模态融合、风险预测
2. **动态干预调整** — 趋势驱动 + 异常事件驱动的方案参数实时调整

## 架构概览

```
DynamicHealthProfileService          DynamicAdjustmentService
  - 时序数据存储/分析                    - 趋势触发器评估
  - 多模态融合评分                        - 异常事件检测
  - 风险预测                            - 调整规则引擎
  - 画像版本管理                        - 调整历史审计
        ↓                                      ↓
  HealthScoreService (existing)     RuleEngine (existing)
  TrainingPlanService (existing)    InterventionEngineClient
```

---

## 模块一：DynamicHealthProfileService

### 核心功能

#### 1. 时序数据管理
- `addTimeSeriesPoint(userId, metric, value, timestamp, source)` — 追加数据点
- `getTimeSeries(userId, metric, windowDays)` — 获取滑动窗口数据
- `computeTrend(userId, metric, windowDays)` — 计算线性趋势（斜率、R²、方向）
- `detectAnomalies(userId, metric, windowDays)` — Z-score 异常检测

#### 2. 多模态融合评分
- `computeMultimodalScore(userId, dataPoint)` → `MultimodalFusionReport`
  - exercise_score (权重 0.30): WHO 2020 运动分钟数
  - sleep_score (权重 0.25): 睡眠时长 + PSQI 质量
  - heart_rate_score (权重 0.25): 静息心率 + HRV
  - nutrition_score (权重 0.20): 蛋白质 + 水分摄入
- 综合评分 = 加权求和，输出 excellent/good/fair/poor 四级

#### 3. 风险预测
- `predictRisk(userId)` → `RiskPrediction`
  - 当前风险评分（0-100）
  - 30天风险轨迹预测（趋势外推）
  - 风险因素归因
  - 是否触发护理升级协议

#### 4. 画像版本管理
- `snapshotProfile(userId)` → `ProfileVersion`
- `compareProfiles(userId, v1, v2)` — 版本对比
- 版本存储支持历史回溯和审计

### 数据模型（Java）

```java
// 时序数据点
public record TimeSeriesPoint(
    String metric,
    double value,
    LocalDateTime timestamp,
    String source,
    String evidenceRef
) {}

// 趋势分析结果
public record TrendAnalysis(
    String metric,
    TrendType trend,        // IMPROVING/STABLE/DECLINING/INSUFFICIENT_DATA
    double slope,           // 线性回归斜率（单位/天）
    double changeFraction,  // 窗口起点到终点的分数变化
    double windowStartValue,
    double windowEndValue,
    int nPoints,
    double confidence,      // 0.0-1.0
    String evidenceRef
) {}

// 异常检测结果
public record AnomalyDetectionResult(
    LocalDateTime timestamp,
    String metric,
    AnomalyType type,       // SPIKE/DROP/OUTLIER
    double value,
    double expectedValue,
    double zScore,
    String severity,        // mild/moderate/severe
    String evidenceRef
) {}

// 多模态融合报告
public record MultimodalFusionReport(
    String userId,
    LocalDateTime assessedAt,
    DimensionFusionScore exerciseScore,
    DimensionFusionScore sleepScore,
    DimensionFusionScore heartRateScore,
    DimensionFusionScore nutritionScore,
    double compositeScore,
    String compositeGrade,
    String compositeGradeLabelZh,
    String configVersion,
    List<String> evidenceRefs
) {}

// 维度评分
public record DimensionFusionScore(
    String dimension,
    double rawScore,
    String grade,
    String gradeLabelZh,
    Map<String, Double> subScores,
    String evidenceRef
) {}

// 风险预测
public record RiskPrediction(
    String userId,
    LocalDateTime predictedAt,
    double currentRiskScore,
    RiskLevel currentRiskLevel,
    Double predictedRiskScore30d,
    TrendType riskTrajectory,
    List<String> contributingFactors,
    boolean escalationRecommended,
    String escalationReason,
    double confidence,
    String evidenceRef
) {}

// 画像版本
public record ProfileVersion(
    String versionId,
    String userId,
    int versionNumber,
    LocalDateTime createdAt,
    double overallScore,
    Double compositeFusionScore,
    RiskLevel riskLevel,
    TrendType overallTrend,
    Integer restingHrSnapshot,
    Integer stepsSnapshot,
    Double sleepHoursSnapshot,
    List<String> changelog,
    String evidenceRef
) {}
```

### 阈值常量（循证）

| 常量 | 值 | 证据来源 |
|------|----|---------|
| WINDOW_SIZE_DAYS | 7 | Klasnja 2015 JITAI |
| MIN_WINDOW_POINTS | 3 | 最小样本量 |
| TREND_SIGNIFICANCE_THRESHOLD | 0.15 (15%) | Nahum-Shani 2018 |
| ANOMALY_ZSCORE_THRESHOLD | 2.0 | Rosner 1983 ESD |
| FUSION_WEIGHT_exercise | 0.30 | WHO 2020 |
| FUSION_WEIGHT_sleep | 0.25 | AASM 2020 |
| FUSION_WEIGHT_heart_rate | 0.25 | Karvonen 1957 |
| FUSION_WEIGHT_nutrition | 0.20 | 专家共识 |
| EXERCISE_MIN_WEEKLY_MINUTES | 150.0 | WHO 2020 |
| SLEEP_OPTIMAL_MIN | 7.0h | Hirshkowitz 2015 |
| SLEEP_OPTIMAL_MAX | 9.0h | Hirshkowitz 2015 |
| HR_RESTING_EXCELLENT | <60bpm | ACSM 2021 Ch.4 |
| RISK_SCORE_HIGH | >=70 | ACSM 2021 Ch.2 |

---

## 模块二：DynamicAdjustmentService

### 核心功能

#### 1. 调整触发器评估
- `evaluateTriggers(userId)` → `List<AdjustmentTrigger>`
  - TREND: 7天滑动窗口趋势驱动
  - ANOMALY: Z-score 异常事件驱动
  - FEEDBACK: FeedbackLoop 反馈信号
  - SCHEDULED: 定时计划审查
  - MANUAL: 人工覆盖

#### 2. 调整规则引擎
- `matchRules(trigger)` → `List<AdjustmentRuleSpec>`
- 内置预设规则（循证）:
  - ACSM 渐进超负荷规则（Week 1-2 维持，Week 3+ 增加强度）
  - 疲劳管理规则（下降趋势降低强度 10%）
  - 异常事件即时调整（Z > 2.0 触发）
  - 长期停滞切换策略（4周无改善则切换）

#### 3. 调整执行 + 安全边界
- `applyAdjustment(userId, recommendation)` → `AdjustmentResult`
- 安全边界校验:
  - 单次强度变化 ≤ 15%
  - 单次时长变化 ≤ 20%
  - 频率变化 ≤ 1次/周
  - 强度地板: >= 30% (安全)
  - 强度天花板: <= 100%
  - 会话时长: 5-120分钟

#### 4. 调整历史审计
- `getAdjustmentHistory(userId)` → `List<AdjustmentHistoryEntry>`
- 完整记录: what/when/why/who

### 调整推荐输出

```java
public record AdjustmentRecommendation(
    String recommendationId,
    String userId,
    String interventionId,
    List<AdjustmentAction> actions,
    String triggerSummary,
    String riskLevel,       // low/moderate/high
    List<String> triggeredByTriggers,
    List<String> matchedRules,
    double confidence,
    List<String> evidenceRefs,
    LocalDateTime createdAt,
    boolean applied,
    LocalDateTime appliedAt
) {}

public record AdjustmentAction(
    String actionId,
    String userId,
    AdjustmentDimension dimension,  // INTENSITY/DURATION/FREQUENCY/TYPE/RECOVERY
    AdjustmentDirection direction,  // INCREASE/DECREASE/MANTAIN/SWITCH
    String parameterName,
    Object oldValue,
    Object newValue,
    double changeFraction,
    double magnitude,
    boolean safetyBoundsRespected,
    String triggeredBy,
    String rationale,
    String evidenceRef,
    LocalDateTime timestamp
) {}
```

### 预设调整规则

| rule_id | 触发条件 | 动作 | 证据 |
|---------|---------|------|------|
| acsm_progressive_overload_week1_2_maintain | 趋势=stable, Week 1-2 | 维持强度 | ACSM Ch.4 |
| acsm_progressive_overload_week3_improving | 趋势=improving, Week 3+ | 强度+2.5%/周 | ACSM Ch.4 |
| acsm_volume_increase_improving | 趋势=improving, Week 3+ | 时长+5%/周 | ACSM Ch.7 |
| fatigue_management_declining | 趋势=declining | 强度-10% | ACSM Ch.4 |
| anomaly_spike_moderate | 异常=z>2.0 | 降低10% | Nahum-Shani 2018 |
| stagnation_switch | 4周无改善 | 策略切换 | Nahum-Shani 2018 |

---

## 文件清单

### 新增文件

```
src/main/java/com/ruoyi/intervention/domain/model/
  TimeSeriesPoint.java
  TrendAnalysis.java
  AnomalyDetectionResult.java
  MultimodalFusionReport.java
  DimensionFusionScore.java
  RiskPrediction.java
  ProfileVersion.java
  AdjustmentRuleSpec.java
  AdjustmentTrigger.java
  AdjustmentAction.java
  AdjustmentRecommendation.java
  AdjustmentHistoryEntry.java
  DynamicAdjustmentConfig.java

src/main/java/com/ruoyi/intervention/domain/enums/
  TrendType.java
  AnomalyType.java
  RiskLevel.java
  AdjustmentTriggerType.java
  AdjustmentDimension.java
  AdjustmentDirection.java

src/main/java/com/ruoyi/intervention/service/
  DynamicHealthProfileService.java   (~600 lines)
  DynamicAdjustmentService.java       (~600 lines)

src/test/java/com/ruoyi/intervention/service/
  DynamicHealthProfileServiceTest.java
  DynamicAdjustmentServiceTest.java
```

### 修改文件

- `CoachApiController.java` — 新增 `/api/health-profile/{userId}/timeseries` 端点
- `TrainingApiController.java` — 新增 `/api/adjustment/evaluate` 端点
- `pom.xml` — 如需要统计相关依赖（可选，无外部依赖）

---

## 依赖关系

```
DynamicHealthProfileService
  ├── BaselineScores (existing)
  ├── BaselineScoringService (existing)
  ├── HeartRateService (existing)
  └── HealthScoreService (existing)

DynamicAdjustmentService
  ├── DynamicHealthProfileService (new)
  ├── FeedbackLoop → ProgressStatsService (existing)
  ├── RuleEngine (existing)
  └── TrainingPlanService (existing)
```

---

## 测试策略

1. **单元测试**: 每个 service 独立测试，mock 所有依赖
2. **算法一致性**: 对比 Java 输出与 Python 参考实现的数值结果
3. **边界测试**: 安全边界、极值输入、空数据处理
4. **集成测试**: 端到端流程（数据输入 → 画像计算 → 调整推荐）

---

## 预计工作量

- DynamicHealthProfileService: 3 天
- DynamicAdjustmentService: 3 天
- 测试 + 一致性验证: 2 天
- 集成 + API 端点: 1 天
- **总计: 9 天（约2周）**
