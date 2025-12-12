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

        // simple keyword scoring on name+description
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
