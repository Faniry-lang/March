package march.tools;

import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

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

    // TODO: implement relevant tool later
    public Tool[] getRelevantTools(String query) {
        Tool[] toolAsArray = new Tool[tools.values().size()];
        int i = 0;
        for(Map.Entry<String, Tool> entry : tools.entrySet()) {
            toolAsArray[i] = entry.getValue();
            i++;
        }
        return toolAsArray;
    }
} 
