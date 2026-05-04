package com.ruoyi.intervention.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ruoyi.intervention.domain.enums.TransitionTrigger;
import com.ruoyi.intervention.domain.enums.UserStage;
import com.ruoyi.intervention.domain.model.StageBehavior;
import com.ruoyi.intervention.domain.model.StageThresholds;
import com.ruoyi.intervention.domain.model.StageTransition;
import com.ruoyi.intervention.domain.model.UserTrainingData;

/**
 * 用户阶段识别服务 — 基于训练行为数据的用户生命周期阶段判定.
 * User Stage Identification for AI Training Engine.
 *
 * <p>Identifies which lifecycle stage a user is in based on training behavior,
 * enabling stage-appropriate engine behavior (difficulty, feedback style, incentives).
 *
 * <p>Evaluation priority (first match wins):
 * <ol>
 *   <li>Plateau — inactivity check (highest priority for intervention)</li>
 *   <li>Advanced — tenure + frequency + intensity growth</li>
 *   <li>Growth — tenure + frequency + completion</li>
 *   <li>Beginner — default / early stage</li>
 * </ol>
 *
 * <p>Migrated from: intervention-engine/src/algorithms/user_stage.py
 * <br>Evidence base:
 * <ul>
 *   <li>Prochaska & DiClemente 1983: Stages of Change (Transtheoretical Model)</li>
 *   <li>Fogg 2020: "Tiny Habits" behavior model — frequency as adoption signal</li>
 *   <li>Board v1.1 Section 2.3: User stage definitions for HealthHub</li>
 * </ul>
 */
@Service
public class UserStageService
{
    private static final Logger log = LoggerFactory.getLogger(UserStageService.class);

    // ========== Default Stage Behaviors ==========

    private static final Map<UserStage, StageBehavior> DEFAULT_STAGE_BEHAVIORS;
    static
    {
        DEFAULT_STAGE_BEHAVIORS = new EnumMap<>(UserStage.class);
        DEFAULT_STAGE_BEHAVIORS.put(UserStage.BEGINNER, new StageBehavior(
            UserStage.BEGINNER, 0.6, "encouraging", "achievement_unlock",
            "beginner_coach",
            "Lower difficulty, strong positive reinforcement, achievement milestones"));
        DEFAULT_STAGE_BEHAVIORS.put(UserStage.GROWTH, new StageBehavior(
            UserStage.GROWTH, 1.0, "progressive", "variety_introduction",
            "growth_coach",
            "Gradual progressive overload, introduce training variety"));
        DEFAULT_STAGE_BEHAVIORS.put(UserStage.PLATEAU, new StageBehavior(
            UserStage.PLATEAU, 0.8, "reengagement", "churn_prevention",
            "plateau_coach",
            "Churn risk alert, adjust plan difficulty, re-engagement push"));
        DEFAULT_STAGE_BEHAVIORS.put(UserStage.ADVANCED, new StageBehavior(
            UserStage.ADVANCED, 1.3, "competitive", "periodization_goals",
            "advanced_coach",
            "Periodized training plans, competitive targets"));
    }

    // ========== In-Memory Storage (Phase 1) ==========

    private final Map<String, UserStage> userStages = new HashMap<>();
    private final Map<String, List<StageTransition>> transitionHistory = new HashMap<>();

    // ========== Core Functions ==========

    /**
     * Determine the current lifecycle stage for a user.
     *
     * @param data User training behavior data
     * @param thresholds Custom thresholds (null for defaults)
     * @param referenceTime Override current time (null for now) — useful in tests
     * @return The identified UserStage
     */
    public UserStage identifyStage(UserTrainingData data, StageThresholds thresholds,
                                   LocalDateTime referenceTime)
    {
        StageThresholds t = (thresholds != null) ? thresholds : new StageThresholds();
        LocalDateTime now = (referenceTime != null) ? referenceTime : LocalDateTime.now();

        // 1. Plateau check — inactivity takes highest priority
        if (data.getLastSessionDate() != null)
        {
            long daysInactive = ChronoUnit.DAYS.between(data.getLastSessionDate(), now);
            if (daysInactive >= t.getPlateauInactivityDays())
            {
                log.debug("Plateau detected: {} days inactive for user {}", daysInactive, data.getUserId());
                return UserStage.PLATEAU;
            }
        }

        // Also plateau if intensity is declining (even if still active)
        if (data.getIntensityTrend() <= t.getPlateauIntensityDecline()
                && data.getTenureWeeks() > t.getBeginnerMaxWeeks())
        {
            log.debug("Plateau detected: intensity trend {} declining for user {}",
                      data.getIntensityTrend(), data.getUserId());
            return UserStage.PLATEAU;
        }

        // 2. Advanced check
        if (data.getTenureWeeks() >= t.getAdvancedMinWeeks()
                && data.getWeeklyFrequency() >= t.getAdvancedMinFrequency()
                && data.getIntensityTrend() >= t.getAdvancedMinIntensityTrend())
        {
            return UserStage.ADVANCED;
        }

        // 3. Growth check
        if (data.getTenureWeeks() > t.getBeginnerMaxWeeks()
                && data.getWeeklyFrequency() >= t.getGrowthMinFrequency()
                && data.getCompletionRate() >= t.getGrowthMinCompletion())
        {
            return UserStage.GROWTH;
        }

        // 4. Beginner (default)
        return UserStage.BEGINNER;
    }

    /**
     * Convenience overload with default thresholds and current time.
     */
    public UserStage identifyStage(UserTrainingData data)
    {
        return identifyStage(data, null, null);
    }

    /**
     * Check if a user should transition stages and return the transition record.
     *
     * @param userId User identifier
     * @param data Updated training data
     * @param thresholds Custom thresholds (null for defaults)
     * @param referenceTime Override current time (null for now)
     * @return StageTransition if stage changed, null otherwise
     */
    public StageTransition determineTransition(String userId, UserTrainingData data,
                                                StageThresholds thresholds,
                                                LocalDateTime referenceTime)
    {
        UserStage newStage = identifyStage(data, thresholds, referenceTime);
        UserStage currentStage = userStages.get(userId);

        if (currentStage == newStage)
        {
            return null;
        }

        StageThresholds t = (thresholds != null) ? thresholds : new StageThresholds();
        LocalDateTime now = (referenceTime != null) ? referenceTime : LocalDateTime.now();

        TransitionTrigger trigger = inferTrigger(data, currentStage, newStage, t, now);

        StageTransition transition = new StageTransition();
        transition.setUserId(userId);
        transition.setFromStage(currentStage);
        transition.setToStage(newStage);
        transition.setTrigger(trigger);
        transition.setTimestamp(now);
        transition.setEvidence(buildEvidenceMap(data));

        // Persist in memory
        userStages.put(userId, newStage);
        transitionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(transition);

        log.info("Stage transition: user {} {} -> {} (trigger: {})",
                 userId, currentStage, newStage, trigger.getValue());

        return transition;
    }

    /**
     * Convenience overload with default thresholds and current time.
     */
    public StageTransition determineTransition(String userId, UserTrainingData data)
    {
        return determineTransition(userId, data, null, null);
    }

    /**
     * Get the current stored stage for a user, or null if not yet evaluated.
     */
    public UserStage getUserStage(String userId)
    {
        return userStages.get(userId);
    }

    /**
     * Get the engine behavior configuration for a given stage.
     */
    public StageBehavior getStageBehavior(UserStage stage)
    {
        return DEFAULT_STAGE_BEHAVIORS.get(stage);
    }

    /**
     * Get all recorded stage transitions for a user.
     */
    public List<StageTransition> getTransitionHistory(String userId)
    {
        return transitionHistory.getOrDefault(userId, Collections.emptyList());
    }

    /**
     * Manually override a user's stage (admin/CE action).
     *
     * @param userId User identifier
     * @param stage Target stage
     * @param reason Justification for the override
     * @return The StageTransition record
     */
    public StageTransition setUserStage(String userId, UserStage stage, String reason)
    {
        UserStage fromStage = userStages.get(userId);

        StageTransition transition = new StageTransition();
        transition.setUserId(userId);
        transition.setFromStage(fromStage);
        transition.setToStage(stage);
        transition.setTrigger(TransitionTrigger.MANUAL_OVERRIDE);
        transition.setTimestamp(LocalDateTime.now());

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("reason", reason != null ? reason : "");
        transition.setEvidence(evidence);

        userStages.put(userId, stage);
        transitionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(transition);

        log.info("Manual stage override: user {} -> {} (reason: {})", userId, stage, reason);

        return transition;
    }

    /**
     * Get count of users in each stage (for dashboard metrics).
     */
    public Map<String, Integer> getStageDistribution()
    {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (UserStage s : UserStage.values())
        {
            counts.put(s.getValue(), 0);
        }
        for (UserStage stage : userStages.values())
        {
            counts.put(stage.getValue(), counts.get(stage.getValue()) + 1);
        }
        return counts;
    }

    /**
     * Reset all in-memory storage (for testing).
     */
    public void resetAll()
    {
        userStages.clear();
        transitionHistory.clear();
    }

    // ========== Internal Helpers ==========

    /**
     * Infer which trigger caused the stage transition.
     */
    private TransitionTrigger inferTrigger(UserTrainingData data, UserStage fromStage,
                                           UserStage toStage, StageThresholds thresholds,
                                           LocalDateTime now)
    {
        if (toStage == UserStage.PLATEAU)
        {
            if (data.getLastSessionDate() != null)
            {
                long daysInactive = ChronoUnit.DAYS.between(data.getLastSessionDate(), now);
                if (daysInactive >= thresholds.getPlateauInactivityDays())
                {
                    return TransitionTrigger.INACTIVITY_DAYS;
                }
            }
            return TransitionTrigger.INTENSITY_GROWTH;
        }

        if (toStage == UserStage.ADVANCED)
        {
            return TransitionTrigger.TENURE_WEEKS;
        }

        if (toStage == UserStage.GROWTH)
        {
            if (fromStage == UserStage.BEGINNER)
            {
                return TransitionTrigger.COMPLETION_RATE;
            }
            return TransitionTrigger.FREQUENCY_THRESHOLD;
        }

        if (toStage == UserStage.BEGINNER)
        {
            return TransitionTrigger.FREQUENCY_THRESHOLD;
        }

        return TransitionTrigger.MANUAL_OVERRIDE;
    }

    /**
     * Build the evidence map from user training data.
     */
    private Map<String, Object> buildEvidenceMap(UserTrainingData data)
    {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("tenure_weeks", data.getTenureWeeks());
        evidence.put("weekly_frequency", data.getWeeklyFrequency());
        evidence.put("completion_rate", data.getCompletionRate());
        evidence.put("intensity_trend", data.getIntensityTrend());
        evidence.put("last_session_date",
            data.getLastSessionDate() != null ? data.getLastSessionDate().toString() : null);
        return evidence;
    }
}
