package march.dev.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;

public class LlmResponse implements Response {
    int step;
    String modelThought;
    String modelAnswer;
    boolean functionCall;
    String toolName;
    Map<String, Object> arguments;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String toJson() throws Exception {
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("step", this.step);
        root.put("modelThought", this.modelThought);

        java.util.Map<String, Object> toolDetails = new java.util.LinkedHashMap<>();
        toolDetails.put("functionCall", this.functionCall);
        if (this.functionCall) {
            toolDetails.put("toolName", this.toolName);
            toolDetails.put("arguments", this.arguments != null ? this.arguments : new java.util.HashMap<>());
        }
        root.put("toolDetails", toolDetails);
        root.put("modelAnswer", this.modelAnswer);

        return MAPPER.writeValueAsString(root);
    }

    public LlmResponse() {
    }

    public LlmResponse(int step, String modelThought, String modelAnswer, boolean functionCall, String toolName,
            Map<String, Object> arguments) {
        this.step = step;
        this.modelThought = modelThought;
        this.modelAnswer = modelAnswer;
        this.functionCall = functionCall;
        this.toolName = toolName;
        this.arguments = arguments;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getModelThought() {
        return modelThought;
    }

    public void setModelThought(String modelThought) {
        this.modelThought = modelThought;
    }

    public String getModelAnswer() {
        return modelAnswer;
    }

    public void setModelAnswer(String modelAnswer) {
        this.modelAnswer = modelAnswer;
    }

    public boolean isFunctionCall() {
        return functionCall;
    }

    public void setFunctionCall(boolean functionCall) {
        this.functionCall = functionCall;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public Object[] getOrderedAndTypedArgs(ToolRegistry toolRegistry, ObjectMapper objectMapper) throws Exception {
        if (!functionCall || toolName == null || toolName.isEmpty()) {
            return new Object[0];
        }

        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        Map<String, java.lang.reflect.Type> paramTypes = tool.getParams();
        List<Object> orderedArgs = new ArrayList<>();

        for (Map.Entry<String, java.lang.reflect.Type> entry : paramTypes.entrySet()) {
            String paramName = entry.getKey();
            java.lang.reflect.Type paramType = entry.getValue();

            if (arguments != null && arguments.containsKey(paramName)) {
                Object argValue = arguments.get(paramName);
                com.fasterxml.jackson.databind.JavaType targetType = objectMapper.constructType(paramType);

                // Try direct conversion first
                try {
                    Object typedArg = objectMapper.convertValue(argValue, targetType);
                    orderedArgs.add(typedArg);
                    continue;
                } catch (Exception ex) {
                    // fallback to more permissive parsing below
                }

                // Handle stringified JSON arrays or comma-separated lists for array targets
                if (targetType.isArrayType()) {
                    Class<?> compRaw = targetType.getContentType().getRawClass();
                    Class<?> compBox = boxPrimitive(compRaw);

                    // If argValue is a String, try parsing JSON array or comma-separated values
                    if (argValue instanceof String) {
                        String s = ((String) argValue).trim();
                        // try JSON array first
                        try {
                            java.util.List<?> list = objectMapper.readValue(s,
                                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, compBox));
                            Object arr = toArrayFromList(list, compRaw);
                            orderedArgs.add(arr);
                            continue;
                        } catch (Exception ex) {
                            // not JSON array, try comma-separated
                            try {
                                String[] parts = s.split(",");
                                java.util.List<Object> vals = new java.util.ArrayList<>();
                                for (String p : parts) {
                                    String t = p.trim();
                                    Object v = parsePrimitiveOrString(t, compRaw);
                                    vals.add(v);
                                }
                                Object arr = toArrayFromList(vals, compRaw);
                                orderedArgs.add(arr);
                                continue;
                            } catch (Exception ex2) {
                                // fall through
                            }
                        }
                    }

                    // If argValue is a List, convert elements
                    if (argValue instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) argValue;
                        Object arr = toArrayFromList(list, compRaw);
                        orderedArgs.add(arr);
                        continue;
                    }

                    // As last resort, try to convert via ObjectMapper again but to a List then to array
                    try {
                        java.util.List<?> list = objectMapper.convertValue(argValue,
                                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, compBox));
                        Object arr = toArrayFromList(list, compRaw);
                        orderedArgs.add(arr);
                        continue;
                    } catch (Exception ex) {
                        // give up and add null
                        orderedArgs.add(null);
                        continue;
                    }
                }

                // Non-array fallback: try to coerce primitive in string form
                if (argValue instanceof String) {
                    String s = ((String) argValue).trim();
                    try {
                        Object coerced = convertStringToType(s, targetType.getRawClass());
                        orderedArgs.add(coerced);
                        continue;
                    } catch (Exception ex) {
                        // fall through
                    }
                }

                // Final fallback: attempt a direct conversion and if it fails add null
                try {
                    Object typedArg = objectMapper.convertValue(argValue, targetType);
                    orderedArgs.add(typedArg);
                } catch (Exception ex) {
                    orderedArgs.add(null);
                }

            } else {
                orderedArgs.add(null);
            }
        }

        return orderedArgs.toArray();
    }

    // Helpers for type coercion
    private static Class<?> boxPrimitive(Class<?> cls) {
        if (!cls.isPrimitive()) return cls;
        if (cls == int.class) return Integer.class;
        if (cls == long.class) return Long.class;
        if (cls == double.class) return Double.class;
        if (cls == float.class) return Float.class;
        if (cls == boolean.class) return Boolean.class;
        if (cls == byte.class) return Byte.class;
        if (cls == short.class) return Short.class;
        if (cls == char.class) return Character.class;
        return cls;
    }

    private static Object toArrayFromList(java.util.List<?> list, Class<?> compRaw) {
        int n = list.size();
        if (compRaw == int.class) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = ((Number) list.get(i)).intValue();
            return a;
        }
        if (compRaw == long.class) {
            long[] a = new long[n];
            for (int i = 0; i < n; i++) a[i] = ((Number) list.get(i)).longValue();
            return a;
        }
        if (compRaw == double.class) {
            double[] a = new double[n];
            for (int i = 0; i < n; i++) a[i] = ((Number) list.get(i)).doubleValue();
            return a;
        }
        if (compRaw == float.class) {
            float[] a = new float[n];
            for (int i = 0; i < n; i++) a[i] = ((Number) list.get(i)).floatValue();
            return a;
        }
        if (compRaw == boolean.class) {
            boolean[] a = new boolean[n];
            for (int i = 0; i < n; i++) a[i] = Boolean.parseBoolean(list.get(i).toString());
            return a;
        }
        if (compRaw == String.class) {
            String[] a = new String[n];
            for (int i = 0; i < n; i++) a[i] = list.get(i) == null ? null : list.get(i).toString();
            return a;
        }
        // boxed types
        Object arr = java.lang.reflect.Array.newInstance(compRaw, n);
        for (int i = 0; i < n; i++) {
            java.lang.reflect.Array.set(arr, i, list.get(i));
        }
        return arr;
    }

    private static Object parsePrimitiveOrString(String token, Class<?> compRaw) {
        if (compRaw == int.class || compRaw == Integer.class) return Integer.parseInt(token);
        if (compRaw == long.class || compRaw == Long.class) return Long.parseLong(token);
        if (compRaw == double.class || compRaw == Double.class) return Double.parseDouble(token);
        if (compRaw == float.class || compRaw == Float.class) return Float.parseFloat(token);
        if (compRaw == boolean.class || compRaw == Boolean.class) return Boolean.parseBoolean(token);
        if (compRaw == String.class) return token;
        return token;
    }

    private static Object convertStringToType(String s, Class<?> targetClass) {
        if (targetClass == String.class) return s;
        if (targetClass == Integer.class || targetClass == int.class) return Integer.parseInt(s);
        if (targetClass == Long.class || targetClass == long.class) return Long.parseLong(s);
        if (targetClass == Double.class || targetClass == double.class) return Double.parseDouble(s);
        if (targetClass == Float.class || targetClass == float.class) return Float.parseFloat(s);
        if (targetClass == Boolean.class || targetClass == boolean.class) return Boolean.parseBoolean(s);
        // fallback: return the raw string
        return s;
    }

}
