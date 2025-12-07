package march.dev.process;

import java.util.Map;

public class Message {
    String role;
    String content;
    String toolName;
    Map<String, Object> toolInput;

    public Message(String role, String content, String toolName, Map<String, Object> toolInput) {
        this.role = role;
        this.content = content;
        this.toolName = toolName;
        this.toolInput = toolInput;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getToolName() {
        return toolName;
    }
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    public Map<String, Object> getToolInput() {
        return toolInput;
    }
    public void setToolInput(Map<String, Object> toolInput) {
        this.toolInput = toolInput;
    }
}
