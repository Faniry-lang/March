package march.dev.data;

import java.util.Map;

public record ToolDto(
    String name,
    String description,
    String providerName,
    String method,
    Map<String, String> params,
    String returnType
) {
    
}
