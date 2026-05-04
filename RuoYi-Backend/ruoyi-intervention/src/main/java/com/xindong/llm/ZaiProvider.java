package com.xindong.llm;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
 * 智谱AI (GLM) 提供者 / Zhipu AI (GLM) API provider.
 *
 * <p>Calls the Zhipu ChatGLM API (POST /api/paas/v4/chat/completions).
 * Compatible with glm-4, glm-4-flash, glm-4-plus models.
 *
 * <p>API doc: https://open.bigmodel.cn/dev/api/normal-model/glm-4
 *
 * <p>Ticket: XIN-93
 */
public class ZaiProvider extends AbstractLLMProvider
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient httpClient;

    public ZaiProvider(ProviderConfig config)
    {
        super(config);
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String name()
    {
        return "zai";
    }

    @Override
    public String getModel()
    {
        ProviderConfig cfg = getConfig();
        return cfg != null && cfg.getModel() != null ? cfg.getModel() : "glm-4-flash";
    }

    @Override
    protected LLMResponse doCall(String systemPrompt, String userPrompt, int maxTokens)
    {
        ProviderConfig cfg = getConfig();
        long start = System.currentTimeMillis();

        try
        {
            // Build request body (Zhipu uses OpenAI-compatible format)
            ObjectNode body = mapper.createObjectNode();
            body.put("model", getModel());
            body.put("max_tokens", maxTokens);
            body.put("temperature", cfg != null ? cfg.getTemperature() : 0.0);

            // Messages array
            ArrayNode messages = body.putArray("messages");
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);

            String jsonBody = mapper.writeValueAsString(body);

            // Build HTTP request
            String apiKey = cfg != null ? cfg.getApiKey() : "";
            String baseUrl = cfg != null && cfg.getBaseUrl() != null ? cfg.getBaseUrl() : DEFAULT_BASE_URL;

            Request request = new Request.Builder()
                .url(baseUrl + "/api/paas/v4/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
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

                // Parse response (OpenAI-compatible format)
                JsonNode root = mapper.readTree(responseBody);
                String content = "";
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0)
                {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content"))
                    {
                        content = message.get("content").asText();
                    }
                }

                int tokensIn = root.has("usage") && root.get("usage").has("prompt_tokens")
                    ? root.get("usage").get("prompt_tokens").asInt() : 0;
                int tokensOut = root.has("usage") && root.get("usage").has("completion_tokens")
                    ? root.get("usage").get("completion_tokens").asInt() : 0;

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
