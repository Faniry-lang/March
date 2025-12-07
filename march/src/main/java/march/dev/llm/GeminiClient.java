package march.dev.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiClient implements LlmClient {

    private final Client geminiClient;
    private final String model;

    public GeminiClient(String apiKey, String model) {
        this.geminiClient = Client.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        String responseText = "";
        try {
            GenerateContentResponse response = geminiClient.models.generateContent(this.model, prompt, null);
            responseText = response.text();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            responseText = "Une erreur s'est produite " + e.getMessage();
        }
        return responseText;
    }
}
