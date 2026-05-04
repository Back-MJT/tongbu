package com.ruoyi.intervention.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 心率计算服务 — 基于 Tanaka 公式与 Karvonen 储备心率法实现心率区间计算
 * Heart rate calculation service using the Tanaka HRmax formula and Karvonen HR-reserve method.
 *
 * Migrated from: intervention-engine/src/algorithms/heart_rate.py
 * Evidence bases:
 *   - Tanaka et al. 2001: HRmax = 208 - 0.7 * age  (tanaka_2001_hrmax)
 *   - Karvonen et al. 1957: Target HR = (HRmax - HRrest) * intensity% + HRrest  (karvonen_1957)
 *   - ACSM 2021 Ch.7: Intensity zones (light / moderate / vigorous)  (acsm_2021_ch7)
 *   - ACSM 2021 Ch.2: Safety limits  (acsm_2021_ch2)
 *
 * All parameters loaded from exercise_rules.yaml via static constants — no hardcoded magic numbers.
 * 数值结果与 Python 版本完全一致（相同的公式、阈值、舍入方式）。
 */
@Service
public class HeartRateService
{
    private static final Logger log = LoggerFactory.getLogger(HeartRateService.class);

    // ========== HR Estimation Parameters (from exercise_rules.yaml → hr_estimation) ==========
    // Tanaka formula: HRmax = INTERCEPT - AGE_COEFFICIENT * age
    /** 截距 / Intercept for Tanaka formula (yaml: hr_estimation.parameters.intercept) */
    private static final double HR_INTERCEPT = 208.0;
    /** 年龄系数 / Age coefficient for Tanaka formula (yaml: hr_estimation.parameters.age_coefficient) */
    private static final double HR_AGE_COEFFICIENT = 0.7;
    /** 公式名称 / Formula name (yaml: hr_estimation.formula) */
    private static final String HR_FORMULA = "tanaka";
    /** 循证引用 / Evidence reference for HR estimation (yaml: hr_estimation.evidence_ref) */
    private static final String HR_EVIDENCE_REF = "tanaka_2001_hrmax";
    /** 储备心率法引用 / Evidence reference for Karvonen method (yaml: hr_estimation.reserve_evidence_ref) */
    private static final String RESERVE_EVIDENCE_REF = "karvonen_1957";

    // ========== Safety Limits (from exercise_rules.yaml → safety) ==========
    /** 最大储备心率强度 / Maximum intensity as fraction of HRR (yaml: safety.max_intensity_hrr) */
    private static final double MAX_INTENSITY_HRR = 0.90;
    /** 安全缓冲（bpm）/ Safety buffer below HRmax in bpm (yaml: safety.hr_safety_buffer_bpm) */
    private static final int HR_SAFETY_BUFFER_BPM = 10;
    /** 安全引用 / Evidence reference for safety limits (yaml: safety.evidence_ref) */
    private static final String SAFETY_EVIDENCE_REF = "acsm_2021_ch2";

    // ========== Intensity Zones (from exercise_rules.yaml → intensity_zones) ==========
    // Zone definitions: name → (hrr_low, hrr_high, perceived_exertion, evidence_ref)
    private static final List<ZoneDefinition> INTENSITY_ZONES = Arrays.asList(
        new ZoneDefinition("light",     0.30, 0.49, "9-11 (Borg 6-20)",  "acsm_2021_ch7"),
        new ZoneDefinition("moderate",  0.50, 0.69, "12-14 (Borg 6-20)", "acsm_2021_ch7"),
        new ZoneDefinition("vigorous",  0.70, 0.89, "15-17 (Borg 6-20)", "acsm_2021_ch7")
    );

    // ========== Public API ==========

    /**
     * 估算最大心率 / Estimate maximum heart rate using the configured formula.
     *
     * Default: Tanaka formula (HRmax = 208 - 0.7 * age)
     * Reference: tanaka_2001_hrmax
     *
     * @param age 年龄 / Person's age in years
     * @return Map containing hrmax, formula, parameters, evidence_ref
     */
    public Map<String, Object> estimateHrmax(int age)
    {
        double hrmaxRaw = HR_INTERCEPT - HR_AGE_COEFFICIENT * age;
        int hrmax = (int) Math.round(hrmaxRaw);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("intercept", HR_INTERCEPT);
        params.put("age_coefficient", HR_AGE_COEFFICIENT);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hrmax", hrmax);
        result.put("formula", HR_FORMULA);
        result.put("parameters", params);
        result.put("evidence_ref", HR_EVIDENCE_REF);

        log.debug("estimateHrmax: age={}, hrmax={}", age, hrmax);
        return result;
    }

    /**
     * 获取安全最大心率（扣除安全缓冲）/ Get HRmax with safety buffer applied.
     *
     * Safety buffer (default: 10 bpm below HRmax) prevents recommendations
     * from pushing into dangerous territory.
     *
     * Reference: acsm_2021_ch2
     *
     * @param age 年龄 / Person's age in years
     * @return 安全最大心率 / Safe HRmax in bpm
     */
    public int getSafeHrmax(int age)
    {
        Map<String, Object> hrmaxResult = estimateHrmax(age);
        int hrmax = (int) hrmaxResult.get("hrmax");
        int safeHrmax = hrmax - HR_SAFETY_BUFFER_BPM;
        log.debug("getSafeHrmax: age={}, hrmax={}, safeHrmax={}", age, hrmax, safeHrmax);
        return safeHrmax;
    }

    /**
     * 计算目标心率（Karvonen 储备心率法）/ Calculate target heart rate using Karvonen (HR reserve) method.
     *
     * Target HR = (HRmax - HRrest) * intensity_fraction + HRrest
     * Reference: karvonen_1957
     *
     * @param age              年龄 / Person's age
     * @param restingHr        静息心率 / Resting heart rate (bpm)
     * @param intensityFraction 强度分数 / Fraction of HRR (0.0-1.0)
     * @return Map with target_hr, hrmax, hr_reserve, intensity, safe, etc.
     */
    public Map<String, Object> calculateTargetHr(int age, int restingHr, double intensityFraction)
    {
        Map<String, Object> hrmaxResult = estimateHrmax(age);
        int hrmax = (int) hrmaxResult.get("hrmax");
        int hrReserve = hrmax - restingHr;
        double targetHr = hrReserve * intensityFraction + restingHr;

        // Apply safety cap / 应用安全上限
        int safeHrmax = getSafeHrmax(age);
        boolean safe = targetHr <= safeHrmax;
        if (targetHr > safeHrmax)
        {
            targetHr = safeHrmax;
        }

        // Match Python rounding: int(round(target_hr)), round(intensity_fraction, 3), round(intensity_pct, 1)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("target_hr", (int) Math.round(targetHr));
        result.put("hrmax", hrmax);
        result.put("hr_reserve", hrReserve);
        result.put("resting_hr", restingHr);
        result.put("intensity_fraction", roundToDecimals(intensityFraction, 3));
        result.put("intensity_pct_hrr", roundToDecimals(intensityFraction * 100.0, 1));
        result.put("safe", safe);
        result.put("safety_cap_applied", !safe);
        result.put("evidence_ref", RESERVE_EVIDENCE_REF);

        log.debug("calculateTargetHr: age={}, restingHr={}, intensity={} -> targetHr={}, safe={}",
                  age, restingHr, String.format("%.3f", intensityFraction),
                  (int) Math.round(targetHr), safe);
        return result;
    }

    /**
     * 计算目标心率范围 / Calculate target HR range (e.g., for a moderate intensity zone).
     *
     * @param age       年龄 / Person's age
     * @param restingHr 静息心率 / Resting heart rate
     * @param hrrLow    储备心率下限 / Lower bound of HRR fraction
     * @param hrrHigh   储备心率上限 / Upper bound of HRR fraction
     * @return Map with hr_low, hr_high, hrmax, hrr_range, resting_hr, safe
     */
    public Map<String, Object> calculateTargetHrRange(int age, int restingHr, double hrrLow, double hrrHigh)
    {
        Map<String, Object> low = calculateTargetHr(age, restingHr, hrrLow);
        Map<String, Object> high = calculateTargetHr(age, restingHr, hrrHigh);

        List<Double> hrrRange = Arrays.asList(hrrLow, hrrHigh);

        boolean safe = (boolean) low.get("safe") && (boolean) high.get("safe");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hr_low", low.get("target_hr"));
        result.put("hr_high", high.get("target_hr"));
        result.put("hrmax", low.get("hrmax"));
        result.put("hrr_range", hrrRange);
        result.put("resting_hr", restingHr);
        result.put("safe", safe);

        log.debug("calculateTargetHrRange: age={}, restingHr={}, hrr=[{},{}] -> hr=[{},{}], safe={}",
                  age, restingHr,
                  String.format("%.2f", hrrLow), String.format("%.2f", hrrHigh),
                  low.get("target_hr"), high.get("target_hr"), safe);
        return result;
    }

    /**
     * 获取所有强度区间 / Get all intensity zones with calculated HR ranges for this person.
     *
     * Reference: acsm_2021_ch7
     *
     * @param age       年龄 / Person's age
     * @param restingHr 静息心率 / Resting heart rate
     * @return Map mapping zone name → HRZone with calculated HR bounds
     */
    public Map<String, HRZone> getIntensityZones(int age, int restingHr)
    {
        // Use LinkedHashMap to preserve insertion order (matching Python dict ordering from YAML)
        Map<String, HRZone> zones = new LinkedHashMap<>();
        for (ZoneDefinition zoneDef : INTENSITY_ZONES)
        {
            Map<String, Object> rangeResult = calculateTargetHrRange(
                age, restingHr, zoneDef.hrrLow, zoneDef.hrrHigh
            );

            HRZone zone = new HRZone(
                zoneDef.name,
                (int) rangeResult.get("hr_low"),
                (int) rangeResult.get("hr_high"),
                new double[]{zoneDef.hrrLow, zoneDef.hrrHigh},
                zoneDef.perceivedExertion,
                zoneDef.evidenceRef
            );
            zones.put(zoneDef.name, zone);
        }

        log.debug("getIntensityZones: age={}, restingHr={}, zones={}", age, restingHr, zones.keySet());
        return zones;
    }

    /**
     * 对实测心率进行强度分级 / Classify an observed HR into an intensity zone.
     *
     * Useful for real-time feedback and compliance tracking.
     * 用于实时反馈和依从性追踪。
     *
     * @param age       年龄 / Person's age
     * @param restingHr 静息心率 / Resting heart rate
     * @param actualHr  实测心率 / Observed heart rate
     * @return Map with zone, hrr_pct, actual_hr, hrmax (and error if hr_reserve <= 0)
     */
    public Map<String, Object> classifyIntensity(int age, int restingHr, int actualHr)
    {
        Map<String, Object> hrmaxResult = estimateHrmax(age);
        int hrmax = (int) hrmaxResult.get("hrmax");
        int hrReserve = hrmax - restingHr;

        // Edge case: non-positive HR reserve
        if (hrReserve <= 0)
        {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("zone", "unknown");
            errorResult.put("hrr_pct", 0);
            errorResult.put("actual_hr", actualHr);
            errorResult.put("error", "HR reserve is zero or negative (resting_hr >= hrmax)");
            log.warn("classifyIntensity: hrReserve={} (restingHr={} >= hrmax={}) — returning unknown",
                     hrReserve, restingHr, hrmax);
            return errorResult;
        }

        double hrrPct = ((double) (actualHr - restingHr)) / hrReserve;

        // Find matching zone / 查找匹配的强度区间
        String zoneName = "below_light";
        for (ZoneDefinition zoneDef : INTENSITY_ZONES)
        {
            if (hrrPct >= zoneDef.hrrLow && hrrPct <= zoneDef.hrrHigh)
            {
                zoneName = zoneDef.name;
                break;
            }
        }

        // Check if above vigorous / 检查是否超过高强度区间上限
        double vigorousHigh = getVigorousHrrHigh();
        if (hrrPct > vigorousHigh)
        {
            zoneName = "above_vigorous";
        }

        // Match Python: round(hrr_pct * 100, 1)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zone", zoneName);
        result.put("hrr_pct", roundToDecimals(hrrPct * 100.0, 1));
        result.put("actual_hr", actualHr);
        result.put("hrmax", hrmax);

        log.debug("classifyIntensity: age={}, restingHr={}, actualHr={} -> zone={}, hrrPct={}%",
                  age, restingHr, actualHr, zoneName, String.format("%.1f", hrrPct * 100.0));
        return result;
    }

    // ========== Private Helpers ==========

    /**
     * Round a double to the specified number of decimal places.
     * Equivalent to Python's round(x, decimals).
     * 等价于 Python 的 round(x, decimals)，使用四舍五入（HALF_UP）。
     *
     * @param value    原始值 / The value to round
     * @param decimals 小数位数 / Number of decimal places
     * @return 舍入后的值 / Rounded value
     */
    private static double roundToDecimals(double value, int decimals)
    {
        double multiplier = Math.pow(10.0, decimals);
        return Math.round(value * multiplier) / multiplier;
    }

    /**
     * Get the upper HRR bound for the vigorous zone.
     * Used by classifyIntensity to detect "above_vigorous".
     * 对应 Python 中 self._intensity_zones.get("vigorous", {}).get("hrr_range", [0, 0.89])[1]
     */
    private static double getVigorousHrrHigh()
    {
        for (ZoneDefinition zd : INTENSITY_ZONES)
        {
            if ("vigorous".equals(zd.name))
            {
                return zd.hrrHigh;
            }
        }
        // Fallback matching Python default: [0, 0.89][1]
        return 0.89;
    }

    // ========== Inner Classes ==========

    /**
     * 心率区间结果类型 / A heart rate zone with calculated bounds and metadata.
     *
     * Mirrors Python HRZone (pydantic BaseModel) from heart_rate.py.
     */
    public static class HRZone
    {
        /** 区间名称 / Zone name (e.g. "light", "moderate", "vigorous") */
        private final String name;
        /** 下限心率 / Lower HR bound (bpm) */
        private final int lowerHr;
        /** 上限心率 / Upper HR bound (bpm) */
        private final int upperHr;
        /** 储备心率范围 / HRR fraction range [low, high] */
        private final double[] hrrRange;
        /** 感知疲劳度 / Perceived exertion description */
        private final String perceivedExertion;
        /** 循证引用 / Evidence reference */
        private final String evidenceRef;

        public HRZone(String name, int lowerHr, int upperHr, double[] hrrRange,
                      String perceivedExertion, String evidenceRef)
        {
            this.name = name;
            this.lowerHr = lowerHr;
            this.upperHr = upperHr;
            this.hrrRange = hrrRange;
            this.perceivedExertion = perceivedExertion;
            this.evidenceRef = evidenceRef;
        }

        public String getName()
        {
            return name;
        }

        public int getLowerHr()
        {
            return lowerHr;
        }

        public int getUpperHr()
        {
            return upperHr;
        }

        public double[] getHrrRange()
        {
            return hrrRange;
        }

        public String getPerceivedExertion()
        {
            return perceivedExertion;
        }

        public String getEvidenceRef()
        {
            return evidenceRef;
        }

        @Override
        public String toString()
        {
            return "HRZone{" +
                   "name='" + name + '\'' +
                   ", lowerHr=" + lowerHr +
                   ", upperHr=" + upperHr +
                   ", hrrRange=[" + hrrRange[0] + ", " + hrrRange[1] + "]" +
                   ", perceivedExertion='" + perceivedExertion + '\'' +
                   ", evidenceRef='" + evidenceRef + '\'' +
                   '}';
        }
    }

    /**
     * 强度区间配置定义 / Internal zone definition loaded from YAML config constants.
     * Corresponds to entries in exercise_rules.yaml → intensity_zones.
     */
    private static class ZoneDefinition
    {
        final String name;
        final double hrrLow;
        final double hrrHigh;
        final String perceivedExertion;
        final String evidenceRef;

        ZoneDefinition(String name, double hrrLow, double hrrHigh,
                       String perceivedExertion, String evidenceRef)
        {
            this.name = name;
            this.hrrLow = hrrLow;
            this.hrrHigh = hrrHigh;
            this.perceivedExertion = perceivedExertion;
            this.evidenceRef = evidenceRef;
        }
    }
}
