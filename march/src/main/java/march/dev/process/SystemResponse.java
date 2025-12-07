package march.dev.process;

import java.util.Objects;

import march.dev.utils.TemplateUtils;

public class SystemResponse implements Response {
    int step;
    String toolName;
    String toolOutput;
    Object[] toolArgs;

    @Override
    public String toXml() throws Exception {
        String content = this.contentToXml();
        String messageTemplate = TemplateUtils.getMessageTemplate();
        TemplateUtils.replace(messageTemplate, "<content-placeholder/>", content);
        TemplateUtils.replace(messageTemplate, "<role-placeholder/>", "system");
        return messageTemplate;
    }

    public String contentToXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<system-response>");
        sb.append("<step>").append(this.step).append("</step>");        
        sb.append("<toolName>").append(this.toolName).append("</toolName>");
        sb.append("<toolOutput>");
        if (this.toolOutput != null) {
            sb.append(this.toolOutput);
        }
        sb.append("</toolOutput>");
        sb.append("<toolArgs>");
        if (this.toolArgs != null) {
            for (Object arg : this.toolArgs) {
                sb.append("<argument>");
                sb.append(Objects.toString(arg, "null"));
                sb.append("</argument>");
            }
        }
        sb.append("</toolArgs>");
        sb.append("</system-response>");
        return sb.toString();
    }

    public SystemResponse(int step, String toolName, String toolOutput, Object[] toolArgs) {
        this.step = step;
        this.toolName = toolName;
        this.toolOutput = toolOutput;
        this.toolArgs = toolArgs;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolOutput() {
        return toolOutput;
    }

    public void setToolOutput(String toolOutput) {
        this.toolOutput = toolOutput;
    }

    public Object[] getToolArgs() {
        return toolArgs;
    }

    public void setToolArgs(Object[] toolArgs) {
        this.toolArgs = toolArgs;
    }
}
