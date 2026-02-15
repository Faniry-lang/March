package march.utils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FormatUtils {
    public static String typeToString(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getName();
        }

        if (type instanceof ParameterizedType pType) {
            String raw = typeToString(pType.getRawType());
                String args = Arrays.stream(pType.getActualTypeArguments())
                    .map(FormatUtils::typeToString)
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
