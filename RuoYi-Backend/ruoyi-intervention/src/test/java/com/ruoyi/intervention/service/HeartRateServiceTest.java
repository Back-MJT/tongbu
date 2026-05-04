package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.service.HeartRateService.HRZone;

/**
 * JUnit 5 tests for HeartRateService.
 * Migrated from: intervention-engine/tests/test_heart_rate.py (31 tests)
 *
 * Covers: HRmax estimation (Tanaka), target HR (Karvonen), HR ranges,
 * intensity zones, intensity classification, safety caps.
 *
 * All numerical assertions match Python test expectations exactly.
 */
class HeartRateServiceTest
{
    private HeartRateService calc;

    @BeforeEach
    void setUp()
    {
        calc = new HeartRateService();
    }

    // ========== HRmax Estimation (Tanaka: 208 - 0.7 * age) ==========

    @Nested
    @DisplayName("TestEstimateHRmax — HRmax estimation using Tanaka formula")
    class TestEstimateHRmax
    {
        @Test
        @DisplayName("30-year-old: 208 - 0.7*30 = 187")
        void test_30_year_old()
        {
            Map<String, Object> result = calc.estimateHrmax(30);
            assertEquals(187, result.get("hrmax"));
            assertEquals("tanaka", result.get("formula"));
        }

        @Test
        @DisplayName("50-year-old: 208 - 0.7*50 = 173")
        void test_50_year_old()
        {
            Map<String, Object> result = calc.estimateHrmax(50);
            assertEquals(173, result.get("hrmax"));
        }

        @Test
        @DisplayName("70-year-old: 208 - 0.7*70 = 159")
        void test_70_year_old()
        {
            Map<String, Object> result = calc.estimateHrmax(70);
            assertEquals(159, result.get("hrmax"));
        }

        @Test
        @DisplayName("20-year-old: 208 - 0.7*20 = 194")
        void test_20_year_old()
        {
            Map<String, Object> result = calc.estimateHrmax(20);
            assertEquals(194, result.get("hrmax"));
        }

        @Test
        @DisplayName("Returns evidence reference")
        void test_returns_evidence_ref()
        {
            Map<String, Object> result = calc.estimateHrmax(30);
            assertTrue(result.containsKey("evidence_ref"));
            assertEquals("tanaka_2001_hrmax", result.get("evidence_ref"));
        }

        @Test
        @DisplayName("Returns formula parameters")
        void test_returns_parameters()
        {
            Map<String, Object> result = calc.estimateHrmax(30);
            assertTrue(result.containsKey("parameters"));
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) result.get("parameters");
            assertEquals(208.0, params.get("intercept"));
            assertEquals(0.7, params.get("age_coefficient"));
        }

        @Test
        @DisplayName("Rounding to int: 208 - 0.7*33 = 184.9 -> 185")
        void test_rounding_to_int()
        {
            Map<String, Object> result = calc.estimateHrmax(33);
            assertEquals(185, result.get("hrmax"));
        }
    }

    // ========== Safe HRmax ==========

    @Nested
    @DisplayName("TestGetSafeHRmax — HRmax with safety buffer")
    class TestGetSafeHRmax
    {
        @Test
        @DisplayName("Safe HRmax is below HRmax by buffer (10 bpm)")
        void test_safe_hrmax_is_below_hrmax()
        {
            int hrmax = (int) calc.estimateHrmax(30).get("hrmax");
            int safe = calc.getSafeHrmax(30);
            assertEquals(hrmax - 10, safe);
        }

        @Test
        @DisplayName("Safe HRmax decreases with age")
        void test_safe_hrmax_decreases_with_age()
        {
            int safeYoung = calc.getSafeHrmax(25);
            int safeOld = calc.getSafeHrmax(65);
            assertTrue(safeYoung > safeOld);
        }
    }

    // ========== Target HR (Karvonen) ==========

    @Nested
    @DisplayName("TestCalculateTargetHR — Karvonen target HR with safety cap")
    class TestCalculateTargetHR
    {
        @Test
        @DisplayName("Moderate intensity 30yr: target = (187-65)*0.6+65 = 138.2 -> 138")
        void test_moderate_intensity_30yr()
        {
            Map<String, Object> result = calc.calculateTargetHr(30, 65, 0.6);
            assertEquals(138, result.get("target_hr"));
            assertEquals(187, result.get("hrmax"));
            assertEquals(122, result.get("hr_reserve"));
            assertEquals(true, result.get("safe"));
        }

        @Test
        @DisplayName("High intensity triggers safety cap")
        void test_high_intensity_triggers_safety_cap()
        {
            // HRmax = 187 (age 30), safe = 177. HRR = 187 - 60 = 127.
            // target = 127 * 0.95 + 60 = 180.65 > 177 => capped to 177
            Map<String, Object> result = calc.calculateTargetHr(30, 60, 0.95);
            int targetHr = (int) result.get("target_hr");
            assertTrue(targetHr <= calc.getSafeHrmax(30));
            assertEquals(false, result.get("safe"));
            assertEquals(true, result.get("safety_cap_applied"));
        }

        @Test
        @DisplayName("Low intensity is safe")
        void test_low_intensity_safe()
        {
            Map<String, Object> result = calc.calculateTargetHr(40, 70, 0.3);
            assertEquals(true, result.get("safe"));
            assertEquals(false, result.get("safety_cap_applied"));
        }

        @Test
        @DisplayName("Returns intensity %HRR")
        void test_returns_intensity_pct_hrr()
        {
            Map<String, Object> result = calc.calculateTargetHr(30, 65, 0.55);
            double pctHrr = ((Number) result.get("intensity_pct_hrr")).doubleValue();
            assertEquals(55.0, pctHrr, 0.01);
        }

        @Test
        @DisplayName("Returns evidence reference for Karvonen")
        void test_returns_evidence_ref()
        {
            Map<String, Object> result = calc.calculateTargetHr(30, 65, 0.5);
            assertEquals("karvonen_1957", result.get("evidence_ref"));
        }
    }

    // ========== Target HR Range ==========

    @Nested
    @DisplayName("TestCalculateTargetHRRange — HR range calculation")
    class TestCalculateTargetHRRange
    {
        @Test
        @DisplayName("Moderate range: hr_low < hr_high")
        void test_moderate_range()
        {
            Map<String, Object> result = calc.calculateTargetHrRange(30, 65, 0.50, 0.69);
            int hrLow = (int) result.get("hr_low");
            int hrHigh = (int) result.get("hr_high");
            assertTrue(hrLow < hrHigh);
            assertEquals(65, result.get("resting_hr"));
        }

        @Test
        @DisplayName("Range safe when both bounds safe")
        void test_range_safe_when_both_safe()
        {
            Map<String, Object> result = calc.calculateTargetHrRange(30, 65, 0.40, 0.60);
            assertEquals(true, result.get("safe"));
        }

        @Test
        @DisplayName("Range unsafe when upper exceeds safe")
        void test_range_unsafe_when_upper_exceeds_safe()
        {
            Map<String, Object> result = calc.calculateTargetHrRange(30, 60, 0.70, 0.95);
            assertEquals(false, result.get("safe"));
        }
    }

    // ========== Intensity Zones ==========

    @Nested
    @DisplayName("TestGetIntensityZones — All intensity zones generation")
    class TestGetIntensityZones
    {
        @Test
        @DisplayName("Returns three zones: light, moderate, vigorous")
        void test_returns_three_zones()
        {
            Map<String, HRZone> zones = calc.getIntensityZones(30, 65);
            assertTrue(zones.containsKey("light"));
            assertTrue(zones.containsKey("moderate"));
            assertTrue(zones.containsKey("vigorous"));
        }

        @Test
        @DisplayName("Zones have valid bounds and correct names")
        void test_zones_are_hrzone_objects()
        {
            Map<String, HRZone> zones = calc.getIntensityZones(30, 65);
            for (Map.Entry<String, HRZone> entry : zones.entrySet())
            {
                HRZone zone = entry.getValue();
                assertTrue(zone.getLowerHr() < zone.getUpperHr(),
                    "Zone " + entry.getKey() + ": lowerHr < upperHr");
                assertEquals(entry.getKey(), zone.getName());
            }
        }

        @Test
        @DisplayName("Zones are ordered: light < moderate < vigorous")
        void test_zones_are_ordered()
        {
            Map<String, HRZone> zones = calc.getIntensityZones(30, 65);
            assertTrue(zones.get("light").getUpperHr() <= zones.get("moderate").getLowerHr()
                || zones.get("light").getUpperHr() < zones.get("moderate").getUpperHr());
            assertTrue(zones.get("moderate").getUpperHr() < zones.get("vigorous").getUpperHr());
        }

        @Test
        @DisplayName("Zones have evidence reference acsm_2021_ch7")
        void test_zones_have_evidence_ref()
        {
            Map<String, HRZone> zones = calc.getIntensityZones(30, 65);
            for (HRZone zone : zones.values())
            {
                assertEquals("acsm_2021_ch7", zone.getEvidenceRef());
            }
        }

        @Test
        @DisplayName("Zones shift with age: older person has lower HR bounds")
        void test_zones_shift_with_age()
        {
            Map<String, HRZone> zonesYoung = calc.getIntensityZones(25, 65);
            Map<String, HRZone> zonesOld = calc.getIntensityZones(70, 70);
            assertTrue(zonesYoung.get("moderate").getLowerHr() > zonesOld.get("moderate").getLowerHr());
        }

        @Test
        @DisplayName("Zones have perceived exertion descriptions")
        void test_zones_have_perceived_exertion()
        {
            Map<String, HRZone> zones = calc.getIntensityZones(30, 65);
            for (HRZone zone : zones.values())
            {
                assertNotNull(zone.getPerceivedExertion());
                assertFalse(zone.getPerceivedExertion().isEmpty());
            }
        }
    }

    // ========== Intensity Classification ==========

    @Nested
    @DisplayName("TestClassifyIntensity — Classify observed HR into zones")
    class TestClassifyIntensity
    {
        @Test
        @DisplayName("Classify moderate: HR 135 for 30yr, RHR 65")
        void test_classify_moderate()
        {
            Map<String, Object> result = calc.classifyIntensity(30, 65, 135);
            assertEquals("moderate", result.get("zone"));
            assertEquals(135, result.get("actual_hr"));
        }

        @Test
        @DisplayName("Classify below_light: HR 70 for 30yr, RHR 65")
        void test_classify_below_light()
        {
            Map<String, Object> result = calc.classifyIntensity(30, 65, 70);
            assertEquals("below_light", result.get("zone"));
        }

        @Test
        @DisplayName("Classify above_vigorous: HR 185 for 30yr, RHR 65")
        void test_classify_above_vigorous()
        {
            Map<String, Object> result = calc.classifyIntensity(30, 65, 185);
            assertEquals("above_vigorous", result.get("zone"));
        }

        @Test
        @DisplayName("Returns HRR percentage > 0")
        void test_returns_hrr_pct()
        {
            Map<String, Object> result = calc.classifyIntensity(30, 65, 130);
            assertTrue(result.containsKey("hrr_pct"));
            double hrrPct = ((Number) result.get("hrr_pct")).doubleValue();
            assertTrue(hrrPct > 0);
        }

        @Test
        @DisplayName("Returns HRmax")
        void test_returns_hrmax()
        {
            Map<String, Object> result = calc.classifyIntensity(30, 65, 130);
            assertEquals(187, result.get("hrmax"));
        }

        @Test
        @DisplayName("Edge case: resting_hr >= HRmax -> unknown zone")
        void test_edge_case_resting_equals_hrmax()
        {
            Map<String, Object> result = calc.classifyIntensity(30, 190, 180);
            assertEquals("unknown", result.get("zone"));
            assertTrue(result.containsKey("error"));
        }

        @Test
        @DisplayName("Classify light: HR 115 for 30yr, RHR 65")
        void test_classify_light()
        {
            // Light zone is 30-49% HRR. For 30yr, RHR 65: HRR = 122
            // 30% = 101.6, 49% = 124.78
            Map<String, Object> result = calc.classifyIntensity(30, 65, 115);
            assertEquals("light", result.get("zone"));
        }

        @Test
        @DisplayName("Classify vigorous: HR 160 for 30yr, RHR 65")
        void test_classify_vigorous()
        {
            // Vigorous zone is 70-89% HRR. For 30yr, RHR 65: HRR = 122
            // 70% = 150.4, 89% = 173.58
            Map<String, Object> result = calc.classifyIntensity(30, 65, 160);
            assertEquals("vigorous", result.get("zone"));
        }
    }
}
