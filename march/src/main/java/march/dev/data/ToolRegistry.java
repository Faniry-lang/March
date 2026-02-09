package march.dev.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public Map<String, Tool> getTools() {
        return tools;
    }

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public Collection<Tool> list() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    public Map<String, ToolDto> getToolsDto() {
        return getToolsForAgent(null);
    }

    public Map<String, ToolDto> getToolsForAgent(String agentId) {
        Map<String, ToolDto> toolDtos = new LinkedHashMap<>();
        for (Map.Entry<String, Tool> entry : this.tools.entrySet()) {
            Tool tool = entry.getValue();
            if (tool.getAccess().isEmpty() || (agentId != null && tool.getAccess().contains(agentId))) {
                toolDtos.put(entry.getKey(), tool.toDto());
            }
        }
        return toolDtos;
    }

    public Map<String, ToolDto> getRelevantTools(String agentId, String query, int topN) {
        Map<String, ToolDto> all = getToolsForAgent(agentId);
        if (query == null || query.isEmpty() || all.isEmpty()) return all;

        String q = query.toLowerCase();
        // If embeddings-based selection is enabled, try to use a description-based
        // similarity selector (safe fallback if heavy resources are not available).
        String useEmb = march.dev.config.PropertyUtils.getPreferred("march.useEmbeddings");
        if (useEmb != null && (useEmb.equalsIgnoreCase("true") || useEmb.equalsIgnoreCase("1"))) {
            try {
                java.util.List<String> descriptions = new java.util.ArrayList<>();
                java.util.Map<String, ToolDto> keyByDesc = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, ToolDto> e : all.entrySet()) {
                    String desc = e.getKey() + ": " + e.getValue().description();
                    descriptions.add(desc);
                    keyByDesc.put(desc, e.getValue());
                }

                // Prefer the full embedding-based selector (uses tokenizer + ONNX if available)
                java.util.List<String> tops = new java.util.ArrayList<>();
                try {
                    if (march.dev.embedding.EmbeddingManager.isAvailable()) {
                        tops = march.dev.embedding.EmbeddingManager.findTopToolsWithEmbeddings(query, descriptions, topN);
                    } else {
                        // fallback to simple text overlap if embeddings not available
                        tops = march.dev.embedding.EmbeddingService.findTopToolsSimple(query, descriptions, topN);
                    }
                } catch (Exception ex) {
                    // fallback to simple selector on any error
                    tops = march.dev.embedding.EmbeddingService.findTopToolsSimple(query, descriptions, topN);
                }

                Map<String, ToolDto> result = new LinkedHashMap<>();
                for (String d : tops) {
                    ToolDto dto = keyByDesc.get(d);
                    if (dto != null) result.put(dto.name(), dto);
                }
                return result;
            } catch (Exception ex) {
                // fall through to keyword scoring
            }
        }

        // simple keyword scoring on name+description (fallback)
        java.util.List<java.util.Map.Entry<String, ToolDto>> list = new java.util.ArrayList<>(all.entrySet());
        list.sort((a, b) -> {
            String aText = (a.getValue().name() + " " + a.getValue().description()).toLowerCase();
            String bText = (b.getValue().name() + " " + b.getValue().description()).toLowerCase();
            int aScore = scoreTextMatch(aText, q);
            int bScore = scoreTextMatch(bText, q);
            return Integer.compare(bScore, aScore);
        });

        Map<String, ToolDto> result = new LinkedHashMap<>();
        int count = 0;
        for (java.util.Map.Entry<String, ToolDto> e : list) {
            if (count >= topN) break;
            result.put(e.getKey(), e.getValue());
            count++;
        }

        return result;
    }

    private static int scoreTextMatch(String text, String query) {
        int score = 0;
        for (String token : query.split("\\s+")) {
            if (token.isEmpty()) continue;
            if (text.contains(token)) score += 2;
            if (text.contains(token + "s")) score += 1;
        }
        return score;
    }
}
