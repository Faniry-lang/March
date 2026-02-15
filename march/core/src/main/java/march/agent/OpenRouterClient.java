package march.agent;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.time.Duration;

public class OpenRouterClient implements LlmClient {

    private final OkHttpClient client;
    private final int maxRetries;
    private final long initialBackoffMs;

    public OpenRouterClient() {

        int connectSec = 15;
        int readSec = 60;
        int writeSec = 60;
        int callSec = 120;
        int retries = 3;
        long backoff = 500;

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
            .build();
    }

    @Override
    public String call(String body, String apiKey) throws Exception {

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
                    System.out.println("OpenRouter transient error (HTTP " + code + "). Attempt " + attempt + " of " + maxRetries + ". Response: " + respBody);
                    if (attempt == maxRetries) {
                        throw new RuntimeException("OpenRouter transient error after " + maxRetries + " attempts: " + code + " " + respBody);
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

                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenRouter error: HTTP " + code + " Response: " + respBody);
                }

                String json = respBody != null ? respBody : "";
                if (json.isEmpty()) {
                    throw new RuntimeException("Empty response from OpenRouter API");
                }

                try {
                    return json;
                } catch (Exception parseEx) {
                    throw new RuntimeException("Failed to parse OpenRouter response JSON: " + parseEx.getMessage() + "\nRaw: " + json, parseEx);
                }
            } catch (IOException e) {
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

