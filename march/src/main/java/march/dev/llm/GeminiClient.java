package march.dev.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiClient implements LlmClient {

    private final Client geminiClient;

    public GeminiClient(String apiKey) {
        this.geminiClient = Client.builder().apiKey(apiKey).build();
    }

    @Override
    public String generate(String prompt, String modelName) {
        String responseText = "";
        try {
            GenerateContentResponse response = geminiClient.models.generateContent(modelName, prompt, null);
            responseText = response.text();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            responseText = "Une erreur s'est produite " + e.getMessage();
        }
        return responseText;
    }
}
