package march.dev.utils;

public class JsonUtils {
    public static String cleanLlmResponse(String llmResponse) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return "";
        }
        int startIndex = llmResponse.indexOf('{');        
        int endIndex = llmResponse.lastIndexOf('}'); 
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return llmResponse.substring(startIndex, endIndex + 1).trim();
        }
        return ""; 
    }
}