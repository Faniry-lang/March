package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class TextProcessingService {

    @LlmTool(description = "Summarize a long text into the requested number of bullet points")
    public String summarize(String text, int bullets) {
        if (text == null || text.isEmpty()) return "No text provided";
        String[] parts = text.split("\\. ");
        StringBuilder sb = new StringBuilder();
        int n = Math.min(bullets, Math.max(1, parts.length));
        for (int i = 0; i < n; i++) {
            sb.append("- ").append(parts[i].trim());
            if (!parts[i].endsWith(".")) sb.append(".");
            sb.append("\n");
        }
        return sb.toString();
    }

    @LlmTool(description = "Extract named entities (simple heuristic) from a short text")
    public String extractEntities(String text) {
        if (text == null || text.isEmpty()) return "[]";
        String[] tokens = text.split("[ ,]+");
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String t : tokens) {
            if (t.length() > 2 && Character.isUpperCase(t.charAt(0))) {
                if (!first) sb.append(", ");
                sb.append('"').append(t.replaceAll("\\W","")).append('"');
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
