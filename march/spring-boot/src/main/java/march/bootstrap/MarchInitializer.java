package march.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import march.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import march.tools.Tool;
import march.tools.Function;
import march.utils.FormatUtils;

public class MarchInitializer implements ApplicationRunner {

    private final ToolRegistry registry;
    private final Environment env;
    private final ObjectMapper mapper;

    public MarchInitializer(ToolRegistry registry, Environment env, ObjectMapper mapper) {
        this.registry = registry;
        this.env = env;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String configured = env.getProperty("march.folder");
        Path marchDir = configured != null
                ? Paths.get(configured)
                : Paths.get(System.getProperty("user.home"), ".march");

        if (!Files.exists(marchDir)) {
             try {
                 Files.createDirectories(marchDir);
             } catch (IOException e) {
                 throw new RuntimeException("Failed to create .march directory at " + marchDir, e);
             }
        }
        
        if (!Files.exists(marchDir)) {
             throw new RuntimeException("Critical Error: .march directory does not exist and could not be created at " + marchDir);
        }

        Path toolsJson = marchDir.resolve("tools.json");
        List<ToolRecord> out = new ArrayList<>();
        for (Tool t : registry.list()) {
            if (t instanceof Function f) {
                String providerClass = f.getProviderInstance() != null ? f.getProviderInstance().getClass().getName() : null;
                Method m = f.getMethodInstance();
                String methodDeclaringClass = m != null ? m.getDeclaringClass().getName() : null;
                String methodName = m != null ? m.getName() : null;

                Map<String, String> params = new LinkedHashMap<>();
                if (f.getParameters() != null) {
                    f.getParameters().forEach((k, v) -> params.put(k, FormatUtils.typeToString(v)));
                }
                String paramsJson = mapper.writeValueAsString(params);

                String returnType = f.getReturnType() != null ? FormatUtils.typeToString(f.getReturnType()) : null;

                out.add(new ToolRecord(
                        t.getName(),
                        t.getDescription(),
                        t.getClass().getName(),
                        providerClass,
                        methodDeclaringClass,
                        methodName,
                        paramsJson,
                        returnType
                ));
            } else {
                out.add(new ToolRecord(
                        t.getName(),
                        t.getDescription(),
                        t.getClass().getName(),
                        null, null, null, null, null
                ));
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(toolsJson.toFile(), out);
    }


    public static record ToolRecord(
            String name,
            String description,
            String toolClass,
            String providerClass,
            String methodDeclaringClass,
            String methodName,
            String parametersJson,
            String returnType
    ) {}
}