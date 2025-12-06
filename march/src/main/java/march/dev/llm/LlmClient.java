package march.dev.llm;

public interface LlmClient {
    String generate(String prompt, String modelName);
}
