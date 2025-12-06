package march.dev.chat;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class LlmService {

    private final Client geminiClient;

    public LlmService(String geminiApiKey) {
        this.geminiClient = Client.builder().apiKey(geminiApiKey).build();
    }

    public String generateContent(String prompt, String modelName) {
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
