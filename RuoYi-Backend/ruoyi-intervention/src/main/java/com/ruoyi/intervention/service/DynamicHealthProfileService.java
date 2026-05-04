package com.ruoyi.intervention.service;

import com.ruoyi.intervention.domain.enums.*;
import com.ruoyi.intervention.domain.model.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dynamic Health Profile Service — Time-series modeling, multimodal fusion, and risk prediction.
 *
 * Phase 2 upgrade from the static HealthProfile model:
 *   - Sliding time-series windows for trend and anomaly detection
 *   - Multimodal fusion scoring across exercise, sleep, HR, nutrition dimensions
 *   - Profile versioning and comparison
 *   - Health risk prediction and early warning
 *
 * Reference:
 *   - Klasnja et al. 2015 JITAI; Nahum-Shani et al. 2018 JITAI design
 *   - WHO 2020 Physical Activity Guidelines
 *   - Hirshkowitz 2015 NSF Sleep Duration Recommendations
 *   - ACSM 2021 Ch.2 risk stratification, Ch.4 fitness assessment
 *   - Tanaka 2001 HRmax; Karvonen 1957 HR reserve
 *   - Buysse 1989 PSQI; Bomberg 2017 progressive overload
 */
public class DynamicHealthProfileService {

    // --- Module-Level Configuration Constants (循证阈值) ---

    private static final int WINDOW_SIZE_DAYS = 7;
    private static final int MIN_WINDOW_POINTS = 3;
    private static final double TREND_SIGNIFICANCE_THRESHOLD = 0.15; // 15% change
    private static final double ANOMALY_ZSCORE_THRESHOLD = 2.0;

    private static final double EXERCISE_MIN_WEEKLY_MINUTES = 150.0;
    private static final double EXERCISE_OPTIMAL_WEEKLY_MINUTES = 300.0;

    private static final double SLEEP_OPTIMAL_MIN = 7.0;
    private static final double SLEEP_OPTIMAL_MAX = 9.0;

    private static final int HR_RESTING_EXCELLENT = 60;
    private static final int HR_RESTING_GOOD = 70;
    private static final int HR_RESTING_FAIR = 80;
    private static final double HR_VARIABILITY_EXCELLENT = 60.0;

    private static final double NUTRITION_WATER_LITER = 2.0;

    private static final double RISK_SCORE_HIGH = 70.0;
    private static final double RISK_SCORE_MODERATE = 50.0;

    // Multimodal fusion weights (心血管是全因死亡率的主要预测因子)
    private static final Map<String, Double> FUSION_WEIGHTS = Map.of(
        "exercise", 0.30,
        "sleep", 0.25,
        "heart_rate", 0.25,
        "nutrition", 0.20
    );

    // Grade labels
    private static final Map<String, String> GRADE_LABELS_ZH = Map.of(
        "excellent", "优秀",
        "good", "良好",
        "fair", "一般",
        "poor", "较差"
    );

    // --- In-Memory Storage ---

    private static final ConcurrentHashMap<String, UserProfileState> PROFILE_STORE = new ConcurrentHashMap<>();

    private static class UserProfileState {
        String userId;
        HealthProfile staticProfile;
        ConcurrentHashMap<String, List<TimeSeriesPoint>> timeSeries = new ConcurrentHashMap<>();
        MultimodalDataPoint latestData;
        MultimodalFusionReport cachedFusionReport;
        boolean fusionReportStale = true;
        int versionCount = 0;
        ConcurrentHashMap<String, TrendAnalysis> cachedTrends = new ConcurrentHashMap<>();
        boolean trendsStale = true;
        List<ProfileVersion> versions = new ArrayList<>();
    }

    // --- Singleton Access ---

    private static final DynamicHealthProfileService INSTANCE = new DynamicHealthProfileService();

    public static DynamicHealthProfileService getInstance() { return INSTANCE; }

    private DynamicHealthProfileService() {}

    // --- Profile Lifecycle ---

    public UserProfileState getOrCreate(String userId) {
        return PROFILE_STORE.computeIfAbsent(userId, k -> {
            UserProfileState s = new UserProfileState();
            s.userId = userId;
            return s;
        });
    }

    public UserProfileState get(String userId) {
        return PROFILE_STORE.get(userId);
    }

    public void delete(String userId) {
        PROFILE_STORE.remove(userId);
    }

    public Set<String> listUsers() {
        return Set.copyOf(PROFILE_STORE.keySet());
    }

    // --- Data Ingestion ---

    public void ingestMultimodalDataPoint(MultimodalDataPoint point) {
        UserProfileState profile = getOrCreate(point.userId());
        profile.latestData = point;
        profile.fusionReportStale = true;
        profile.trendsStale = true;

        // Add individual metric time-series points
        addTsPoint(profile, "exercise_minutes", point.timestamp(), point.exerciseMinutes());
        addTsPoint(profile, "steps", point.timestamp(), point.steps() != null ? (double) point.steps() : null);
        addTsPoint(profile, "sleep_hours", point.timestamp(), point.sleepDurationHours());
        addTsPoint(profile, "resting_hr", point.timestamp(), point.restingHr() != null ? (double) point.restingHr() : null);
        addTsPoint(profile, "hrv", point.timestamp(), point.hrvRmssd());
    }

    private void addTsPoint(UserProfileState profile, String metric, LocalDateTime ts, Double value) {
        if (value == null) return;
        profile.timeSeries.computeIfAbsent(metric, k -> new ArrayList<>())
            .add(new TimeSeriesPoint(metric, value, ts));
        // Prune old points beyond 2x window
        LocalDateTime cutoff = LocalDateTime.now().minusDays(WINDOW_SIZE_DAYS * 2L);
        profile.timeSeries.get(metric).removeIf(p -> p.timestamp().isBefore(cutoff));
    }

    // --- Time-Series Analysis ---

    public List<TimeSeriesPoint> getTimeSeries(String userId, String metric, Integer windowDays) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        if (profile == null || !profile.timeSeries.containsKey(metric)) return List.of();
        int window = windowDays != null ? windowDays : WINDOW_SIZE_DAYS;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(window);
        return profile.timeSeries.get(metric).stream()
            .filter(p -> !p.timestamp().isBefore(cutoff))
            .collect(Collectors.toList());
    }

    public Map<String, TrendAnalysis> analyzeTrends(String userId) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        if (profile == null) return Map.of();
        if (!profile.trendsStale) return Map.copyOf(profile.cachedTrends);

        Map<String, TrendAnalysis> results = new HashMap<>();
        for (Map.Entry<String, List<TimeSeriesPoint>> e : profile.timeSeries.entrySet()) {
            String metric = e.getKey();
            List<TimeSeriesPoint> pts = e.getValue();
            TrendAnalysis ta = computeTrendAnalysis(metric, pts);
            results.put(metric, ta);
        }
        profile.cachedTrends = new ConcurrentHashMap<>(results);
        profile.trendsStale = false;
        return results;
    }

    private TrendAnalysis computeTrendAnalysis(String metric, List<TimeSeriesPoint> pts) {
        if (pts == null || pts.size() < MIN_WINDOW_POINTS) {
            return new TrendAnalysis(
                metric, TrendType.INSUFFICIENT_DATA, 0.0, 0.0,
                pts != null && !pts.isEmpty() ? pts.get(0).value() : 0.0,
                pts != null && !pts.isEmpty() ? pts.get(pts.size() - 1).value() : 0.0,
                pts != null ? pts.size() : 0,
                0.0, "nahum_shani_2018_jitai_design"
            );
        }

        // Sliding window filter
        LocalDateTime cutoff = LocalDateTime.now().minusDays(WINDOW_SIZE_DAYS);
        List<TimeSeriesPoint> window = pts.stream()
            .filter(p -> !p.timestamp().isBefore(cutoff))
            .collect(Collectors.toList());

        if (window.size() < MIN_WINDOW_POINTS) {
            double start = window.isEmpty() ? 0.0 : window.get(0).value();
            double end = window.isEmpty() ? 0.0 : window.get(window.size() - 1).value();
            return new TrendAnalysis(metric, TrendType.INSUFFICIENT_DATA, 0.0,
                start, end, window.size(), "nahum_shani_2018_jitai_design");
        }

        double[] result = linearTrend(window);
        double slope = result[0];
        double rSquared = result[1];

        double startVal = window.get(0).value();
        double endVal = window.get(window.size() - 1).value();
        double changeFrac = startVal != 0.0 ? (endVal - startVal) / startVal : 0.0;

        TrendType trend;
        if (Math.abs(changeFrac) < TREND_SIGNIFICANCE_THRESHOLD) {
            trend = TrendType.STABLE;
        } else if (changeFrac > 0) {
            trend = TrendType.IMPROVING;
        } else {
            trend = TrendType.DECLINING;
        }

        return new TrendAnalysis(
            metric, trend, round(slope, 4), round(changeFrac, 4),
            round(startVal, 2), round(endVal, 2),
            window.size(), round(rSquared, 3),
            "nahum_shani_2018_jitai_design"
        );
    }

    private double[] linearTrend(List<TimeSeriesPoint> pts) {
        // Returns [slope, rSquared]
        if (pts.size() < 2) return new double[]{0.0, 0.0};
        double n = pts.size();
        double x0 = pts.get(0).timestamp().toLocalTime().toSecondOfDay() / 86400.0
            + pts.get(0).timestamp().toLocalDate().toEpochDay();

        List<Double> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (TimeSeriesPoint p : pts) {
            double days = ChronoUnit.SECONDS.between(
                pts.get(0).timestamp(), p.timestamp()) / 86400.0;
            times.add(days);
            values.add(p.value());
        }

        double xMean = times.stream().mapToDouble(d -> d).sum() / n;
        double yMean = values.stream().mapToDouble(v -> v).sum() / n;

        double num = 0.0, denom = 0.0, ssTot = 0.0, ssRes = 0.0;
        for (int i = 0; i < n; i++) {
            double xi = times.get(i) - x0;
            double yi = values.get(i);
            num += xi * (yi - yMean);
            denom += xi * xi;
            ssTot += (yi - yMean) * (yi - yMean);
        }

        if (denom == 0) return new double[]{0.0, 0.0};
        double slope = num / denom;
        for (int i = 0; i < n; i++) {
            double predicted = yMean + slope * (times.get(i) - x0);
            ssRes += (values.get(i) - predicted) * (values.get(i) - predicted);
        }
        double rSquared = ssTot > 0 ? 1.0 - (ssRes / ssTot) : 0.0;
        return new double[]{slope, Math.max(0.0, rSquared)};
    }

    // --- Anomaly Detection ---

    public List<AnomalyDetectionResult> detectAnomalies(String userId, String metric, Double zThreshold) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        if (profile == null) return List.of();
        double zThresh = zThreshold != null ? zThreshold : ANOMALY_ZSCORE_THRESHOLD;
        List<TimeSeriesPoint> pts = getTimeSeries(userId, metric, null);
        return detectAnomaliesInWindow(pts, zThresh);
    }

    private List<AnomalyDetectionResult> detectAnomaliesInWindow(List<TimeSeriesPoint> pts, double zThresh) {
        List<AnomalyDetectionResult> results = new ArrayList<>();
        if (pts == null || pts.size() < 3) return results;

        double[] values = pts.stream().mapToDouble(TimeSeriesPoint::value).toArray();

        // Global stats for expected_value reporting
        double globalMu = mean(values);
        double globalSigma = stdev(values);
        if (globalSigma == 0) return results;

        for (int i = 0; i < pts.size(); i++) {
            // Leave-one-out statistics
            double[] otherValues = new double[values.length - 1];
            int idx = 0;
            for (int j = 0; j < values.length; j++) {
                if (j != i) otherValues[idx++] = values[j];
            }
            double looMu = mean(otherValues);
            double looSigma = stdev(otherValues);
            if (looSigma == 0) {
                if (pts.get(i).value() != looMu) {
                    // All other points identical — check if this differs
                    addAnomalyResult(results, pts, i, looMu, Double.POSITIVE_INFINITY, globalMu, globalSigma, zThresh);
                }
                continue;
            }
            double z = Math.abs((pts.get(i).value() - looMu) / looSigma);
            if (z >= zThresh) {
                addAnomalyResult(results, pts, i, looMu, z, globalMu, globalSigma, zThresh);
            }
        }
        results.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        return results;
    }

    private void addAnomalyResult(List<AnomalyDetectionResult> results, List<TimeSeriesPoint> pts,
            int i, double looMu, double z, double globalMu, double globalSigma, double zThresh) {
        TimeSeriesPoint p = pts.get(i);
        String severity = z >= 3.0 ? "severe" : z >= 2.5 ? "moderate" : "mild";
        AnomalyType type = AnomalyType.OUTLIER;

        if (i > 0 && i < pts.size() - 1) {
            double prevVal = pts.get(i - 1).value();
            double nextVal = pts.get(i + 1).value();
            if (p.value() > prevVal && p.value() > nextVal) {
                type = AnomalyType.SPIKE;
            } else if (p.value() < prevVal && p.value() < nextVal) {
                type = AnomalyType.DROP;
            }
        }

        results.add(new AnomalyDetectionResult(
            p.timestamp(), p.metric(), type,
            round(p.value(), 2), round(looMu, 2),
            z == Double.POSITIVE_INFINITY ? 999.99 : round(z, 2),
            severity, "nahum_shani_2018_jitai_design"
        ));
    }

    // --- Multimodal Fusion Scoring ---

    public MultimodalFusionReport computeMultimodalScore(String userId) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        if (profile == null || profile.latestData == null) return null;
        if (!profile.fusionReportStale && profile.cachedFusionReport != null) {
            return profile.cachedFusionReport;
        }

        MultimodalDataPoint data = profile.latestData;
        DimensionFusionScore ex = scoreExerciseDimension(data);
        DimensionFusionScore sl = scoreSleepDimension(data);
        DimensionFusionScore hr = scoreHrDimension(data);
        DimensionFusionScore nu = scoreNutritionDimension(data);

        double composite = (
            ex.rawScore() * FUSION_WEIGHTS.get("exercise") +
            sl.rawScore() * FUSION_WEIGHTS.get("sleep") +
            hr.rawScore() * FUSION_WEIGHTS.get("heart_rate") +
            nu.rawScore() * FUSION_WEIGHTS.get("nutrition")
        );

        String compositeGrade = assignGrade(composite);

        MultimodalFusionReport report = new MultimodalFusionReport(
            userId, LocalDateTime.now(),
            ex, sl, hr, nu,
            round(composite, 1), compositeGrade,
            GRADE_LABELS_ZH.get(compositeGrade),
            "1.0",
            List.of("klasnja_2015_jitai", "nahum_shani_2018_jitai_design",
                "who_2020_pa", "hirshkowitz_2015", "aasm_2020",
                "tanaka_2001_hrmax", "karvonen_1957", "acsm_2021_ch4")
        );

        profile.cachedFusionReport = report;
        profile.fusionReportStale = false;
        return report;
    }

    public double getCompositeScore(String userId) {
        MultimodalFusionReport r = computeMultimodalScore(userId);
        if (r != null) return r.compositeScore();
        UserProfileState p = PROFILE_STORE.get(userId);
        if (p != null && p.staticProfile != null) {
            return p.staticProfile.calculateOverallScore();
        }
        return 50.0;
    }

    private DimensionFusionScore scoreExerciseDimension(MultimodalDataPoint data) {
        Map<String, Double> subScores = new LinkedHashMap<>();

        // Exercise minutes score
        if (data.exerciseMinutes() != null) {
            double exMin = Math.min(data.exerciseMinutes(), EXERCISE_OPTIMAL_WEEKLY_MINUTES);
            double exScore = Math.min(100.0, (exMin / EXERCISE_MIN_WEEKLY_MINUTES) * 60.0);
            if (data.exerciseMinutes() >= EXERCISE_MIN_WEEKLY_MINUTES) {
                double excess = Math.min(
                    data.exerciseMinutes() - EXERCISE_MIN_WEEKLY_MINUTES,
                    EXERCISE_OPTIMAL_WEEKLY_MINUTES - EXERCISE_MIN_WEEKLY_MINUTES
                );
                exScore = Math.min(100.0, exScore + (excess / EXERCISE_OPTIMAL_WEEKLY_MINUTES) * 40.0);
            }
            subScores.put("exercise_minutes", round(exScore, 1));
        }

        // Intensity score (RPE 0-10 -> 0-100)
        if (data.exerciseIntensityAvg() != null) {
            double rpe = data.exerciseIntensityAvg();
            double intScore;
            if (rpe >= 3.0 && rpe <= 6.0) {
                intScore = 80.0 + (Math.min(Math.abs(rpe - 4.5), 1.5) / 1.5) * 20.0;
            } else if (rpe < 3.0) {
                intScore = 30.0 + (rpe / 3.0) * 50.0;
            } else {
                intScore = Math.max(50.0, 100.0 - (rpe - 6.0) * 12.5);
            }
            subScores.put("exercise_intensity", round(intScore, 1));
        }

        // Steps score
        if (data.steps() != null) {
            subScores.put("steps", round(Math.min(100.0, (data.steps() / 10000.0) * 100.0), 1));
        }

        double raw = subScores.isEmpty() ? 50.0
            : weightedMean(subScores, Map.of("exercise_minutes", 0.50, "exercise_intensity", 0.20, "steps", 0.30));

        String grade = assignGrade(raw);
        return new DimensionFusionScore("exercise", round(raw, 1), grade,
            GRADE_LABELS_ZH.get(grade), subScores, "who_2020_pa");
    }

    private DimensionFusionScore scoreSleepDimension(MultimodalDataPoint data) {
        Map<String, Double> subScores = new LinkedHashMap<>();

        if (data.sleepDurationHours() != null) {
            double dur = data.sleepDurationHours();
            double durScore;
            if (dur >= SLEEP_OPTIMAL_MIN && dur <= SLEEP_OPTIMAL_MAX) {
                durScore = 100.0;
            } else if (dur < SLEEP_OPTIMAL_MIN) {
                durScore = Math.max(0.0, (dur / SLEEP_OPTIMAL_MIN) * 100.0);
            } else {
                double excess = dur - SLEEP_OPTIMAL_MAX;
                durScore = Math.max(20.0, 100.0 - (excess * 15.0));
            }
            subScores.put("sleep_duration", round(durScore, 1));
        }

        if (data.sleepQualityScore() != null) {
            subScores.put("sleep_quality", round(Math.max(0.0, Math.min(100.0, data.sleepQualityScore())), 1));
        }

        double raw = subScores.isEmpty() ? 50.0 : mean(subScores.values().stream().mapToDouble(Double::doubleValue).toArray());
        String grade = assignGrade(raw);
        return new DimensionFusionScore("sleep", round(raw, 1), grade,
            GRADE_LABELS_ZH.get(grade), subScores, "hirshkowitz_2015");
    }

    private DimensionFusionScore scoreHrDimension(MultimodalDataPoint data) {
        Map<String, Double> subScores = new LinkedHashMap<>();

        if (data.restingHr() != null) {
            int hr = data.restingHr();
            double hrScore;
            if (hr <= HR_RESTING_EXCELLENT) {
                hrScore = 100.0;
            } else if (hr <= HR_RESTING_GOOD) {
                hrScore = 100.0 - ((hr - HR_RESTING_EXCELLENT) / 10.0) * 25.0;
            } else if (hr <= HR_RESTING_FAIR) {
                hrScore = 75.0 - ((hr - HR_RESTING_GOOD) / 10.0) * 25.0;
            } else {
                hrScore = Math.max(0.0, 50.0 - ((hr - HR_RESTING_FAIR) * 2.5));
            }
            subScores.put("resting_hr", round(Math.max(0.0, hrScore), 1));
        }

        if (data.hrvRmssd() != null) {
            double hrv = data.hrvRmssd();
            double hrvScore;
            if (hrv >= HR_VARIABILITY_EXCELLENT) {
                hrvScore = 100.0;
            } else if (hrv >= 30.0) {
                hrvScore = 75.0 + ((hrv - 30.0) / 30.0) * 25.0;
            } else if (hrv >= 20.0) {
                hrvScore = 50.0 + ((hrv - 20.0) / 10.0) * 25.0;
            } else {
                hrvScore = Math.max(0.0, (hrv / 20.0) * 50.0);
            }
            subScores.put("hrv", round(hrvScore, 1));
        }

        double raw = subScores.isEmpty() ? 50.0
            : mean(subScores.values().stream().mapToDouble(Double::doubleValue).toArray());
        String grade = assignGrade(raw);
        return new DimensionFusionScore("heart_rate", round(raw, 1), grade,
            GRADE_LABELS_ZH.get(grade), subScores, "tanaka_2001_hrmax");
    }

    private DimensionFusionScore scoreNutritionDimension(MultimodalDataPoint data) {
        Map<String, Double> subScores = new LinkedHashMap<>();

        // Protein score
        if (data.proteinG() != null) {
            double pg = data.proteinG();
            double proteinScore;
            if (pg <= 30.0) {
                proteinScore = Math.max(0.0, (pg / 30.0) * 20.0);
            } else if (pg <= 56.0) {
                proteinScore = 20.0 + ((pg - 30.0) / 26.0) * 40.0;
            } else if (pg <= 90.0) {
                proteinScore = 60.0 + ((pg - 56.0) / 34.0) * 30.0;
            } else {
                proteinScore = Math.min(100.0, 90.0 + ((Math.min(pg, 120.0) - 90.0) / 30.0) * 10.0);
            }
            subScores.put("protein", round(proteinScore, 1));
        }

        // Hydration score
        if (data.waterLiters() != null) {
            double wl = data.waterLiters();
            double waterScore;
            if (wl >= NUTRITION_WATER_LITER) {
                waterScore = Math.min(100.0, 80.0 + ((wl - NUTRITION_WATER_LITER) / 1.0) * 20.0);
            } else {
                waterScore = Math.max(0.0, (wl / NUTRITION_WATER_LITER) * 80.0);
            }
            subScores.put("hydration", round(waterScore, 1));
        }

        // Caloric balance
        if (data.caloriesConsumed() != null) {
            double calRatio = data.caloriesConsumed() / 2000.0;
            double calScore;
            if (calRatio >= 0.8 && calRatio <= 1.2) {
                calScore = 100.0 - Math.abs(calRatio - 1.0) * 50.0;
            } else if (calRatio < 0.8) {
                calScore = Math.max(0.0, 60.0 - (0.8 - calRatio) * 100.0);
            } else {
                calScore = Math.max(0.0, 100.0 - (calRatio - 1.2) * 100.0);
            }
            subScores.put("caloric_balance", round(calScore, 1));
        }

        double raw = subScores.isEmpty() ? 50.0
            : mean(subScores.values().stream().mapToDouble(Double::doubleValue).toArray());
        String grade = assignGrade(raw);
        return new DimensionFusionScore("nutrition", round(raw, 1), grade,
            GRADE_LABELS_ZH.get(grade), subScores, "who_2020_pa");
    }

    // --- Risk Prediction ---

    public RiskPrediction predictRisk(String userId) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        MultimodalFusionReport fusion = computeMultimodalScore(userId);
        Map<String, TrendType> trends = new HashMap<>();
        if (profile != null) {
            for (Map.Entry<String, TrendAnalysis> e : analyzeTrends(userId).entrySet()) {
                trends.put(e.getKey(), e.getValue().trend());
            }
        }

        if (fusion == null) {
            return new RiskPrediction(
                userId, LocalDateTime.now(), 50.0, RiskLevel.MODERATE,
                null, TrendType.INSUFFICIENT_DATA,
                List.of("No health data available"),
                false, null, 0.0, "acsm_2021_ch2"
            );
        }

        double riskScore = 100.0 - fusion.compositeScore();
        List<String> contributing = new ArrayList<>();

        // Add risk from static risk factors
        if (profile != null && profile.staticProfile != null && profile.staticProfile.getRiskFactors() != null) {
            int nRf = profile.staticProfile.getRiskFactors().countRiskFactors();
            riskScore += nRf * 3.0;
            if (nRf > 0) contributing.add(nRf + " ACSM risk factor(s)");
        }

        // Add risk from declining trends
        List<String> decliningDims = trends.entrySet().stream()
            .filter(e -> e.getValue() == TrendType.DECLINING)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        if (!decliningDims.isEmpty()) {
            riskScore += decliningDims.size() * 2.5;
            contributing.add("Declining trend in: " + String.join(", ", decliningDims));
        }

        riskScore = Math.min(100.0, Math.max(0.0, riskScore));
        RiskLevel level = riskScore >= RISK_SCORE_HIGH ? RiskLevel.HIGH
            : riskScore >= RISK_SCORE_MODERATE ? RiskLevel.MODERATE : RiskLevel.LOW;

        // 30-day prediction
        long decliningCount = trends.values().stream().filter(t -> t == TrendType.DECLINING).count();
        long improvingCount = trends.values().stream().filter(t -> t == TrendType.IMPROVING).count();
        TrendType trajectory;
        Double predicted30d = null;

        if (decliningCount > improvingCount) {
            trajectory = TrendType.DECLINING;
            double delta = decliningCount * 1.0;
            predicted30d = round(Math.min(100.0, riskScore + (delta * 30)), 1);
        } else if (improvingCount > decliningCount) {
            trajectory = TrendType.IMPROVING;
            double delta = improvingCount * 0.5;
            predicted30d = round(Math.max(0.0, riskScore - (delta * 30)), 1);
        } else {
            trajectory = TrendType.STABLE;
        }

        boolean escalation = (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL)
            || (trajectory == TrendType.DECLINING && predicted30d != null && predicted30d >= RISK_SCORE_HIGH);

        String escalationReason = null;
        if (escalation) {
            if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL) {
                escalationReason = "Current risk level is " + level.name().toLowerCase() + " (score=" + riskScore + ")";
            } else {
                escalationReason = "Risk trajectory declining: 30d predicted score=" + predicted30d;
            }
        }

        int totalPoints = profile != null
            ? profile.timeSeries.values().stream().mapToInt(List::size).sum() : 0;
        double confidence = Math.min(0.9, 0.3 + (totalPoints * 0.05));

        return new RiskPrediction(
            userId, LocalDateTime.now(), round(riskScore, 1), level,
            predicted30d, trajectory, contributing,
            escalation, escalationReason, round(confidence, 2), "acsm_2021_ch2"
        );
    }

    // --- Profile Versioning ---

    public ProfileVersion createVersion(String userId, List<String> changelog) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        if (profile == null) throw new IllegalArgumentException("Profile not found: " + userId);

        profile.versionCount++;
        MultimodalFusionReport fusion = computeMultimodalScore(userId);
        double overall = getCompositeScore(userId);
        TrendType overallTrend = getOverallTrend(userId);

        Integer restingHr = profile.latestData != null ? profile.latestData.restingHr() : null;
        Integer steps = profile.latestData != null ? profile.latestData.steps() : null;
        Double sleepHours = profile.latestData != null ? profile.latestData.sleepDurationHours() : null;

        RiskPrediction riskPred = predictRisk(userId);

        ProfileVersion version = new ProfileVersion(
            "v" + userId + "_" + profile.versionCount + "_" + System.currentTimeMillis(),
            userId, profile.versionCount, LocalDateTime.now(),
            round(overall, 1),
            fusion != null ? round(fusion.compositeScore(), 1) : null,
            riskPred.currentRiskLevel(),
            overallTrend,
            restingHr, steps, sleepHours,
            changelog != null ? changelog : List.of(),
            "nahum_shani_2018_jitai_design"
        );

        profile.versions.add(version);
        return version;
    }

    public List<ProfileVersion> getVersionHistory(String userId) {
        UserProfileState profile = PROFILE_STORE.get(userId);
        if (profile == null) return List.of();
        return profile.versions.stream()
            .sorted((a, b) -> Integer.compare(b.versionNumber(), a.versionNumber()))
            .collect(Collectors.toList());
    }

    public ProfileVersion getVersion(String userId, int versionNumber) {
        return getVersionHistory(userId).stream()
            .filter(v -> v.versionNumber() == versionNumber)
            .findFirst().orElse(null);
    }

    public Map<String, Object> compareVersions(String userId, int v1Number, int v2Number) {
        ProfileVersion v1 = getVersion(userId, v1Number);
        ProfileVersion v2 = getVersion(userId, v2Number);
        if (v1 == null || v2 == null) {
            return Map.of("error", "One or both versions not found");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score_delta", round(v2.overallScore() - v1.overallScore(), 1));
        if (v1.compositeFusionScore() != null && v2.compositeFusionScore() != null) {
            result.put("composite_delta", round(v2.compositeFusionScore() - v1.compositeFusionScore(), 1));
        }
        result.put("risk_level_change", !v1.riskLevel().equals(v2.riskLevel())
            ? v1.riskLevel().name().toLowerCase() + " -> " + v2.riskLevel().name().toLowerCase()
            : v1.riskLevel().name().toLowerCase());
        result.put("trend_change", !v1.overallTrend().equals(v2.overallTrend())
            ? v1.overallTrend().name().toLowerCase() + " -> " + v2.overallTrend().name().toLowerCase()
            : v1.overallTrend().name().toLowerCase());
        if (v1.restingHrSnapshot() != null && v2.restingHrSnapshot() != null) {
            result.put("resting_hr_delta", v2.restingHrSnapshot() - v1.restingHrSnapshot());
        }
        if (v1.stepsSnapshot() != null && v2.stepsSnapshot() != null) {
            result.put("steps_delta", v2.stepsSnapshot() - v1.stepsSnapshot());
        }
        if (v1.sleepHoursSnapshot() != null && v2.sleepHoursSnapshot() != null) {
            result.put("sleep_delta", round(v2.sleepHoursSnapshot() - v1.sleepHoursSnapshot(), 1));
        }
        return result;
    }

    // --- Overall Trend ---

    public TrendType getOverallTrend(String userId) {
        Map<String, TrendAnalysis> trends = analyzeTrends(userId);
        if (trends.isEmpty()) return TrendType.INSUFFICIENT_DATA;

        List<TrendType> valid = trends.values().stream()
            .filter(t -> t.trend() != TrendType.INSUFFICIENT_DATA)
            .map(TrendAnalysis::trend)
            .collect(Collectors.toList());

        if (valid.isEmpty()) return TrendType.INSUFFICIENT_DATA;

        long improving = valid.stream().filter(t -> t == TrendType.IMPROVING).count();
        long declining = valid.stream().filter(t -> t == TrendType.DECLINING).count();

        if (improving > declining) return TrendType.IMPROVING;
        if (declining > improving) return TrendType.DECLINING;
        return TrendType.STABLE;
    }

    // --- Grade Assignment ---

    private String assignGrade(double score) {
        if (score >= 85.0) return "excellent";
        if (score >= 70.0) return "good";
        if (score >= 50.0) return "fair";
        return "poor";
    }

    // --- Math Utilities ---

    private double mean(double[] arr) {
        if (arr == null || arr.length == 0) return 0.0;
        return Arrays.stream(arr).sum() / arr.length;
    }

    private double stdev(double[] arr) {
        if (arr == null || arr.length < 2) return 0.0;
        double mu = mean(arr);
        double var = Arrays.stream(arr).map(v -> (v - mu) * (v - mu)).sum() / (arr.length - 1);
        return Math.sqrt(Math.max(0.0, var));
    }

    private double weightedMean(Map<String, Double> scores, Map<String, Double> weights) {
        double totalWeight = 0.0, weightedSum = 0.0;
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            Double w = weights.get(e.getKey());
            if (w != null) {
                weightedSum += e.getValue() * w;
                totalWeight += w;
            }
        }
        return totalWeight > 0 ? weightedSum / totalWeight : 50.0;
    }

    private double round(double value, int decimals) {
        double mult = Math.pow(10, decimals);
        return Math.round(value * mult) / mult;
    }
}
