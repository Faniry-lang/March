package march.utils;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.LinkedHashMap;

import march.tools.ToolRegistry;
import march.annotations.MarchProvider;
import march.annotations.MarchTool;
import march.enums.ToolType;
import march.tools.Function;


public class SpringBootProviderScan implements ProviderScan {

    private ApplicationContext context;
    private ToolRegistry registry;

    public SpringBootProviderScan(ApplicationContext context, ToolRegistry registry) {
        this.context = context;
        this.registry = registry;
    }

    @PostConstruct
    public void init()  {
        scan();
    }

    @Override
    public void scan() {
        Map<String, Object> beans = context.getBeansWithAnnotation(MarchProvider.class);

        for (Object bean : beans.values()) {
            Class<?> beanClass = AopUtils.getTargetClass(bean);

            String[] beanNames = context.getBeanNamesForType(beanClass);
            String beanName = beanNames.length > 0 ? beanNames[0] : null;

            for (Method method : beanClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(MarchTool.class)) {
                    MarchTool toolAnnotation = method.getAnnotation(MarchTool.class);

                    String name = StringUtils.hasText(toolAnnotation.name()) ? toolAnnotation.name() : method.getName();
                    String description = toolAnnotation.description();
                    ToolType type = toolAnnotation.type();

					LinkedHashMap<String, Type> paramMap = new LinkedHashMap<>();
					for (Parameter p : method.getParameters()) {
						paramMap.put(p.getName(), p.getParameterizedType());
					}

                    Type returnType = method.getGenericReturnType();

                    switch (type) {
                        case FUNCTION:
                            Function toolFunction = new Function(name, description, bean, method, paramMap, returnType);
                            registry.register(toolFunction);
                            break;
                    
                        default:
                            break;
                    }
                }
            }
        }
    }
}
