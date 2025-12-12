package march.dev.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String cleanLlmResponse(String llmResponse) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return "";
        }

        String candidateWhole = llmResponse.trim();
        try {
            MAPPER.readTree(candidateWhole);
            return candidateWhole;
        } catch (Exception e) {
            // ignore and try extraction heuristics
        }

        // Try to extract JSON from common code-fence wrappers (```json ... ```) first
        try {
            java.util.regex.Pattern fence = java.util.regex.Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = fence.matcher(llmResponse);
            if (m.find()) {
                String inside = m.group(1).trim();
                try {
                    MAPPER.readTree(inside);
                    return inside;
                } catch (Exception ex) {
                    // fallthrough to further extraction
                }
            }
        } catch (Exception ex) {
            // ignore
        }

        // Remove surrounding backticks or code fences if present
        String cleaned = candidateWhole;
        // remove triple backticks wrappers
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            cleaned = cleaned.substring(3, cleaned.length() - 3).trim();
        }
        // strip single backtick wrappers
        while (cleaned.startsWith("`") && cleaned.endsWith("`") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        // remove any remaining inline backticks
        cleaned = cleaned.replace('`', ' ').trim();

        // If the whole cleaned response is valid JSON, return it
        try {
            MAPPER.readTree(cleaned);
            return cleaned;
        } catch (Exception ex) {
            // continue to scanning
        }

        // Scan for all balanced JSON objects/arrays and try parsing each (return first that parses)
        int len = candidateWhole.length();
        for (int i = 0; i < len; i++) {
            char start = candidateWhole.charAt(i);
            if (start != '{' && start != '[') continue;

            int depth = 0;
            for (int j = i; j < len; j++) {
                char c = candidateWhole.charAt(j);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;

                if (depth == 0) {
                    String candidate = candidateWhole.substring(i, j + 1).trim();
                    // clean candidate from backticks
                    candidate = candidate.replace('`', ' ').trim();
                    try {
                        MAPPER.readTree(candidate);
                        return candidate;
                    } catch (Exception e) {
                        // try next balanced region
                        break;
                    }
                }
            }
        }

        // Nothing found
        return "";
    }
}