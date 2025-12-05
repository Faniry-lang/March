package march.dev.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import march.dev.data.Tool;

public class MethodRunner {

    private final Map<String, Object> providerInstances = new HashMap<>();

    public Object execute(Tool tool, Object... args) throws Exception {
        String providerName = tool.getProviderName();
        Object providerInstance = getProviderInstance(providerName);

        Method method = tool.getMethod();
        if (args == null)
            args = new Object[] {};

        return method.invoke(providerInstance, args);
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
