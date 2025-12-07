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

    public String archive(String historyChunk) {
        String summary = llmClient.generate("Summarize the following conversation history concisely, return only text, not json or any other format, just text, do not use quotes or backtricks:\n" + historyChunk);
        String id = UUID.randomUUID().toString();

        try {
            Files.writeString(Paths.get(historyDir + id + ".txt"), historyChunk);

            File indexFile = new File(historyDir + "index.json");
            List<Map<String, String>> index = objectMapper.readValue(indexFile,
                    new TypeReference<List<Map<String, String>>>() {
                    });

            Map<String, String> entry = new HashMap<>();
            entry.put("id", id);
            entry.put("summary", summary);
            entry.put("timestamp", String.valueOf(System.currentTimeMillis()));
            entry.put("order", String.valueOf(index.size() + 1));
            index.add(entry);

            objectMapper.writeValue(indexFile, index);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return id;
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
            return Files.readString(Paths.get(historyDir + id + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getSummaryById(String id) {
        List<Map<String, String>> summaries = getSummaries();
        for (Map<String, String> entry : summaries) {
            if (entry.get("id").equals(id)) {
                return entry.get("summary");
            }
        }
        return null;
    }
}
