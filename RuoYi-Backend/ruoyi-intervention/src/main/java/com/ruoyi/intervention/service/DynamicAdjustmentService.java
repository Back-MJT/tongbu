package com.ruoyi.intervention.service;

import com.ruoyi.intervention.domain.enums.*;
import com.ruoyi.intervention.domain.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dynamic Adjustment Service — Real-time data-driven intervention parameter optimization.
 *
 * Integrates:
 *   - TrendAnalysis from DynamicHealthProfileService (7-day window trend detection)
 *   - AnomalyDetectionResult from DynamicHealthProfileService (z-score anomaly events)
 *   - AdaptationRecommendation from ProgressStatsService (feedback loop signals)
 *   - Preset rule library (ACSM progressive overload, fatigue recovery, JITAI patterns)
 *
 * Reference:
 *   - ACSM 2021 Ch.4 progressive overload, Ch.7 exercise prescription
 *   - Nahum-Shani et al. 2018 JITAI design patterns
 *   - Klasnja et al. 2015 JITAI theory
 *   - Bomberg 2017 progressive overload review
 */
public class DynamicAdjustmentService {

    // --- Module-Level Configuration Constants ---
    private static final int TREND_WINDOW_DAYS = 7;
    private static final double ANOMALY_ZSCORE_TRIGGER = 2.0;
    private static final double MAX_SINGLE_INTENSITY_CHANGE = 0.15;
    private static final double MAX_SINGLE_DURATION_CHANGE_PCT = 0.20;
    private static final double MIN_INTENSITY_FLOOR = 0.30;
    private static final double MAX_INTENSITY_CEILING = 1.0;
    private static final int MIN_SESSION_DURATION_MIN = 5;
    private static final int MAX_SESSION_DURATION_MIN = 120;
    private static final double PROGRESSIVE_OVERLOAD_WEEKLY_INTENSITY_INCREMENT = 0.025;
    private static final double PROGRESSIVE_OVERLOAD_WEEKLY_VOLUME_INCREMENT = 0.05;
    private static final int FATIGUE_RECOVERY_DAYS_MILD = 1;
    private static final double DECLINING_TREND_INTENSITY_DECREASE = 0.10;
    private static final int STAGNATION_WEEKS = 4;

    // --- In-Memory Storage ---
    private static final ConcurrentHashMap<String, UserAdjustmentState> STATES = new ConcurrentHashMap<>();

    private static class UserAdjustmentState {
        String userId;
        Map<String, Object> currentParams = new ConcurrentHashMap<>();
        Map<String, LocalDateTime> ruleCooldowns = new ConcurrentHashMap<>();
        LocalDateTime lastAdjustment;
        List<AdjustmentHistoryEntry> history = new ArrayList<>();
        List<InterventionRecommendation> recommendations = new ArrayList<>();
    }

    private static final DynamicAdjustmentService INSTANCE = new DynamicAdjustmentService();
    public static DynamicAdjustmentService getInstance() { return INSTANCE; }
    private DynamicAdjustmentService() {}

    // --- Preset Adjustment Rules ---
    private List<AdjustmentRuleSpec> buildPresetRules() {
        List<AdjustmentRuleSpec> rules = new ArrayList<>();

        // Week 1-2: Maintenance phase
        rules.add(newRule("acsm_progressive_overload_week1_2_maintain",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.TREND,
            Map.of("trend_direction", "STABLE", "weeks_since_start", List.of(1, 2)),
            AdjustmentDirection.MAINTAIN, 0.0, 10, 168,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "ACSM Ch.4: First 2 weeks are adaptation phase — maintain to prevent overtraining.", "acsm_2021_ch4"));

        // Week 3+: Improving trend — apply progressive overload
        rules.add(newRule("acsm_progressive_overload_week3_improving",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.TREND,
            Map.of("trend_direction", "IMPROVING", "weeks_since_start", List.of(3, 52)),
            AdjustmentDirection.INCREASE, PROGRESSIVE_OVERLOAD_WEEKLY_INTENSITY_INCREMENT, 20, 168,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "ACSM Ch.4: Progressive overload — increase intensity 2.5% per week when adapting well.", "acsm_2021_ch4"));

        // Improving trend — also increase volume (duration)
        rules.add(newRule("acsm_volume_increase_improving",
            AdjustmentDimension.DURATION, AdjustmentTriggerType.TREND,
            Map.of("trend_direction", "IMPROVING", "weeks_since_start", List.of(3, 52)),
            AdjustmentDirection.INCREASE, PROGRESSIVE_OVERLOAD_WEEKLY_VOLUME_INCREMENT, 15, 168,
            Map.of("min", (double) MIN_SESSION_DURATION_MIN, "max", (double) MAX_SESSION_DURATION_MIN),
            "ACSM Ch.7: Volume (duration) increase 5% per week during adaptation phase.", "acsm_2021_ch7"));

        // Declining trend — decrease intensity to manage fatigue
        rules.add(newRule("fatigue_management_declining",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.TREND,
            Map.of("trend_direction", "DECLINING"),
            AdjustmentDirection.DECREASE, DECLINING_TREND_INTENSITY_DECREASE, 30, 72,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "ACSM Ch.4: Declining trend suggests accumulated fatigue — reduce intensity 10%.", "acsm_2021_ch4"));

        // Anomaly-Based Immediate Adjustments
        rules.add(newRule("anomaly_hr_spike_reduce",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.ANOMALY,
            Map.of("anomaly_type", "SPIKE", "metric", "resting_hr", "severity", "moderate"),
            AdjustmentDirection.DECREASE, 0.15, 50, 24,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "HR spike anomaly — immediate 15% intensity reduction for cardiac safety.", "nahum_shani_2018_jitai_design"));

        rules.add(newRule("anomaly_hr_drop_reduce_frequency",
            AdjustmentDimension.FREQUENCY, AdjustmentTriggerType.ANOMALY,
            Map.of("anomaly_type", "DROP", "metric", "resting_hr", "severity", "moderate"),
            AdjustmentDirection.DECREASE, 1.0, 50, 48,
            null,
            "HR drop anomaly — possible overtraining syndrome. Reduce frequency by 1 session/wk.", "nahum_shani_2018_jitai_design"));

        rules.add(newRule("anomaly_activity_drop_add_recovery",
            AdjustmentDimension.RECOVERY, AdjustmentTriggerType.ANOMALY,
            Map.of("anomaly_type", "DROP", "metric", "exercise_minutes", "severity", "mild"),
            AdjustmentDirection.INCREASE, (double) FATIGUE_RECOVERY_DAYS_MILD, 40, 72,
            null,
            "Activity drop detected — add 1 recovery day to prevent overtraining.", "acsm_2021_ch4"));

        rules.add(newRule("anomaly_sleep_deficit_reduce_evening_intensity",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.ANOMALY,
            Map.of("anomaly_type", "DROP", "metric", "sleep_hours", "severity", "moderate"),
            AdjustmentDirection.DECREASE, 0.20, 35, 48,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "Sleep deficit detected — reduce evening exercise intensity by 20% to improve sleep quality.", "hirshkowitz_2015"));

        // Feedback-Loop Integration Rules
        rules.add(newRule("feedback_high_rpe_decrease",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.FEEDBACK,
            Map.of("adaptation_rule", "DECREASE_INTENSITY"),
            AdjustmentDirection.DECREASE, 0.05, 25, 24,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "User reported high RPE — decrease intensity 5% per event.", "nahum_shani_2018_jitai_design"));

        rules.add(newRule("feedback_low_rpe_increase",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.FEEDBACK,
            Map.of("adaptation_rule", "INCREASE_INTENSITY"),
            AdjustmentDirection.INCREASE, 0.05, 20, 24,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "User reported low RPE — safe to increase intensity 5%.", "nahum_shani_2018_jitai_design"));

        rules.add(newRule("feedback_adverse_event_halve_intensity",
            AdjustmentDimension.INTENSITY, AdjustmentTriggerType.FEEDBACK,
            Map.of("adaptation_rule", "DECREASE_INTENSITY", "adverse_event", Boolean.TRUE),
            AdjustmentDirection.DECREASE, 0.50, 60, 24,
            Map.of("min", MIN_INTENSITY_FLOOR, "max", MAX_INTENSITY_CEILING),
            "Adverse event reported — halve intensity immediately for safety.", "klasnja_2015_jitai"));

        // Stagnation / Strategy Switching
        rules.add(newRule("stagnation_switch_type",
            AdjustmentDimension.TYPE, AdjustmentTriggerType.TREND,
            Map.of("stagnation_weeks", STAGNATION_WEEKS),
            AdjustmentDirection.SWITCH, 1.0, 15, 672,
            null,
            "4+ weeks without improvement — switching exercise type can break plateaus.", "nahum_shani_2018_jitai_design"));

        return rules;
    }

    private AdjustmentRuleSpec newRule(String ruleId, AdjustmentDimension dim,
            AdjustmentTriggerType triggerType, Map<String, Object> conditions,
            AdjustmentDirection direction, double magnitude, int priority, int cooldownHrs,
            Map<String, Double> safetyBounds, String rationale, String evidenceRef) {
        return new AdjustmentRuleSpec(ruleId, dim, triggerType, conditions, direction,
            magnitude, priority, cooldownHrs, safetyBounds, rationale, evidenceRef, true);
    }

    private final List<AdjustmentRuleSpec> presetRules = buildPresetRules();

    // --- Parameter State ---

    public void setCurrentParameters(String userId, Map<String, Object> params) {
        UserAdjustmentState state = STATES.computeIfAbsent(userId, k -> {
            UserAdjustmentState s = new UserAdjustmentState();
            s.userId = userId;
            return s;
        });
        state.currentParams.putAll(params);
    }

    public Map<String, Object> getCurrentParameters(String userId) {
        UserAdjustmentState state = STATES.get(userId);
        if (state == null) return Map.of();
        return Map.copyOf(state.currentParams);
    }

    // --- Trigger Evaluation ---

    public List<AdjustmentTrigger> evaluateTriggers(String userId,
            List<TrendAnalysis> trends, List<AnomalyDetectionResult> anomalies,
            List<AdaptationRecommendation> adaptations, int weeksSinceStart,
            int stagnationWeeks) {

        List<AdjustmentTrigger> triggers = new ArrayList<>();

        // Trend triggers
        if (trends != null) {
            for (TrendAnalysis trend : trends) {
                if (trend.trend() != TrendType.INSUFFICIENT_DATA) {
                    AdjustmentTrigger t = new AdjustmentTrigger();
                    t.setTriggerId("trigger_trend_" + userId + "_" + trend.metric() + "_" + System.currentTimeMillis());
                    t.setTriggerType(AdjustmentTriggerType.TREND);
                    t.setUserId(userId);
                    t.setDimension(metricToDimension(trend.metric()));
                    t.setTrend(trend);
                    t.setConfidence(trend.confidence());
                    t.setEvidenceRef("nahum_shani_2018_jitai_design");
                    triggers.add(t);
                }
            }
        }

        // Anomaly triggers
        if (anomalies != null) {
            for (AnomalyDetectionResult anomaly : anomalies) {
                if (Math.abs(anomaly.zScore()) >= ANOMALY_ZSCORE_TRIGGER) {
                    double conf = Math.min(1.0, Math.abs(anomaly.zScore()) / 3.0);
                    AdjustmentTrigger t = AdjustmentTrigger.forAnomaly(
                        "trigger_anomaly_" + userId + "_" + anomaly.metric() + "_" + System.currentTimeMillis(),
                        userId, metricToDimension(anomaly.metric()), anomaly, conf,
                        "nahum_shani_2018_jitai_design");
                    triggers.add(t);
                }
            }
        }

        // Feedback triggers
        if (adaptations != null) {
            for (AdaptationRecommendation adaptation : adaptations) {
                if (adaptation.getConfidence() >= 0.5) {
                    AdjustmentTrigger t = AdjustmentTrigger.forFeedback(
                        "trigger_feedback_" + userId + "_" + System.currentTimeMillis(),
                        userId, adaptationRuleToDimension(adaptation.getAdaptationRule()),
                        adaptation, adaptation.getConfidence(),
                        "nahum_shani_2018_jitai_design");
                    triggers.add(t);
                }
            }
        }

        // Stagnation trigger
        if (stagnationWeeks >= STAGNATION_WEEKS) {
            AdjustmentTrigger t = AdjustmentTrigger.forStagnation(
                "trigger_stagnation_" + userId + "_" + System.currentTimeMillis(),
                userId, AdjustmentDimension.TYPE,
                stagnationWeeks + " weeks without improvement — strategy switch recommended",
                0.7, "nahum_shani_2018_jitai_design");
            triggers.add(t);
        }

        return triggers;
    }

    private AdjustmentDimension metricToDimension(String metric) {
        return switch (metric) {
            case "exercise_minutes", "resting_hr" -> AdjustmentDimension.INTENSITY;
            case "sleep_hours" -> AdjustmentDimension.SLEEP_HYGIENE;
            case "steps" -> AdjustmentDimension.FREQUENCY;
            case "hrv", "hrv_rmssd" -> AdjustmentDimension.RECOVERY;
            case "protein_g", "water_liters" -> AdjustmentDimension.NUTRITION;
            default -> AdjustmentDimension.INTENSITY;
        };
    }

    private AdjustmentDimension adaptationRuleToDimension(AdaptationRule rule) {
        if (rule == null) return AdjustmentDimension.INTENSITY;
        return switch (rule) {
            case INCREASE_INTENSITY, DECREASE_INTENSITY -> AdjustmentDimension.INTENSITY;
            case ADD_REST -> AdjustmentDimension.RECOVERY;
            case SWITCH_TYPE -> AdjustmentDimension.TYPE;
            case REDUCE_FREQUENCY -> AdjustmentDimension.FREQUENCY;
            case MAINTAIN -> AdjustmentDimension.INTENSITY;
        };
    }

    // --- Core Recommendation Engine ---

    public InterventionRecommendation evaluateAndRecommend(String userId, String interventionId,
            List<TrendAnalysis> trends, List<AnomalyDetectionResult> anomalies,
            List<AdaptationRecommendation> adaptations, int weeksSinceStart, int stagnationWeeks) {

        UserAdjustmentState state = STATES.computeIfAbsent(userId, k -> {
            UserAdjustmentState s = new UserAdjustmentState();
            s.userId = userId;
            return s;
        });

        // Check global cooldown
        if (state.lastAdjustment != null) {
            long hours = Duration.between(state.lastAdjustment, LocalDateTime.now()).toHours();
            if (hours < 24) return null;
        }

        // Evaluate triggers
        List<AdjustmentTrigger> triggers = evaluateTriggers(userId, trends, anomalies,
            adaptations, weeksSinceStart, stagnationWeeks);
        if (triggers.isEmpty()) return null;

        // Match rules to triggers
        List<AdjustmentAction> actions = new ArrayList<>();
        List<String> matchedRuleIds = new ArrayList<>();
        List<String> triggerIds = new ArrayList<>();

        for (AdjustmentTrigger trigger : triggers) {
            for (AdjustmentRuleSpec rule : presetRules) {
                if (!rule.isActive()) continue;
                if (isOnCooldown(rule, state)) continue;

                boolean matched = matchRule(rule, trigger, weeksSinceStart, stagnationWeeks);
                if (!matched) continue;

                AdjustmentAction action = computeAdjustment(userId, rule, state, trigger);
                if (action == null) continue;

                actions.add(action);
                matchedRuleIds.add(rule.getRuleId());
                triggerIds.add(trigger.getTriggerId());
                applyCooldown(rule, state);
            }
        }

        if (actions.isEmpty()) return null;

        // Build recommendation
        String recId = "rec_" + userId + "_" + interventionId + "_" + System.currentTimeMillis();
        String risk = assessRisk(actions);
        double confidence = computeConfidence(triggers, actions);

        InterventionRecommendation rec = new InterventionRecommendation(recId, userId, interventionId);
        rec.setActions(actions);
        rec.setTriggerSummary(summarizeTriggers(triggers));
        rec.setRiskLevel(risk);
        rec.setTriggeredByTriggers(triggerIds);
        rec.setMatchedRules(matchedRuleIds);
        rec.setConfidence(confidence);
        rec.setEvidenceRefs(List.of(
            "acsm_2021_ch4", "acsm_2021_ch7", "nahum_shani_2018_jitai_design",
            "klasnja_2015_jitai", "hirshkowitz_2015", "bomberg_2017_progressive_overload"
        ));
        rec.setApplied(false);

        state.recommendations.add(rec);
        state.lastAdjustment = LocalDateTime.now();

        return rec;
    }

    private boolean matchRule(AdjustmentRuleSpec rule, AdjustmentTrigger trigger,
            int weeksSinceStart, int stagnationWeeks) {

        if (rule.getTriggerType() != trigger.getTriggerType()) return false;

        Map<String, Object> cond = rule.getTriggerConditions();
        if (cond == null) return true;

        if (rule.getTriggerType() == AdjustmentTriggerType.TREND) {
            TrendAnalysis trend = trigger.getTrend();
            if (trend == null) return false;

            if (cond.containsKey("stagnation_weeks")) {
                int sw = ((Number) cond.get("stagnation_weeks")).intValue();
                if (stagnationWeeks < sw) return false;
            }

            if (cond.containsKey("trend_direction")) {
                String dir = String.valueOf(cond.get("trend_direction"));
                if (!trend.trend().name().equalsIgnoreCase(dir)) return false;
            }

            if (cond.containsKey("metric")) {
                if (!trend.metric().equals(cond.get("metric"))) return false;
            }

            if (cond.containsKey("weeks_since_start")) {
                Object ws = cond.get("weeks_since_start");
                int lo, hi;
                if (ws instanceof List) {
                    List<?> lst = (List<?>) ws;
                    lo = ((Number) lst.get(0)).intValue();
                    hi = ((Number) lst.get(1)).intValue();
                } else {
                    lo = hi = ((Number) ws).intValue();
                }
                if (!(lo <= weeksSinceStart && weeksSinceStart <= hi)) return false;
            }
        }

        if (rule.getTriggerType() == AdjustmentTriggerType.ANOMALY) {
            AnomalyDetectionResult anomaly = trigger.getAnomaly();
            if (anomaly == null) return false;

            if (cond.containsKey("anomaly_type")) {
                String at = String.valueOf(cond.get("anomaly_type"));
                if (!anomaly.anomalyType().name().equalsIgnoreCase(at)) return false;
            }
            if (cond.containsKey("metric")) {
                if (!anomaly.metric().equals(cond.get("metric"))) return false;
            }
            if (cond.containsKey("severity")) {
                String sev = String.valueOf(cond.get("severity"));
                if (!anomaly.severity().equalsIgnoreCase(sev)) return false;
            }
        }

        if (rule.getTriggerType() == AdjustmentTriggerType.FEEDBACK) {
            AdaptationRecommendation adaptation = trigger.getAdaptation();
            if (adaptation == null) return false;
            AdaptationRule ar = adaptation.getAdaptationRule();

            if (cond.containsKey("adaptation_rule")) {
                String arStr = String.valueOf(cond.get("adaptation_rule"));
                if (!ar.name().equalsIgnoreCase(arStr)) return false;
            }
        }

        return true;
    }

    private boolean isOnCooldown(AdjustmentRuleSpec rule, UserAdjustmentState state) {
        LocalDateTime last = state.ruleCooldowns.get(rule.getRuleId());
        if (last == null) return false;
        long hours = Duration.between(last, LocalDateTime.now()).toHours();
        return hours < rule.getCooldownHours();
    }

    private void applyCooldown(AdjustmentRuleSpec rule, UserAdjustmentState state) {
        state.ruleCooldowns.put(rule.getRuleId(), LocalDateTime.now());
    }

    private AdjustmentAction computeAdjustment(String userId, AdjustmentRuleSpec rule,
            UserAdjustmentState state, AdjustmentTrigger trigger) {

        String paramName = dimensionToParam(rule.getDimension());
        double oldVal = getParamFloat(state.currentParams, paramName, defaultParamValue(paramName));

        double newVal;
        switch (rule.getDirection()) {
            case MAINTAIN -> newVal = oldVal;
            case INCREASE -> newVal = oldVal * (1.0 + rule.getMagnitude());
            case DECREASE -> newVal = oldVal * (1.0 - rule.getMagnitude());
            case SWITCH -> newVal = -1.0; // sentinel for type switch
            default -> newVal = oldVal;
        }

        boolean bounded = false;
        if (rule.getSafetyBounds() != null && newVal >= 0) {
            Map<String, Double> sb = rule.getSafetyBounds();
            if (sb.containsKey("min") && newVal < sb.get("min")) { newVal = sb.get("min"); bounded = true; }
            if (sb.containsKey("max") && newVal > sb.get("max")) { newVal = sb.get("max"); bounded = true; }
        }

        if (rule.getDimension() == AdjustmentDimension.INTENSITY && newVal >= 0) {
            if (newVal < MIN_INTENSITY_FLOOR) { newVal = MIN_INTENSITY_FLOOR; bounded = true; }
            if (newVal > MAX_INTENSITY_CEILING) { newVal = MAX_INTENSITY_CEILING; bounded = true; }
            double changePct = oldVal > 0 ? Math.abs(newVal - oldVal) / oldVal : 0;
            if (changePct > MAX_SINGLE_INTENSITY_CHANGE) {
                double sign = newVal > oldVal ? 1.0 : -1.0;
                newVal = oldVal * (1.0 + MAX_SINGLE_INTENSITY_CHANGE * sign);
                bounded = true;
            }
        }

        if (rule.getDimension() == AdjustmentDimension.DURATION && newVal >= 0) {
            if (newVal < MIN_SESSION_DURATION_MIN) { newVal = MIN_SESSION_DURATION_MIN; bounded = true; }
            if (newVal > MAX_SESSION_DURATION_MIN) { newVal = MAX_SESSION_DURATION_MIN; bounded = true; }
            double changePct = oldVal > 0 ? Math.abs(newVal - oldVal) / oldVal : 0;
            if (changePct > MAX_SINGLE_DURATION_CHANGE_PCT) {
                double sign = newVal > oldVal ? 1.0 : -1.0;
                newVal = oldVal * (1.0 + MAX_SINGLE_DURATION_CHANGE_PCT * sign);
                bounded = true;
            }
        }

        if (rule.getDirection() != AdjustmentDirection.SWITCH) {
            state.currentParams.put(paramName, round(newVal, 4));
        }

        double changeFrac = oldVal > 0 ? (newVal - oldVal) / oldVal : 0.0;

        return new AdjustmentAction(
            "action_" + rule.getRuleId() + "_" + userId + "_" + System.currentTimeMillis(),
            userId, rule.getDimension(), rule.getDirection(),
            paramName, oldVal, newVal,
            round(changeFrac, 4), rule.getMagnitude(),
            !bounded,
            rule.getRuleId(), rule.getRationale(), rule.getEvidenceRef()
        );
    }

    private String dimensionToParam(AdjustmentDimension dim) {
        return switch (dim) {
            case INTENSITY -> "intensity_multiplier";
            case DURATION -> "session_duration_min";
            case FREQUENCY -> "sessions_per_week";
            case RECOVERY -> "rest_days";
            case TYPE -> "exercise_type";
            case SLEEP_HYGIENE -> "sleep_hygiene_score";
            case NUTRITION -> "nutrition_compliance";
        };
    }

    private double getParamFloat(Map<String, Object> params, String key, double defaultVal) {
        Object v = params.get(key);
        if (v == null) return defaultVal;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }

    private double defaultParamValue(String param) {
        return switch (param) {
            case "intensity_multiplier" -> 0.6;
            case "session_duration_min" -> 30.0;
            case "sessions_per_week" -> 3.0;
            case "rest_days" -> 1.0;
            default -> 1.0;
        };
    }

    private String assessRisk(List<AdjustmentAction> actions) {
        if (actions.stream().anyMatch(a -> !a.isSafetyBoundsRespected())) return "high";
        if (actions.stream().anyMatch(a -> a.getMagnitude() >= 0.3)) return "moderate";
        if (actions.size() > 3) return "moderate";
        return "low";
    }

    private double computeConfidence(List<AdjustmentTrigger> triggers, List<AdjustmentAction> actions) {
        if (triggers.isEmpty()) return 0.0;
        double avgConf = triggers.stream().mapToDouble(AdjustmentTrigger::getConfidence).sum() / triggers.size();
        double boost = Math.min(0.1, actions.size() * 0.02);
        return Math.min(0.95, avgConf + boost);
    }

    private String summarizeTriggers(List<AdjustmentTrigger> triggers) {
        List<String> parts = new ArrayList<>();
        for (AdjustmentTrigger t : triggers) {
            if (t.getTriggerType() == AdjustmentTriggerType.TREND && t.getTrend() != null) {
                parts.add(t.getTriggerType().name().toLowerCase() + ": " +
                    t.getTrend().trend().name().toLowerCase() + " trend in " + t.getTrend().metric());
            } else if (t.getTriggerType() == AdjustmentTriggerType.ANOMALY && t.getAnomaly() != null) {
                parts.add(t.getTriggerType().name().toLowerCase() + ": " +
                    t.getAnomaly().anomalyType().name().toLowerCase() + " in " + t.getAnomaly().metric() +
                    " (z=" + t.getAnomaly().zScore() + ")");
            } else if (t.getTriggerType() == AdjustmentTriggerType.FEEDBACK && t.getAdaptation() != null) {
                parts.add(t.getTriggerType().name().toLowerCase() + ": " +
                    t.getAdaptation().getAdaptationRule().name().toLowerCase());
            } else if (t.getReason() != null) {
                parts.add(t.getReason());
            }
        }
        return String.join("; ", parts);
    }

    // --- History ---

    public List<AdjustmentHistoryEntry> getAdjustmentHistory(String userId, Integer limit) {
        UserAdjustmentState state = STATES.get(userId);
        if (state == null) return List.of();
        List<AdjustmentHistoryEntry> entries = new ArrayList<>(state.history);
        entries.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        int lim = limit != null ? limit : 50;
        return entries.subList(0, Math.min(entries.size(), lim));
    }

    public void recordAdjustment(String userId, String interventionId, InterventionRecommendation rec) {
        UserAdjustmentState state = STATES.computeIfAbsent(userId, k -> {
            UserAdjustmentState s = new UserAdjustmentState();
            s.userId = userId;
            return s;
        });
        for (AdjustmentAction action : rec.getActions()) {
            AdjustmentHistoryEntry entry = new AdjustmentHistoryEntry(
                "hist_" + userId + "_" + action.getActionId() + "_" + System.currentTimeMillis(),
                userId, interventionId, rec.getRecommendationId(),
                action.getDimension(), action.getParameterName(),
                action.getOldValue(), action.getNewValue(), action.getChangeFraction(),
                guessTriggerType(action.getTriggeredBy()),
                null, action.getTriggeredBy(),
                action.getRationale(), action.getEvidenceRef(),
                1.0
            );
            state.history.add(entry);
        }
    }

    private AdjustmentTriggerType guessTriggerType(String ruleId) {
        if (ruleId == null) return AdjustmentTriggerType.SCHEDULED;
        if (ruleId.contains("anomaly")) return AdjustmentTriggerType.ANOMALY;
        if (ruleId.contains("feedback")) return AdjustmentTriggerType.FEEDBACK;
        if (ruleId.contains("stagnation") || ruleId.contains("acsm") || ruleId.contains("progressive")) {
            return AdjustmentTriggerType.TREND;
        }
        return AdjustmentTriggerType.SCHEDULED;
    }

    // --- Recommendation Management ---

    public List<InterventionRecommendation> getRecommendations(String userId, boolean unappliedOnly) {
        UserAdjustmentState state = STATES.get(userId);
        if (state == null) return List.of();
        List<InterventionRecommendation> recs = new ArrayList<>(state.recommendations);
        if (unappliedOnly) recs.removeIf(InterventionRecommendation::isApplied);
        recs.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return recs;
    }

    public void markRecommendationApplied(String userId, String recommendationId) {
        UserAdjustmentState state = STATES.get(userId);
        if (state == null) return;
        for (InterventionRecommendation rec : state.recommendations) {
            if (rec.getRecommendationId().equals(recommendationId)) {
                rec.setApplied(true);
                rec.setAppliedAt(LocalDateTime.now());
                break;
            }
        }
    }

    // --- Convenience: Full Pipeline Integration ---

    public InterventionRecommendation evaluateUser(String userId, String interventionId,
            int weeksSinceStart, int stagnationWeeks) {

        DynamicHealthProfileService hp = DynamicHealthProfileService.getInstance();
        Map<String, TrendAnalysis> trendsMap = hp.analyzeTrends(userId);
        List<TrendAnalysis> trends = trendsMap.isEmpty() ? List.of()
            : new ArrayList<>(trendsMap.values());

        List<AnomalyDetectionResult> anomalies = new ArrayList<>();
        for (String metric : trendsMap.keySet()) {
            anomalies.addAll(hp.detectAnomalies(userId, metric, ANOMALY_ZSCORE_TRIGGER));
        }

        return evaluateAndRecommend(userId, interventionId, trends, anomalies,
            null, weeksSinceStart, stagnationWeeks);
    }

    // --- Utility ---
    private double round(double value, int decimals) {
        double mult = Math.pow(10, decimals);
        return Math.round(value * mult) / mult;
    }
}
