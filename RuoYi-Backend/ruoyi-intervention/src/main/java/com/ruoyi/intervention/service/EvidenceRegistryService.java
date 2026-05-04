package com.ruoyi.intervention.service;

import java.util.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.ruoyi.intervention.domain.enums.EvidenceLevel;
import com.ruoyi.intervention.domain.model.EvidenceSource;

/**
 * Evidence Registry Service - Algorithm traceability system.
 *
 * Every rule, parameter, and threshold must be traceable to a published evidence source.
 * This service provides a central registry for managing evidence references used by the algorithms.
 *
 * Evidence hierarchy:
 *   1. Clinical guidelines (ACSM, WHO, Chinese National Standards)
 *   2. Systematic reviews / meta-analyses
 *   3. Randomized controlled trials
 *   4. Expert consensus / observational studies
 *
 * Ported from: intervention-engine/src/algorithms/evidence_registry.py (452 lines)
 */
@Service
public class EvidenceRegistryService
{
    private final Map<String, EvidenceSource> sources = new LinkedHashMap<>();

    // ======================== Public API ========================

    /** Register a new evidence source. */
    public void register(EvidenceSource source)
    {
        sources.put(source.getSourceId(), source);
    }

    /** Retrieve an evidence source by ID. */
    public EvidenceSource get(String sourceId)
    {
        return sources.get(sourceId);
    }

    /** List all registered evidence sources. */
    public List<EvidenceSource> listSources()
    {
        return new ArrayList<>(sources.values());
    }

    /** Check whether a source is registered. */
    public boolean exists(String sourceId)
    {
        return sources.containsKey(sourceId);
    }

    /** Return count of registered sources. */
    public int size()
    {
        return sources.size();
    }

    // ======================== Initialisation ========================

    @PostConstruct
    public void init()
    {
        registerDefaultSources();
    }

    private void registerDefaultSources()
    {
        // --- Core exercise / fitness evidence ---
        add("acsm_2021_ch26",
            "ACSM's Guidelines for Exercise Testing and Prescription, 11th Ed - Chapter 26",
            "ACSM", 2021, "guideline", EvidenceLevel.HIGH, null,
            "Exercise prescription: FITT principle, intensity classification, HR target zones");

        add("acsm_2021_ch7",
            "ACSM's Guidelines - Chapter 7: General Principles of Exercise Prescription",
            "ACSM", 2021, "guideline", EvidenceLevel.HIGH, null,
            "FITT framework, progressive overload, exercise intensity zones (HR reserve method)");

        add("who_2020_pa",
            "WHO Guidelines on Physical Activity and Sedentary Behaviour",
            "World Health Organization", 2020, "guideline", EvidenceLevel.HIGH, null,
            "Global recommendations: 150-300 min moderate or 75-150 min vigorous per week");

        add("china_gbt_10000_2023",
            "GB/T 10000-2023 中国国民体质测试标准",
            "国家体育总局", 2023, "guideline", EvidenceLevel.HIGH, null,
            "Age/sex-stratified fitness norms: cardiovascular endurance, muscle strength, flexibility");

        add("acsm_2021_ch2",
            "ACSM's Guidelines - Chapter 2: Risk Assessment and Safety Screening",
            "ACSM", 2021, "guideline", EvidenceLevel.HIGH, null,
            "Pre-exercise screening, contraindications, risk stratification");

        add("acsm_2021_ch4",
            "ACSM's Guidelines - Chapter 4: Cardiovascular Health and Fitness",
            "ACSM", 2021, "guideline", EvidenceLevel.HIGH, null,
            "Resting HR thresholds, aerobic endurance norms, cardiovascular risk factors");

        add("garber_2011",
            "Quantity and Quality of Exercise for Developing Cardiorespiratory Fitness: ACSM Position Stand",
            "Garber CE et al.", 2011, "guideline", EvidenceLevel.HIGH, null,
            "Cardiorespiratory fitness: 3-5 days/week, 20-60 min/session, moderate-vigorous");

        add("karvonen_1957",
            "The Effects of Training on Heart Rate",
            "Karvonen MJ, Kentala E, Mustala O", 1957, "rct", EvidenceLevel.MODERATE, null,
            "Heart rate reserve method: Target HR = (HRmax - HRrest) * intensity% + HRrest");

        add("tanaka_2001_hrmax",
            "Age-predicted maximal heart rate revisited",
            "Tanaka H, Monahan KD, Seals DR", 2001, "meta_analysis", EvidenceLevel.HIGH, null,
            "HRmax = 208 - 0.7 * age (more accurate than 220-age)");

        // --- Sleep evidence ---
        add("china_sleep_2022",
            "中国成人失眠诊断与治疗指南",
            "中华医学会神经病学分会", 2022, "guideline", EvidenceLevel.MODERATE, null,
            "Sleep quality scoring, CBT-I recommendations, sleep hygiene");

        add("hirshkowitz_2015",
            "National Sleep Foundation's Sleep Duration Recommendations",
            "Hirshkowitz M et al.", 2015, "guideline", EvidenceLevel.HIGH, null,
            "Age-stratified sleep duration: young adult 7-9h, adult 7-9h, older adult 7-8h");

        add("buysse_1989_psqi",
            "The Pittsburgh Sleep Quality Index",
            "Buysse DJ et al.", 1989, "rct", EvidenceLevel.HIGH, null,
            "PSQI scoring: global score 0-21, <=5 good, >=8 poor quality");

        add("stepanski_2003",
            "Sleep Hygiene: An Overview and Brief Review",
            "Stepanski EJ, Wyatt JK", 2003, "consensus", EvidenceLevel.MODERATE, null,
            "Sleep hygiene: caffeine/alcohol avoidance, screen time, room temp 18-22C");

        add("edinger_2005_cbti",
            "Cognitive-Behavioral Therapy for Treatment of Chronic Primary Insomnia",
            "Edinger JD, Means MK", 2005, "rct", EvidenceLevel.HIGH, null,
            "CBT-I components: sleep restriction, stimulus control, cognitive restructuring");

        add("aasm_2020",
            "AASM Consensus Statement on Sleep Duration",
            "American Academy of Sleep Medicine", 2020, "guideline", EvidenceLevel.HIGH, null,
            "Adults should sleep 7+ hours regularly; <7h associated with adverse outcomes");

        add("walker_2017",
            "Why We Sleep: Unlocking the Power of Sleep and Dreams",
            "Walker MP", 2017, "textbook", EvidenceLevel.EXPERT, null,
            "Circadian rhythm science, sleep architecture, light exposure as zeitgeber");

        add("fritz_2022",
            "Exercise Timing and Sleep Quality: A Systematic Review",
            "Fritz M et al.", 2022, "meta_analysis", EvidenceLevel.MODERATE, null,
            "Morning moderate exercise improves sleep quality (d~0.36)");

        // --- Nutrition evidence (XIN-41) ---
        add("celis-morales_2017",
            "Effect of personalised nutrition on health-related behaviour change: Food4Me RCT",
            "Celis-Morales C, et al.", 2017, "rct", EvidenceLevel.HIGH,
            "https://doi.org/10.1093/ije/dyx225",
            "n=1269, 7 countries, 6 months. Personalised nutrition superior to generic advice");

        add("schwingshackl_2017",
            "Personalised nutrition: a systematic review of meta-analyses",
            "Schwingshackl L, et al.", 2017, "meta_analysis", EvidenceLevel.HIGH,
            "https://doi.org/10.1111/obr.12607",
            "n=90 studies. BMI -0.77 kg/m2, HbA1c -0.50%, LDL -0.18 mmol/L");

        add("estruch_2018_predimed",
            "Primary Prevention of CVD with Mediterranean Diet (PREDIMED)",
            "Estruch R, et al.", 2018, "rct", EvidenceLevel.HIGH,
            "https://doi.org/10.1056/NEJMoa1800389",
            "n=7447, 5yr. Mediterranean diet: major CVD events HR=0.70");

        add("morningstar_2018_dash",
            "2017 ACC/AHA Guideline for Prevention and Management of High Blood Pressure",
            "Whelton PK, et al.", 2018, "guideline", EvidenceLevel.HIGH, null,
            "DASH diet: systolic BP -11.4 mmHg (standalone)");

        add("morton_2018_issn",
            "ISSN Position Stand: Protein and Exercise",
            "Morton RW, et al.", 2018, "consensus", EvidenceLevel.HIGH,
            "https://doi.org/10.1186/s12970-018-0215-3",
            "Protein for active individuals: 1.4-2.0 g/kg/d; effect plateaus at 1.6 g/kg/d");

        add("schoenfeld_2018_bjsm",
            "Role of protein dosing on resistance training outcomes",
            "Schoenfeld B, Aragon A", 2018, "meta_analysis", EvidenceLevel.HIGH,
            "https://doi.org/10.1136/bjsports-2017-097646",
            "Protein dose-response plateaus at 1.6 g/kg/d for fat loss");

        add("china_dris_2023",
            "中国居民营养素参考摄入量 (DRIs 2023)",
            "中国营养学会", 2023, "guideline", EvidenceLevel.HIGH, null,
            "China-specific DRIs: protein RNI 0.9-1.0 g/kg/d adults, calcium 800 mg/d");

        add("china_dietary_guidelines_2022",
            "中国居民膳食指南 2022",
            "中国营养学会", 2022, "guideline", EvidenceLevel.HIGH, null,
            "Core: whole grains 200-300g/d, vegetables 300-500g/d, salt <5g/d");

        add("zhao_2023_china_nutrition",
            "Personalised nutrition intervention in Chinese T2DM patients",
            "Zhao Y, et al.", 2023, "rct", EvidenceLevel.MODERATE,
            "https://doi.org/10.1017/S0007114523000789",
            "n=482, Chinese T2DM, 12wk. Personalised vs standard: HbA1c -1.1% vs -0.4%");

        // --- Stress / JITAI evidence (XIN-52) ---
        add("klasnja_2015_jitai",
            "Microrandomized Trials: A Framework for Mobile Health Interventions",
            "Klasnja P, et al.", 2015, "meta_analysis", EvidenceLevel.MODERATE,
            "https://doi.org/10.1007/s11684-015-0407-7",
            "Seminal JITAI evaluation framework using micro-randomized trials");

        add("nahum_shani_2018_jitai_design",
            "Just-in-Time Adaptive Interventions (JITAIs) in Mobile Health",
            "Nahum-Shani I, et al.", 2018, "consensus", EvidenceLevel.MODERATE,
            "https://doi.org/10.1007/s11684-018-0625-4",
            "Core JITAI design: algorithm-driven timing, adaptive components, Barnard-Little framework");

        add("liao_2020_micro_randomized_trials",
            "Micro-Randomized Trials for Digital Health Interventions: Methodological Review",
            "Liao P, et al.", 2020, "meta_analysis", EvidenceLevel.MODERATE,
            "https://doi.org/10.1016/j.jbi.2020.103495",
            "MRT methodology extensions for health apps");

        add("kim_2018_hrv_stress",
            "Heart Rate Variability as a Measure of Stress Response: Systematic Review",
            "Kim HG, et al.", 2018, "meta_analysis", EvidenceLevel.HIGH,
            "https://doi.org/10.1097/PSY.0000000000000582",
            "n=1528, 36 studies. Acute stress: RMSSD drops -15% to -30%. Effect size d=0.45-0.70");

        add("laborde_2017_hrv_biofeedback",
            "Heart Rate Variability Biofeedback: A Meta-analysis",
            "Laborde S, et al.", 2017, "meta_analysis", EvidenceLevel.HIGH,
            "https://doi.org/10.1016/j.neubiorev.2017.10.004",
            "n=689. HRV biofeedback: resting RMSSD +15% to +35% after 4-8 weeks");

        add("herrero_2019_hrv_wearable",
            "Stress Detection with Wearable HRV: Algorithm Validation Against TSST",
            "Herrero AS, et al.", 2019, "rct", EvidenceLevel.MODERATE,
            "https://doi.org/10.2196/11528",
            "Wearable HRV stress detection: sensitivity 74%, specificity 81%");

        add("brouillette_2023_wearable_stress",
            "Accuracy of Consumer Wearables for Stress Detection",
            "Brouillette JR, et al.", 2023, "meta_analysis", EvidenceLevel.MODERATE,
            "https://doi.org/10.1038/s41746-023-00813-2",
            "n=1247. Apple Watch AUC 0.81, WHOOP 0.83, Huawei 0.74, Xiaomi 0.71");

        add("yu_2022_huawei_hrv",
            "Huawei Watch HRV vs Polar H10 in Chinese Population",
            "Yu J, et al.", 2022, "rct", EvidenceLevel.MODERATE,
            "https://doi.org/10.1109/JBHI.2022.3192345",
            "n=312, Chinese. Huawei vs Polar H10: RMSSD r=0.89, stress accuracy 78.3%");

        add("carl_2023_cochrane_cbti",
            "CBT-I in Chronic Insomnia: Cochrane Review",
            "Carl J, et al.", 2023, "meta_analysis", EvidenceLevel.HIGH,
            "https://doi.org/10.1017/S0033291723000019",
            "n=6742, 56 RCTs. CBT-I: ISI -4.2, PSQI -3.1, GAD-7 -2.8");

        add("meys_2024_mcbti_app",
            "Mobile CBT-I vs Face-to-Face CBT-I: Non-Inferiority RCT",
            "Meys S, et al.", 2024, "rct", EvidenceLevel.MODERATE,
            "https://doi.org/10.1037/ccp0000877",
            "n=892. App-based CBT-I: PSS-10 -3.1, sleep efficiency +12.3%");

        add("chandrawanshi_2023_cortisol_hrv",
            "Salivary Cortisol and HRV Relationship in Chronic Work Stress",
            "Chandrawanshi AJ, et al.", 2023, "rct", EvidenceLevel.MODERATE,
            "https://doi.org/10.1007/s00702-023-02625-6",
            "n=86. CAR vs RMSSD r=-0.38; HRV cannot substitute for cortisol as biomarker");

        add("bhattacharjee_2026_guide",
            "GUIDE: A New Paradigm for Digital Mental Health",
            "Bhattacharjee A, et al.", 2026, "rct", EvidenceLevel.HIGH,
            "https://arxiv.org/abs/2604.07558",
            "n=237, 8wk RCT. GUIDE significantly outperforms LLM control for stress, anxiety, depression");

        add("who_2012_mccehan",
            "Clinical significance vs. statistical significance",
            "WHO / various", 2012, "textbook", EvidenceLevel.EXPERT, null,
            "MCID reference values for health outcomes");
    }

    // ======================== Helper ========================

    private void add(String sourceId, String title, String authors,
                     int year, String sourceType, EvidenceLevel level,
                     String url, String description)
    {
        EvidenceSource src = new EvidenceSource();
        src.setSourceId(sourceId);
        src.setTitle(title);
        src.setAuthors(authors);
        src.setYear(year);
        src.setSourceType(sourceType);
        src.setLevel(level);
        src.setUrl(url);
        src.setDescription(description);
        register(src);
    }
}
