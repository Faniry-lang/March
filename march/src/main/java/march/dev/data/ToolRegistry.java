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
}
