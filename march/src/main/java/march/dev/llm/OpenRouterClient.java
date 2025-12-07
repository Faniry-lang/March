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

    public OpenRouterClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.mapper = new ObjectMapper();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(System.out::println);
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        this.client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(Duration.ofSeconds(15))
            .callTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .writeTimeout(Duration.ofSeconds(60))
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

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String respBody = response.body() != null ? response.body().string() : "empty response";
                System.out.println("OpenRouter API returned error code " + response.code() + ": " + respBody);
                throw new RuntimeException("OpenRouter error: " + response.code() + " " + respBody);
            }

            String json = response.body() != null ? response.body().string() : "";
            if (json.isEmpty()) {
                throw new RuntimeException("Empty response from OpenRouter API");
            }

            return mapper.readTree(json)
                         .get("choices")
                         .get(0)
                         .get("message")
                         .get("content")
                         .asText();
        } catch (IOException e) {
            System.out.println("Network/IO error during request: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
