package march.dev.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.Encoding;
import java.util.PriorityQueue;
import java.util.AbstractMap;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class EmbeddingService {

    private static float cosineSimilarity(float[] vecA, float[] vecB) {
        float dot = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < vecA.length; i++) {
            dot += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        return dot / ((float)(Math.sqrt(normA) * Math.sqrt(normB)) + 1e-8f);
    }

    public static float[] getSentenceEmbedding(String text,
                                            HuggingFaceTokenizer tokenizer,
                                            OrtEnvironment env,
                                            OrtSession session) throws OrtException {

        Encoding tokens = tokenizer.encode(text);
        long[] inputIds = tokens.getIds();
        long[] attentionMask = tokens.getAttentionMask();
        long[][] inputIds2d = new long[][]{inputIds};
        long[][] attentionMask2d = new long[][]{attentionMask};
        long[][] tokenTypeIds2d = new long[1][inputIds.length];

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(env, inputIds2d));
        inputs.put("attention_mask", OnnxTensor.createTensor(env, attentionMask2d));
        inputs.put("token_type_ids", OnnxTensor.createTensor(env, tokenTypeIds2d));

        try (OrtSession.Result result = session.run(inputs)) {
            float[][][] embeddings3d = (float[][][]) result.get(0).getValue();
            float[][] tokenEmbeddings = embeddings3d[0]; // first batch
            int seqLen = tokenEmbeddings.length;
            int hiddenSize = tokenEmbeddings[0].length;

            float[] sentenceEmbedding = new float[hiddenSize];
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < hiddenSize; j++) {
                    sentenceEmbedding[j] += tokenEmbeddings[i][j];
                }
            }
            for (int j = 0; j < hiddenSize; j++) {
                sentenceEmbedding[j] /= seqLen;
            }

            return sentenceEmbedding;
        }
    }

    public static List<String> findTopTools(String userRequest,
                                            List<String> tools,
                                            int n,
                                            HuggingFaceTokenizer tokenizer,
                                            OrtEnvironment env,
                                            OrtSession session) throws OrtException {

        float[] requestEmbedding = getSentenceEmbedding(userRequest, tokenizer, env, session);

        // Compute similarity for each tool
        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<>(
                Map.Entry.comparingByValue() // min-heap by similarity
        );

        for (String tool : tools) {
            float[] toolEmbedding = getSentenceEmbedding(tool, tokenizer, env, session);
            float sim = cosineSimilarity(requestEmbedding, toolEmbedding);

            pq.offer(new AbstractMap.SimpleEntry<>(tool, sim));
            if (pq.size() > n) pq.poll(); // keep only top n
        }

        // Extract top n tools in descending order
        List<String> topTools = new ArrayList<>();
        List<Map.Entry<String, Float>> tmp = new ArrayList<>();
        while (!pq.isEmpty()) tmp.add(pq.poll());
        Collections.reverse(tmp);
        for (Map.Entry<String, Float> e : tmp) topTools.add(e.getKey());

        return topTools;
    }
}   