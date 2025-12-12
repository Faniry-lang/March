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

            if (dto.params() != null) {
                for (Map.Entry<String, String> p : dto.params().entrySet()) {
                    Map<String, Object> prop = new LinkedHashMap<>();
                    String typeStr = mapTypeNameToJsonType(p.getValue());
                    prop.put("type", typeStr);
                    properties.put(p.getKey(), prop);
                }
            }

            params.put("properties", properties);
            func.put("parameters", params);

            functions.add(func);
        }

        return functions;
    }

    private static String mapTypeNameToJsonType(String javaType) {
        if (javaType == null) return "string";
        javaType = javaType.toLowerCase();
        if (javaType.contains("int") || javaType.contains("long") || javaType.contains("integer") || javaType.contains("double")|| javaType.contains("float")) return "number";
        if (javaType.contains("bool")) return "boolean";
        if (javaType.contains("map") || javaType.contains("list") || javaType.contains("java.util")) return "object";
        return "string";
    }
}
