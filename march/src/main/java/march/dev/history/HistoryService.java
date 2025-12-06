package march.dev.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import march.dev.llm.LlmClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HistoryService {
    private final String agentId;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final String historyDir;

    public HistoryService(String agentId, LlmClient llmClient, ObjectMapper objectMapper) {
        this.agentId = agentId;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.historyDir = "src/main/resources/march-history/" + agentId + "/";
        createHistoryDir();
    }

    public HistoryService(String agentId, ObjectMapper objectMapper) {
        this(agentId, null, objectMapper);
    }

    private void createHistoryDir() {
        try {
            Files.createDirectories(Paths.get(historyDir));
            File indexFile = new File(historyDir + "index.json");
            if (!indexFile.exists()) {
                objectMapper.writeValue(indexFile, new ArrayList<Map<String, String>>());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void archive(String historyChunk) {
        String summary = llmClient.generate("Summarize the following conversation history concisely:\n" + historyChunk,
                "gemini-2.5-flash");
        String id = UUID.randomUUID().toString();

        try {
            Files.writeString(Paths.get(historyDir + id + ".json"), historyChunk);

            File indexFile = new File(historyDir + "index.json");
            List<Map<String, String>> index = objectMapper.readValue(indexFile,
                    new TypeReference<List<Map<String, String>>>() {
                    });

            Map<String, String> entry = new HashMap<>();
            entry.put("id", id);
            entry.put("summary", summary);
            entry.put("timestamp", String.valueOf(System.currentTimeMillis()));
            index.add(entry);

            objectMapper.writeValue(indexFile, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> getSummaries() {
        try {
            File indexFile = new File(historyDir + "index.json");
            if (indexFile.exists()) {
                return objectMapper.readValue(indexFile, new TypeReference<List<Map<String, String>>>() {
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public String getHistoryContent(String id) {
        try {
            return Files.readString(Paths.get(historyDir + id + ".json"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
