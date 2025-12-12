package march.dev.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import march.dev.process.LlmResponse;

/**
 * Lightweight mock LLM client for local/offline testing.
 * Returns simple deterministic JSON responses based on prompt heuristics.
 */
public class MockLlmClient implements LlmClient {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String generate(String prompt) {
        try {
            // Basic heuristics to return usable LlmResponse JSON for ChatSession parsing.
            LlmResponse resp = null;

            String lp = prompt == null ? "" : prompt.toLowerCase();

            if (lp.contains("fibonacci")) {
                // return a function-like response emulating a model that computed fibonacci
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("n", 12);
                args.put("result", new int[] {0,1,1,2,3,5,8,13,21,34,55,89});
                resp = new LlmResponse(0, "computed fibonacci sequence (mock)", "Here is the sequence.", true,
                        "computeFibonacci", args);
            } else if (lp.contains("statistical") || lp.contains("stat") || lp.contains("mean") || lp.contains("median")) {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("mean", 15.6);
                args.put("median", 16);
                args.put("stdev", 3.4);
                resp = new LlmResponse(0, "performed basic stats (mock)", "Statistics computed.", true,
                        "analyzeStatistics", args);
            } else if (lp.contains("translate") || lp.contains("bonjour")) {
                resp = new LlmResponse(0, "translated text (mock)", "Translated: 'Hello, how are you today?'. Formal: 'Good day, how do you do?'",
                        false, null, null);
            } else if (lp.contains("matrix") || lp.contains("multiply") || lp.contains("10x10")) {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                int[][] m = new int[10][10];
                for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) m[i][j] = (i * 10 + j + 1);
                args.put("matrix", m);
                resp = new LlmResponse(0, "matrix computed (mock)", "Matrix multiplication result included.", true,
                        "multiplyMatrix", args);
            } else if (lp.contains("summarize")) {
                resp = new LlmResponse(0, "summary (mock)", "- Bullet 1\n- Bullet 2\n- Bullet 3\n- Bullet 4\n- Bullet 5",
                        false, null, null);
            } else {
                // default: simple conversational answer (non-function call)
                resp = new LlmResponse(0, "mock thought: returning simple reply",
                        "This is a mock response. Please use a real LLM for production.", false, null, null);
            }

            return mapper.writeValueAsString(mapper.readTree(resp.toJson()));
        } catch (Exception e) {
            // Fallback: minimal JSON string
            return "{\"step\":0,\"modelThought\":\"mock error\",\"toolDetails\":{\"functionCall\":false},\"modelAnswer\":\"mock fallback\"}";
        }
    }
}
