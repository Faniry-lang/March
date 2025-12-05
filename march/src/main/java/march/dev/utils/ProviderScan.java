package march.dev.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;
import march.dev.data.Tool;

public class ProviderScan {

    public static List<Tool> scanTools(String packageName) throws Exception {
        
        List<Tool> tools = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Package not found: " + packageName);
        }

        File directory = new File(resource.toURI());
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(LlmContextProvider.class)) {
                    String providerName = clazz.getName();
                    for(Method method : clazz.getDeclaredMethods()) {
                        if(method.isAnnotationPresent(LlmTool.class)) {
                            LlmTool toolAnnotation = method.getAnnotation(LlmTool.class);
                            String name = method.getName();
                            if(toolAnnotation.name() != null && !toolAnnotation.name().isEmpty()) {
                                name = toolAnnotation.name();
                            }

                            String description = toolAnnotation.description();

                            Map<String, Type> paramMap = new LinkedHashMap<>();
                            for (Parameter p : method.getParameters()) {
                                paramMap.put(p.getName(), p.getParameterizedType());
                            }

                            Type returnType = method.getGenericReturnType();

                            Tool tool = new Tool(name, description, providerName, method, paramMap, returnType);    
                            tools.add(tool);                    
                        }       
                    }
                }
            }
        }
        
        return tools;
    }
}


