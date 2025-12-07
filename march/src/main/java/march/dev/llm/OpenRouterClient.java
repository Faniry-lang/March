package march.dev.llm;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenRouterClient implements LlmClient {

    private final String apiKey;
    private final String model;

    public OpenRouterClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        String chosenModel = this.model;

        OkHttpClient client = new OkHttpClient();

        String body = """
            {
              "model": "%s",
              "messages": [
                { "role": "user", "content": "%s" }
              ]
            }
        """.formatted(chosenModel, prompt);

        Request request = new Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("HTTP-Referer", "march-framework://local")
            .addHeader("X-Title", "March Framework")
            .post(RequestBody.create(body, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("OpenRouter error: " + response.code() + " " + response.body().string());
            }

            String json = response.body().string();
            return new ObjectMapper()
                .readTree(json)
                .get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

