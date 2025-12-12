package march.dev.llm;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.logging.HttpLoggingInterceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Duration;

public class OpenRouterClient implements LlmClient {

    private final String apiKey;
    private final String model;
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final int maxRetries;
    private final long initialBackoffMs;

    public OpenRouterClient(String apiKey, String model, march.dev.config.AgentConfig config) {
        this.apiKey = apiKey;
        this.model = model;
        this.mapper = new ObjectMapper();

        int connectSec = 15;
        int readSec = 60;
        int writeSec = 60;
        int callSec = 120;
        int retries = 3;
        long backoff = 500;

        try {
            if (config != null) {
                connectSec = config.getConnectTimeoutSec();
                readSec = config.getReadTimeoutSec();
                writeSec = config.getWriteTimeoutSec();
                callSec = config.getCallTimeoutSec();
                retries = Math.max(1, config.getMaxLlmRetries());
                backoff = config.getLlmInitialBackoffMs();
            }
        } catch (Exception e) {
            // use defaults
        }

        this.maxRetries = retries;
        this.initialBackoffMs = backoff;

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(System.out::println);
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

        this.client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(Duration.ofSeconds(connectSec))
            .callTimeout(Duration.ofSeconds(callSec))
            .readTimeout(Duration.ofSeconds(readSec))
            .writeTimeout(Duration.ofSeconds(writeSec))
            // .addInterceptor(loggingInterceptor)
            .build();
    }

    @Override
    public String generate(String prompt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);

        root.set("messages", messages);

        String body;
        try {
            body = mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            System.out.println("Error serializing request body: " + e.getMessage());
            throw new RuntimeException(e);
        }

        Request request = new Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("HTTP-Referer", "march-framework://local")
            .addHeader("X-Title", "March Framework")
            .post(RequestBody.create(body, MediaType.parse("application/json")))
            .build();

        int maxRetries = this.maxRetries;
        long backoffMs = this.initialBackoffMs;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                String respBody = response.body() != null ? response.body().string() : "";

                if (code == 401 || code == 403) {
                    String hint = "";
                    if (apiKey == null || apiKey.isEmpty()) {
                        hint = " (no API key provided; ensure OPENROUTER_API_KEY is set or providerConfig.apiKey/apiKeyEnv is correct)";
                    }
                    throw new RuntimeException("OpenRouter authentication error: HTTP " + code + "." + hint + " Response: " + respBody);
                }

                if (code >= 500 || code == 429) {
                    // transient server error or rate limit: may retry
                    System.out.println("OpenRouter transient error (HTTP " + code + "). Attempt " + attempt + " of " + maxRetries + ". Response: " + respBody);
                    if (attempt == maxRetries) {
                        throw new RuntimeException("OpenRouter transient error after " + maxRetries + " attempts: " + code + " " + respBody);
                    }
                    // backoff with jitter
                    try {
                        long jitter = (long) (Math.random() * 200);
                        Thread.sleep(backoffMs + jitter);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    backoffMs *= 2;
                    continue;
                }

                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenRouter error: HTTP " + code + " Response: " + respBody);
                }

                String json = respBody != null ? respBody : "";
                if (json.isEmpty()) {
                    throw new RuntimeException("Empty response from OpenRouter API");
                }

                try {
                    return mapper.readTree(json)
                                 .get("choices")
                                 .get(0)
                                 .get("message")
                                 .get("content")
                                 .asText();
                } catch (Exception parseEx) {
                    throw new RuntimeException("Failed to parse OpenRouter response JSON: " + parseEx.getMessage() + "\nRaw: " + json, parseEx);
                }
            } catch (IOException e) {
                // network issue: retry unless we've exhausted attempts
                System.out.println("Network/IO error during OpenRouter request: " + e.getMessage() + ". Attempt " + attempt + " of " + maxRetries + ".");
                if (attempt == maxRetries) {
                    throw new RuntimeException("OpenRouter network error after " + maxRetries + " attempts: " + e.getMessage(), e);
                }
                try {
                    long jitter = (long) (Math.random() * 200);
                    Thread.sleep(backoffMs + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                backoffMs *= 2;
                continue;
            }
        }

        throw new RuntimeException("Unreachable code in OpenRouterClient.generate");
    }
}
