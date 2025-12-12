package march.dev.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import march.dev.data.Tool;

public class MethodRunner {
    private final Map<String, Object> providerInstances = new HashMap<>();
    private final String cacheKeyPrefix;
    private final march.dev.utils.ToolResultCache cache;

    public MethodRunner() {
        this.cacheKeyPrefix = "";
        this.cache = new march.dev.utils.ToolResultCache(5 * 60 * 1000);
    }

    public MethodRunner(String cacheKeyPrefix) {
        this(cacheKeyPrefix, new march.dev.utils.ToolResultCache(5 * 60 * 1000));
    }

    public MethodRunner(String cacheKeyPrefix, march.dev.utils.ToolResultCache cache) {
        this.cacheKeyPrefix = cacheKeyPrefix != null ? cacheKeyPrefix : "";
        this.cache = cache != null ? cache : new march.dev.utils.ToolResultCache(5 * 60 * 1000);
    }

    public Object execute(Tool tool, Object... args) throws Exception {
        String providerName = tool.getProviderName();
        Object providerInstance = getProviderInstance(providerName);

        Method method = tool.getMethod();
        if (args == null)
            args = new Object[] {};
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String key = tool.getName() + ":" + mapper.writeValueAsString(args == null ? new Object[0] : args);
            String namespacedKey = (this.cacheKeyPrefix == null || this.cacheKeyPrefix.isEmpty()) ? key : (this.cacheKeyPrefix + ":" + key);
            String cached = this.cache.get(namespacedKey);
            if (cached != null) {
                java.lang.reflect.Type returnType = tool.getReturnType();
                if (returnType != null) {
                    try {
                        Object obj = mapper.readValue(cached, mapper.constructType(returnType));
                        return obj;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    return cached;
                }
            }

            Object result = method.invoke(providerInstance, args);

            try {
                String resJson = mapper.writeValueAsString(result);
                this.cache.put(namespacedKey, resJson);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        } catch (IllegalArgumentException iae) {
            return method.invoke(providerInstance, args);
        }
    }

    private Object getProviderInstance(String providerName) throws Exception {
        if (providerInstances.containsKey(providerName)) {
            return providerInstances.get(providerName);
        }

        Class<?> clazz = Class.forName(providerName);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        providerInstances.put(providerName, instance);
        return instance;
    }
}
