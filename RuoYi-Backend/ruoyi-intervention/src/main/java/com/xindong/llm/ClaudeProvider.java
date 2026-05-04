package com.xindong.llm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Anthropic Claude提供者 / Anthropic Claude API provider.
 *
 * <p>Calls the Anthropic Messages API (POST /v1/messages).
 * Supports claude-sonnet-4, claude-haiku, etc.
 *
 * <p>Ticket: XIN-93
 */
public class ClaudeProvider extends AbstractLLMProvider
{
    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient httpClient;

    public ClaudeProvider(ProviderConfig config)
    {
        super(config);
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String name()
    {
        return "claude";
    }

    @Override
    public String getModel()
    {
        ProviderConfig cfg = getConfig();
        return cfg != null && cfg.getModel() != null ? cfg.getModel() : "claude-sonnet-4-20250514";
    }

    @Override
    protected LLMResponse doCall(String systemPrompt, String userPrompt, int maxTokens)
    {
        ProviderConfig cfg = getConfig();
        long start = System.currentTimeMillis();

        try
        {
            // Build request body
            ObjectNode body = mapper.createObjectNode();
            body.put("model", getModel());
            body.put("max_tokens", maxTokens);
            body.put("temperature", cfg != null ? cfg.getTemperature() : 0.0);

            // System prompt as top-level field
            body.put("system", systemPrompt);

            // Messages array
            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);

            String jsonBody = mapper.writeValueAsString(body);

            // Build HTTP request
            String apiKey = cfg != null ? cfg.getApiKey() : "";
            String baseUrl = cfg != null && cfg.getBaseUrl() != null ? cfg.getBaseUrl() : DEFAULT_BASE_URL;

            Request request = new Request.Builder()
                .url(baseUrl + "/v1/messages")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

            // Execute
            try (Response httpResponse = httpClient.newCall(request).execute())
            {
                long latency = System.currentTimeMillis() - start;
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

                if (!httpResponse.isSuccessful())
                {
                    return LLMResponse.error(name(), getModel(),
                        "API error " + httpResponse.code() + ": " + responseBody, latency);
                }

                // Parse response
                JsonNode root = mapper.readTree(responseBody);
                String content = "";
                JsonNode contentArr = root.get("content");
                if (contentArr != null && contentArr.isArray() && contentArr.size() > 0)
                {
                    content = contentArr.get(0).has("text") ? contentArr.get(0).get("text").asText() : "";
                }

                int tokensIn = root.has("usage") && root.get("usage").has("input_tokens")
                    ? root.get("usage").get("input_tokens").asInt() : 0;
                int tokensOut = root.has("usage") && root.get("usage").has("output_tokens")
                    ? root.get("usage").get("output_tokens").asInt() : 0;

                double costCents = cfg != null ? cfg.calculateCostCents(tokensIn, tokensOut) : 0;

                return LLMResponse.ok(content, getModel(), name(), tokensIn, tokensOut, latency, costCents);
            }
        }
        catch (IOException e)
        {
            long latency = System.currentTimeMillis() - start;
            return LLMResponse.error(name(), getModel(), "IO error: " + e.getMessage(), latency);
        }
    }
}
