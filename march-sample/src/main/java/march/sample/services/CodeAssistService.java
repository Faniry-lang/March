package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class CodeAssistService {

    @LlmTool(description = "Suggest a refactor for a small Java method and explain changes")
    public String suggestRefactor(String javaMethod) {
        if (javaMethod == null || javaMethod.isEmpty()) return "No method provided";
        // Very simple heuristic: suggest extracting repeated code, use descriptive names
        StringBuilder sb = new StringBuilder();
        sb.append("Refactor suggestions:\n");
        sb.append("- Use descriptive variable names.\n");
        sb.append("- Add parameter validation at the top.\n");
        sb.append("- Prefer using standard library helpers where possible.\n");
        sb.append("Example rewrite: (pseudo)\n");
        sb.append(javaMethod).append("\n");
        return sb.toString();
    }
}
