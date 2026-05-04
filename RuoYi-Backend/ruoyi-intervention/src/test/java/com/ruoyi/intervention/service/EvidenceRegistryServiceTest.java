package com.ruoyi.intervention.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ruoyi.intervention.domain.enums.EvidenceLevel;
import com.ruoyi.intervention.domain.model.EvidenceSource;

/**
 * JUnit 5 tests for EvidenceRegistryService.
 * Tests the evidence traceability registry: register, retrieve, list, exists.
 *
 * Migrated from: intervention-engine/src/algorithms/evidence_registry.py
 * Ticket: XIN-83
 */
class EvidenceRegistryServiceTest
{
    private EvidenceRegistryService service;

    @BeforeEach
    void setUp()
    {
        service = new EvidenceRegistryService();
        service.init(); // Trigger @PostConstruct manually
    }

    // ========== Default Sources ==========

    @Nested
    @DisplayName("Init — Default source registration")
    class TestInit
    {
        @Test
        @DisplayName("Default sources registered on init")
        void test_default_sources_registered()
        {
            assertTrue(service.size() > 10);
        }

        @Test
        @DisplayName("Core ACSM source exists")
        void test_acsm_exists()
        {
            assertTrue(service.exists("acsm_2021_ch26"));
            EvidenceSource src = service.get("acsm_2021_ch26");
            assertNotNull(src);
            assertEquals("ACSM", src.getAuthors());
            assertEquals(2021, src.getYear());
            assertEquals("guideline", src.getSourceType());
            assertEquals(EvidenceLevel.HIGH, src.getLevel());
        }

        @Test
        @DisplayName("WHO source exists")
        void test_who_exists()
        {
            EvidenceSource src = service.get("who_2020_pa");
            assertNotNull(src);
            assertTrue(src.getTitle().contains("WHO"));
            assertEquals(EvidenceLevel.HIGH, src.getLevel());
        }

        @Test
        @DisplayName("JITAI sources exist")
        void test_jitai_sources()
        {
            assertTrue(service.exists("klasnja_2015_jitai"));
            assertTrue(service.exists("nahum_shani_2018_jitai_design"));
            assertTrue(service.exists("liao_2020_micro_randomized_trials"));
        }

        @Test
        @DisplayName("Chinese national standards exist")
        void test_chinese_standards()
        {
            assertTrue(service.exists("china_gbt_10000_2023"));
            assertTrue(service.exists("china_dris_2023"));
            assertTrue(service.exists("china_dietary_guidelines_2022"));
        }

        @Test
        @DisplayName("Sleep evidence sources exist")
        void test_sleep_sources()
        {
            assertTrue(service.exists("hirshkowitz_2015"));
            assertTrue(service.exists("buysse_1989_psqi"));
            assertTrue(service.exists("aasm_2020"));
        }

        @Test
        @DisplayName("HRV/stress sources exist")
        void test_hrv_sources()
        {
            assertTrue(service.exists("kim_2018_hrv_stress"));
            assertTrue(service.exists("laborde_2017_hrv_biofeedback"));
        }
    }

    // ========== Register & Retrieve ==========

    @Nested
    @DisplayName("RegisterRetrieve — Manual registration")
    class TestRegisterRetrieve
    {
        @Test
        @DisplayName("Register and retrieve a source")
        void test_register_and_get()
        {
            EvidenceSource src = new EvidenceSource(
                "test_001", "Test Study", "Test Author", 2024,
                "rct", EvidenceLevel.MODERATE, "https://example.com", "A test source");

            service.register(src);

            EvidenceSource retrieved = service.get("test_001");
            assertNotNull(retrieved);
            assertEquals("Test Study", retrieved.getTitle());
            assertEquals("Test Author", retrieved.getAuthors());
            assertEquals(2024, retrieved.getYear());
            assertEquals("rct", retrieved.getSourceType());
            assertEquals(EvidenceLevel.MODERATE, retrieved.getLevel());
            assertEquals("https://example.com", retrieved.getUrl());
            assertEquals("A test source", retrieved.getDescription());
        }

        @Test
        @DisplayName("Register overwrites existing source")
        void test_overwrite()
        {
            EvidenceSource v1 = new EvidenceSource();
            v1.setSourceId("dup_001");
            v1.setTitle("Version 1");
            service.register(v1);

            EvidenceSource v2 = new EvidenceSource();
            v2.setSourceId("dup_001");
            v2.setTitle("Version 2");
            service.register(v2);

            assertEquals("Version 2", service.get("dup_001").getTitle());
        }

        @Test
        @DisplayName("Get non-existent returns null")
        void test_get_nonexistent()
        {
            assertNull(service.get("nonexistent_999"));
        }
    }

    // ========== List & Exists ==========

    @Nested
    @DisplayName("ListExists — Listing and checking")
    class TestListExists
    {
        @Test
        @DisplayName("listSources returns all registered sources")
        void test_list_sources()
        {
            List<EvidenceSource> sources = service.listSources();
            assertEquals(service.size(), sources.size());
        }

        @Test
        @DisplayName("exists returns true for registered source")
        void test_exists_true()
        {
            assertTrue(service.exists("acsm_2021_ch26"));
        }

        @Test
        @DisplayName("exists returns false for unknown source")
        void test_exists_false()
        {
            assertFalse(service.exists("no_such_source"));
        }

        @Test
        @DisplayName("size increments after register")
        void test_size_increment()
        {
            int before = service.size();
            EvidenceSource src = new EvidenceSource();
            src.setSourceId("new_001");
            src.setTitle("New");
            service.register(src);

            assertEquals(before + 1, service.size());
        }

        @Test
        @DisplayName("size does not increment for overwrite")
        void test_size_no_increment_overwrite()
        {
            int before = service.size();
            EvidenceSource src = new EvidenceSource();
            src.setSourceId("acsm_2021_ch26");
            src.setTitle("Updated");
            service.register(src);

            assertEquals(before, service.size());
        }
    }

    // ========== Evidence Level Coverage ==========

    @Nested
    @DisplayName("EvidenceLevels — All levels represented")
    class TestEvidenceLevels
    {
        @Test
        @DisplayName("HIGH level sources exist")
        void test_high_level()
        {
            EvidenceSource src = service.get("acsm_2021_ch26");
            assertEquals(EvidenceLevel.HIGH, src.getLevel());
        }

        @Test
        @DisplayName("MODERATE level sources exist")
        void test_moderate_level()
        {
            EvidenceSource src = service.get("klasnja_2015_jitai");
            assertEquals(EvidenceLevel.MODERATE, src.getLevel());
        }

        @Test
        @DisplayName("EXPERT level sources exist")
        void test_expert_level()
        {
            EvidenceSource src = service.get("walker_2017");
            assertEquals(EvidenceLevel.EXPERT, src.getLevel());
        }
    }

    // ========== Source Types ==========

    @Nested
    @DisplayName("SourceTypes — All types represented")
    class TestSourceTypes
    {
        @Test
        @DisplayName("guideline type exists")
        void test_guideline()
        {
            EvidenceSource src = service.get("acsm_2021_ch26");
            assertEquals("guideline", src.getSourceType());
        }

        @Test
        @DisplayName("meta_analysis type exists")
        void test_meta_analysis()
        {
            EvidenceSource src = service.get("tanaka_2001_hrmax");
            assertEquals("meta_analysis", src.getSourceType());
        }

        @Test
        @DisplayName("rct type exists")
        void test_rct()
        {
            EvidenceSource src = service.get("karvonen_1957");
            assertEquals("rct", src.getSourceType());
        }

        @Test
        @DisplayName("consensus type exists")
        void test_consensus()
        {
            EvidenceSource src = service.get("stepanski_2003");
            assertEquals("consensus", src.getSourceType());
        }

        @Test
        @DisplayName("textbook type exists")
        void test_textbook()
        {
            EvidenceSource src = service.get("walker_2017");
            assertEquals("textbook", src.getSourceType());
        }
    }
}
