package march.dev.embedding;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.Encoding;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lazy manager for embedding tokenizer and ONNX session resources.
 * Tries to load resources from classpath using the resource path configured
 * via system property `march.embedding.modelResource` (default: models/all-MiniLM-L6-v2.onnx).
 */
public class EmbeddingManager {

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static HuggingFaceTokenizer tokenizer;
    private static OrtEnvironment env;
    private static OrtSession session;

    public static synchronized void initIfNeeded() {
        if (initialized) return;
        initialized = true;
        try {
            String tokenizerName = march.dev.config.PropertyUtils.getPreferred("march.embedding.tokenizer", "sentence-transformers/all-MiniLM-L6-v2");
            String resourcePath = march.dev.config.PropertyUtils.getPreferred("march.embedding.modelResource", "models/all-MiniLM-L6-v2.onnx");

            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerName);
            env = OrtEnvironment.getEnvironment();

            java.net.URL res = EmbeddingManager.class.getClassLoader().getResource(resourcePath);
            if (res == null) throw new IllegalStateException("Embedding model resource not found on classpath: " + resourcePath);

            String modelPath = Paths.get(res.toURI()).toString();
            session = env.createSession(modelPath, new OrtSession.SessionOptions());
            available = true;
        } catch (URISyntaxException | OrtException | RuntimeException e) {
            available = false;
            tokenizer = null;
            env = null;
            session = null;
        }
    }

    public static boolean isAvailable() {
        initIfNeeded();
        return available;
    }

    public static List<String> findTopToolsWithEmbeddings(String userRequest, List<String> tools, int n) throws OrtException {
        initIfNeeded();
        if (!available) return new ArrayList<>();
        return EmbeddingService.findTopTools(userRequest, tools, n, tokenizer, env, session);
    }
}
