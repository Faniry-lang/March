package march.agent;

public interface LlmClient {
    String call(String prompt, String apiKey) throws Exception;
}
    