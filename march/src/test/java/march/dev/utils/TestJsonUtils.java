package march.dev.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestJsonUtils {

    @Test
    public void testCleanFromCodeFence() throws Exception {
        String raw = "Here is the result:\n```json\n{ \"step\":1, \"modelAnswer\": \"ok\" }\n```";
        String cleaned = JsonUtils.cleanLlmResponse(raw);
        assertNotNull(cleaned);
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.readTree(cleaned).has("step"));
    }

    @Test
    public void testCleanFromBackticks() throws Exception {
        String raw = "`{ \"step\":2, \"modelAnswer\": \"x\" }`";
        String cleaned = JsonUtils.cleanLlmResponse(raw);
        assertNotNull(cleaned);
        ObjectMapper m = new ObjectMapper();
        assertEquals(2, m.readTree(cleaned).get("step").asInt());
    }

    @Test
    public void testExtractBalancedJson() throws Exception {
        String raw = "Some text before { \"a\": { \"b\": [1,2,3] } } some trailing text";
        String cleaned = JsonUtils.cleanLlmResponse(raw);
        assertNotNull(cleaned);
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.readTree(cleaned).has("a"));
    }
}
