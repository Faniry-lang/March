package march.dev.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String cleanLlmResponse(String llmResponse) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return "";
        }

        try {
            MAPPER.readTree(llmResponse);
            return llmResponse.trim();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int lastObjStart = -1;
        int lastObjEnd = -1;
        int depthObj = 0;
        int objStart = -1;

        int lastArrStart = -1;
        int lastArrEnd = -1;
        int depthArr = 0;
        int arrStart = -1;

        for (int i = 0; i < llmResponse.length(); i++) {
            char c = llmResponse.charAt(i);

            if (c == '{') {
                if (depthObj == 0) objStart = i;
                depthObj++;
            } else if (c == '}') {
                depthObj--;
                if (depthObj == 0 && objStart != -1) {
                    lastObjStart = objStart;
                    lastObjEnd = i;
                    objStart = -1;
                }
            } else if (c == '[') {
                if (depthArr == 0) arrStart = i;
                depthArr++;
            } else if (c == ']') {
                depthArr--;
                if (depthArr == 0 && arrStart != -1) {
                    lastArrStart = arrStart;
                    lastArrEnd = i;
                    arrStart = -1;
                }
            }
        }

        String candidate = null;
        if (lastObjStart != -1 && lastObjEnd != -1) {
            candidate = llmResponse.substring(lastObjStart, lastObjEnd + 1).trim();
        }

        if ((lastArrStart != -1 && lastArrEnd != -1)
                && (candidate == null || lastArrEnd > lastObjEnd)) {
            candidate = llmResponse.substring(lastArrStart, lastArrEnd + 1).trim();
        }

        if (candidate != null) {
            try {
                MAPPER.readTree(candidate);
                return candidate;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "";
    }
}