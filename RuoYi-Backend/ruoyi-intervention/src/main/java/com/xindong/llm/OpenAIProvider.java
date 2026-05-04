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
 * OpenAI API提供者 / OpenAI-compatible API provider.
 *
 * <p>Calls the OpenAI Chat Completions API (POST /v1/chat/completions).
 * Also works with any OpenAI-compatible endpoint (e.g., Azure OpenAI, local LLMs).
 *
 * <p>Ticket: XIN-93
 */
public class OpenAIProvider extends AbstractLLMProvider
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient httpClient;

    public OpenAIProvider(ProviderConfig config)
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
        return "openai";
    }

    @Override
    public String getModel()
    {
        ProviderConfig cfg = getConfig();
        return cfg != null && cfg.getModel() != null ? cfg.getModel() : "gpt-4o";
    }

    @Override
    protected LLMResponse doCall(String systemPrompt, String userPrompt, int maxTokens)
    {
        ProviderConfig cfg = getConfig();
        long start = System.currentTimeMillis();

        try
        {
            // Build request body (OpenAI Chat Completions format)
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
                .url(baseUrl + "/v1/chat/completions")
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

                // Parse response
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
