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
    private String historyDir;

    public HistoryService(String agentId, LlmClient llmClient, ObjectMapper objectMapper) {
        this.agentId = agentId;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        // Decide where to store history:
        // 1) honor system property `march.history.dir`
        // 2) prefer local project resources `src/main/resources/march-history/<agentId>/` if writable
        // 3) fallback to user home `~/.march/history/<agentId>/`
        String explicit = march.dev.config.PropertyUtils.getPreferred("march.history.dir");
        if (explicit != null && !explicit.isBlank()) {
            this.historyDir = explicit.endsWith("/") ? explicit + agentId + "/" : explicit + "/" + agentId + "/";
        } else {
            Path projectPath = Paths.get("src", "main", "resources", "march-history", agentId);
            try {
                Files.createDirectories(projectPath);
                this.historyDir = projectPath.toString() + "/";
            } catch (Exception e) {
                String base = System.getProperty("user.home") + "/.march/history/";
                this.historyDir = base + agentId + "/";
            }
        }
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

    /**
     * Write a transient history file (used for active chat sessions). This file
     * is intended for internal use and may be removed when the session closes.
     */
    public void writeTransientHistory(String historyJson) {
        try {
            Path p = Paths.get(historyDir + "history.json");
            Files.createDirectories(p.getParent());
            Files.writeString(p, historyJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Delete the transient history file if it exists. */
    public void deleteTransientHistory() {
        try {
            Files.deleteIfExists(Paths.get(historyDir + "history.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String archive(String historyChunk) {
        if (this.llmClient == null) {
            throw new IllegalStateException("HistoryService.archive requires a non-null LlmClient (summarization). Provide an LlmClient when constructing HistoryService.");
        }

        String summary = llmClient.generate("Summarize the following conversation history concisely, return only text, not json or any other format, just text, do not use quotes or backtricks:\n" + historyChunk);
        String id = UUID.randomUUID().toString();

        try {
            // Store the raw chunk inside a JSON file to avoid writing raw XML/HTML into source tree.
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("id", id);
            payload.put("content", historyChunk);
            payload.put("timestamp", System.currentTimeMillis());

            File chunkFile = new File(historyDir + id + ".json");
            objectMapper.writeValue(chunkFile, payload);

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
            File chunkFile = new File(historyDir + id + ".json");
            if (!chunkFile.exists()) return null;
            java.util.Map<?, ?> payload = objectMapper.readValue(chunkFile, java.util.Map.class);
            Object content = payload.get("content");
            return content != null ? content.toString() : null;
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
