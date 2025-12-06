package march.dev.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;
import march.dev.history.HistoryService;
import java.util.List;
import java.util.Map;

@LlmContextProvider
public class HistoryTools {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @LlmTool(name = "getHistorySummaries", description = "Returns a list of summaries of past conversations. Use this to find relevant history.")
    public List<Map<String, String>> getHistorySummaries(String agentId) {
        HistoryService historyService = new HistoryService(agentId, objectMapper);
        return historyService.getSummaries();
    }

    @LlmTool(name = "getHistoryById", description = "Retrieves the full content of a specific history chunk by its ID.")
    public String getHistoryById(String agentId, String historyId) {
        HistoryService historyService = new HistoryService(agentId, objectMapper);
        String content = historyService.getHistoryContent(historyId);
        if (content != null) {
            return "[EPHEMERAL]" + content + "[/EPHEMERAL]";
        }
        return "History not found.";
    }
}
