package march.dev.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import march.dev.data.ToolDto;

public class FunctionDefinitionBuilder {

    public static List<Map<String, Object>> buildFromToolDtos(Map<String, ToolDto> tools) {
        List<Map<String, Object>> functions = new ArrayList<>();
        if (tools == null) return functions;

        for (ToolDto dto : tools.values()) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", dto.name());
            func.put("description", dto.description());

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("type", "object");
            Map<String, Object> properties = new LinkedHashMap<>();
            java.util.List<String> required = new java.util.ArrayList<>();

            if (dto.params() != null) {
                for (Map.Entry<String, String> p : dto.params().entrySet()) {
                    String paramName = p.getKey();
                    String rawType = p.getValue();
                    Map<String, Object> prop = new LinkedHashMap<>();

                    // detect array types
                    if (rawType != null && rawType.endsWith("[]")) {
                        prop.put("type", "array");
                        String itemType = mapTypeNameToJsonType(rawType.substring(0, rawType.length() - 2));
                        prop.put("items", Map.of("type", itemType));
                    } else if (rawType != null && rawType.toLowerCase().contains("list<")) {
                        prop.put("type", "array");
                        // attempt to guess inner type
                        int start = rawType.indexOf('<');
                        int end = rawType.indexOf('>');
                        String inner = "string";
                        if (start > 0 && end > start) {
                            inner = mapTypeNameToJsonType(rawType.substring(start + 1, end));
                        }
                        prop.put("items", Map.of("type", inner));
                    } else {
                        prop.put("type", mapTypeNameToJsonType(rawType));
                    }

                    properties.put(paramName, prop);
                    required.add(paramName);
                }
            }

            params.put("properties", properties);
            if (!required.isEmpty()) params.put("required", required);
            func.put("parameters", params);

            functions.add(func);
        }

        return functions;
    }

    private static String mapTypeNameToJsonType(String javaType) {
        if (javaType == null) return "string";
        javaType = javaType.toLowerCase();
        if (javaType.contains("int") || javaType.contains("long") || javaType.contains("integer")) return "integer";
        if (javaType.contains("double") || javaType.contains("float") || javaType.contains("number")) return "number";
        if (javaType.contains("bool")) return "boolean";
        if (javaType.contains("map") || javaType.contains("list") || javaType.contains("java.util")) return "object";
        return "string";
    }
}
