package march.dev.llm;

import march.dev.config.AgentConfig;

/**
 * Simple factory to create LlmClient instances from AgentConfig provider settings.
 */
public class LlmClientFactory {

    public static LlmClient createFromConfig(AgentConfig config) {
        if (config == null) return null;

        String provider = config.getProvider();
        java.util.Map<String, String> pconf = config.getProviderConfig();

        if (provider == null || provider.isEmpty()) {
            // fallback: use model to guess provider (e.g. gemini in name)
            String model = config.getModel();
            if (model != null && model.toLowerCase().contains("gemini")) {
                provider = "gemini";
            } else {
                provider = "openrouter";
            }
        }

        if (provider.equalsIgnoreCase("gemini")) {
            String apiKeyEnv = pconf != null ? pconf.get("apiKeyEnv") : null;
            String apiKey = apiKeyEnv != null ? System.getenv(apiKeyEnv) : null;
            if ((apiKey == null || apiKey.isEmpty()) && pconf != null) {
                apiKey = pconf.get("apiKey");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getenv("GEMINI_API_KEY");
            }
            String model = pconf != null && pconf.get("model") != null ? pconf.get("model") : config.getModel();
            return new GeminiClient(apiKey, model);
        }

        if (provider.equalsIgnoreCase("openrouter") || provider.equalsIgnoreCase("open_router")) {
            String apiKeyEnv = pconf != null ? pconf.get("apiKeyEnv") : null;
            String apiKey = apiKeyEnv != null ? System.getenv(apiKeyEnv) : null;
            if ((apiKey == null || apiKey.isEmpty()) && pconf != null) {
                apiKey = pconf.get("apiKey");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getenv("OPENROUTER_API_KEY");
            }
            String model = pconf != null && pconf.get("model") != null ? pconf.get("model") : config.getModel();
            return new OpenRouterClient(apiKey, model);
        }

        // OpenAI support (best-effort): if a providerConfig contains an 'apiKey' or 'apiKeyEnv',
        // attempt to construct a basic OpenAI-backed LlmClient if an OpenAiClient implementation exists.
        if (provider.equalsIgnoreCase("openai")) {
            String apiKeyEnv = pconf != null ? pconf.get("apiKeyEnv") : null;
            String apiKey = apiKeyEnv != null ? System.getenv(apiKeyEnv) : null;
            if ((apiKey == null || apiKey.isEmpty()) && pconf != null) {
                apiKey = pconf.get("apiKey");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getenv("OPENAI_API_KEY");
            }
            String model = pconf != null && pconf.get("model") != null ? pconf.get("model") : config.getModel();

            try {
                // If a local OpenAiLlmClient implementation exists, prefer it.
                Class<?> cls = Class.forName("march.dev.llm.OpenAiLlmClient");
                java.lang.reflect.Constructor<?> ctor = cls.getConstructor(String.class, String.class);
                Object inst = ctor.newInstance(apiKey, model);
                if (inst instanceof LlmClient) return (LlmClient) inst;
            } catch (ClassNotFoundException cnf) {
                // No local OpenAiLlmClient; fall through and return null so caller can fallback.
            } catch (Exception e) {
                // ignore other errors and fall through
            }
        }

        // Unknown provider: return null to let caller decide fallback
        return null;
    }
}
