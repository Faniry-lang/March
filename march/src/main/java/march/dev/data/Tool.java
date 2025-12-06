package march.dev.data;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Tool {
    String name;
    String description;
    String providerName;
    Method method;
    Map<String, Type> params;
    Type returnType;
    List<String> access = new ArrayList<>();

    public Tool() {
    }

    public Tool(String name, String description, String providerName, Method method, Map<String, Type> params,
            Type returnType, List<String> access) {
        this.name = name;
        this.description = description;
        this.providerName = providerName;
        this.method = method;
        this.params = params;
        this.returnType = returnType;
        this.access = access;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Map<String, Type> getParams() {
        return params;
    }

    public void setParams(Map<String, Type> params) {
        this.params = params;
    }

    public List<String> getAccess() {
        return access;
    }

    public void setAccess(List<String> access) {
        this.access = access;
    }

    public ToolDto toDto() {
        String methodName = method.getName();
        String returnTypeName = typeToString(returnType);

        Map<String, String> paramMap = new LinkedHashMap<>();
        for (Map.Entry<String, Type> entry : params.entrySet()) {
            paramMap.put(entry.getKey(), typeToString(entry.getValue()));
        }

        return new ToolDto(
                name,
                description,
                providerName,
                methodName,
                paramMap,
                returnTypeName);
    }

    private static String typeToString(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getName();
        }

        if (type instanceof ParameterizedType pType) {
            String raw = typeToString(pType.getRawType());
            String args = Arrays.stream(pType.getActualTypeArguments())
                    .map(Tool::typeToString)
                    .collect(Collectors.joining(", "));
            return raw + "<" + args + ">";
        }

        if (type instanceof GenericArrayType arrType) {
            return typeToString(arrType.getGenericComponentType()) + "[]";
        }

        if (type instanceof TypeVariable<?> typeVar) {
            return typeVar.getName();
        }

        if (type instanceof WildcardType wildcard) {
            StringBuilder sb = new StringBuilder("?");
            Type[] lower = wildcard.getLowerBounds();
            Type[] upper = wildcard.getUpperBounds();

            if (lower.length > 0) {
                sb.append(" super ").append(typeToString(lower[0]));
            } else if (upper.length > 0 && !upper[0].equals(Object.class)) {
                sb.append(" extends ").append(typeToString(upper[0]));
            }
            return sb.toString();
        }

        return type.getTypeName();
    }

}
