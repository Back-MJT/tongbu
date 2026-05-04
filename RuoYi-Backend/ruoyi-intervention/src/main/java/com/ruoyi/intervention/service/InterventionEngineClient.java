package com.ruoyi.intervention.service;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * InterventionEngine HTTP Client — Java ↔ Python FastAPI Integration.
 *
 * <p>XIN-146: Python算法服务化 (intervention-engine → Java/RuoYi微服务)
 *
 * <p>This client bridges the Java RuoYi backend to the Python FastAPI engine
 * running at :4001. It calls the Python engine for algorithm-heavy operations
 * that are already implemented there (health profile management, prescription
 * generation, rule engine).
 *
 * <p>Architecture (per XIN-144 heartbeat):
 * <ul>
 *   <li>Python :4001 = Algorithm engine (prescription generation, rule engine, health scoring)</li>
 *   <li>Java RuoYi :8080 = API gateway (auth, tenant isolation, business logic)</li>
 *   <li>This client = HTTP bridge from Java to Python for algorithm requests</li>
 * </ul>
 *
 * <p>Endpoints called:
 * <ul>
 *   <li>GET  /health — Engine health check</li>
 *   <li>POST /api/profiles — Create health profile</li>
 *   <li>GET  /api/profiles/{profileId} — Get health profile</li>
 *   <li>POST /api/prescriptions/exercise — Generate exercise prescription</li>
 *   <li>POST /api/prescriptions/sleep — Generate sleep prescription</li>
 *   <li>POST /api/miniprogram/prescription — Generate mini-program prescription (primary)</li>
 * </ul>
 *
 * <p>All methods return null on failure (graceful degradation) so the Java
 * services can fall back to their own in-memory logic without crashing.
 */
@Service
public class InterventionEngineClient
{
    private static final Logger log = LoggerFactory.getLogger(InterventionEngineClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String engineBaseUrl;
    private boolean available = false;

    // ========== Constructor ==========

    @Autowired
    public InterventionEngineClient(
            @Value("${ruoyi.intervention.engine-url:http://localhost:4001}") String engineBaseUrl)
    {
        this.engineBaseUrl = engineBaseUrl;

        // Configure RestTemplate with timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);

        this.objectMapper = new ObjectMapper();

        // Initial health check
        checkHealth();
    }

    // ========== Health Check ==========

    /**
     * Check if the Python engine is available.
     *
     * @return true if engine responds to /health
     */
    public boolean isAvailable()
    {
        return available;
    }

    private void checkHealth()
    {
        try
        {
            String url = engineBaseUrl + "/health";
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            this.available = resp.getStatusCode().is2xxSuccessful();
            log.info("InterventionEngineClient: engine at {} is {}",
                engineBaseUrl, available ? "AVAILABLE" : "UNAVAILABLE");
        }
        catch (Exception e)
        {
            this.available = false;
            log.warn("InterventionEngineClient: engine at {} is UNAVAILABLE: {}",
                engineBaseUrl, e.getMessage());
        }
    }

    // ========== Profile Operations ==========

    /**
     * Create a health profile in the Python engine.
     *
     * @param userId User ID
     * @param age Age in years
     * @param gender Gender (male/female/other)
     * @param healthGoals List of health goals
     * @return Profile ID, or null on failure
     */
    public String createProfile(String userId, int age, String gender, java.util.List<String> healthGoals)
    {
        try
        {
            String url = engineBaseUrl + "/api/profiles";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("user_id", userId);

            ObjectNode demographic = objectMapper.createObjectNode();
            demographic.put("age", age);
            demographic.put("gender", gender);
            body.set("demographic", demographic);

            body.putPOJO("goals", healthGoals != null ? healthGoals : java.util.List.of());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null)
            {
                JsonNode node = objectMapper.readTree(resp.getBody());
                String profileId = node.path("id").asText(node.path("user_id").asText(userId));
                log.info("createProfile: userId={}, profileId={}", userId, profileId);
                return profileId;
            }
        }
        catch (Exception e)
        {
            log.error("createProfile failed: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * Get a health profile from the Python engine.
     *
     * @param profileId Profile ID
     * @return Profile as JsonNode map, or null on failure
     */
    public Map<String, Object> getProfile(String profileId)
    {
        try
        {
            String url = engineBaseUrl + "/api/profiles/" + profileId;
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = objectMapper.readValue(resp.getBody(), Map.class);
                return profile;
            }
        }
        catch (Exception e)
        {
            log.error("getProfile failed: profileId={}, error={}", profileId, e.getMessage());
        }
        return null;
    }

    // ========== Prescription Operations ==========

    /**
     * Generate an exercise prescription from the Python rule engine.
     *
     * @param profileId Profile ID
     * @return Prescription as JsonNode, or null on failure
     */
    public Map<String, Object> generateExercisePrescription(String profileId)
    {
        try
        {
            String url = engineBaseUrl + "/api/prescriptions/exercise";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("profile_id", profileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> rx = objectMapper.readValue(resp.getBody(), Map.class);
                log.info("generateExercisePrescription: profileId={}, prescriptionId={}",
                    profileId, rx.get("prescription_id"));
                return rx;
            }
        }
        catch (Exception e)
        {
            log.error("generateExercisePrescription failed: profileId={}, error={}", profileId, e.getMessage());
        }
        return null;
    }

    /**
     * Generate a sleep prescription from the Python rule engine.
     *
     * @param profileId Profile ID
     * @return Prescription as JsonNode, or null on failure
     */
    public Map<String, Object> generateSleepPrescription(String profileId)
    {
        try
        {
            String url = engineBaseUrl + "/api/prescriptions/sleep";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("profile_id", profileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> rx = objectMapper.readValue(resp.getBody(), Map.class);
                log.info("generateSleepPrescription: profileId={}, prescriptionId={}",
                    profileId, rx.get("prescription_id"));
                return rx;
            }
        }
        catch (Exception e)
        {
            log.error("generateSleepPrescription failed: profileId={}, error={}", profileId, e.getMessage());
        }
        return null;
    }

    // ========== Mini-Program Prescription (Primary for C-end) ==========

    /**
     * Generate a mini-program prescription (the primary C-end API).
     *
     * @param userId User ID
     * @param age Age in years
     * @param gender Gender string
     * @param deviceType Device type (跑步机, etc.)
     * @param restingHr Resting heart rate (optional)
     * @param weeklyFrequency Weekly training frequency
     * @param sessionsLast30Days Sessions in last 30 days
     * @param hypertension Has hypertension
     * @param cardiovascularRisk Has cardiovascular risk
     * @param sedentary Is sedentary
     * @param overweight Is overweight
     * @param highStress Has high stress
     * @param cardiovascularScore Cardiovascular score 0-100
     * @param musculoskeletalScore Musculoskeletal score 0-100
     * @param metabolicScore Metabolic score 0-100
     * @return Mini-program prescription dict, or null on failure
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> generateMiniProgramPrescription(
            String userId,
            int age,
            String gender,
            String deviceType,
            Integer restingHr,
            Float weeklyFrequency,
            int sessionsLast30Days,
            boolean hypertension,
            boolean cardiovascularRisk,
            boolean sedentary,
            boolean overweight,
            boolean highStress,
            Double cardiovascularScore,
            Double musculoskeletalScore,
            Double metabolicScore)
    {
        try
        {
            String url = engineBaseUrl + "/api/miniprogram/prescription";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("user_id", userId);
            body.put("age", age);
            body.put("gender", gender != null ? gender : "未指定");
            body.put("device_type", deviceType != null ? deviceType : "跑步机");
            body.put("sessions_last_30_days", sessionsLast30Days);
            body.put("hypertension", hypertension);
            body.put("cardiovascular_risk", cardiovascularRisk);
            body.put("sedentary", sedentary);
            body.put("overweight", overweight);
            body.put("high_stress", highStress);

            if (restingHr != null) body.put("resting_hr", restingHr);
            if (weeklyFrequency != null) body.put("weekly_frequency", weeklyFrequency);
            if (cardiovascularScore != null) body.put("cardiovascular_score", cardiovascularScore);
            if (musculoskeletalScore != null) body.put("musculoskeletal_score", musculoskeletalScore);
            if (metabolicScore != null) body.put("metabolic_score", metabolicScore);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null)
            {
                Map<String, Object> result = objectMapper.readValue(resp.getBody(), Map.class);
                log.info("generateMiniProgramPrescription: userId={}, resultKeys={}",
                    userId, result.keySet());
                return result;
            }
        }
        catch (Exception e)
        {
            log.error("generateMiniProgramPrescription failed: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * Generate mini-program prescription with just userId + age (minimal params).
     */
    public java.util.Map<String, Object> generateMiniProgramPrescriptionMinimal(String userId, int age)
    {
        return generateMiniProgramPrescription(
            userId, age, "未指定", "跑步机",
            null, null, 0,
            false, false, false, false, false,
            null, null, null
        );
    }
}
