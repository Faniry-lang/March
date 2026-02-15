package march.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;

public class Function extends Tool {
    Object providerInstance;
    Method methodInstance;
    LinkedHashMap<String, Type> parameters;
    Type returnType;
    public Function(String name, String description, Object providerInstance, Method methodInstance, LinkedHashMap<String, Type> parameters, Type returnType) {
        super(name, description);
        this.providerInstance = providerInstance;
        this.methodInstance = methodInstance;
        this.parameters = parameters;
        this.returnType = returnType;
    }
    public Function() {
    }
    public Object getProviderInstance() {
        return providerInstance;
    }
    public void setProviderInstance(Object providerInstance) {
        this.providerInstance = providerInstance;
    }
    public Method getMethodInstance() {
        return methodInstance;
    }
    public void setMethodInstance(Method methodInstance) {
        this.methodInstance = methodInstance;
    }
    public LinkedHashMap<String, Type> getParameters() {
        return parameters;
    }
    public void setParameters(LinkedHashMap<String, Type> parameters) {
        this.parameters = parameters;
    }
    public Type getReturnType() {
        return returnType;
    }
    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public Object invoke(Object... args) throws Exception {
        Object[] convertedArgs = new Object[args.length];
        
        if (args.length != parameters.size()) {
             throw new Exception("Invalid number of arguments. Expected: " + parameters.size() + ", Received: " + args.length);
        }

        int i = 0;
        for (java.util.Map.Entry<String, Type> entry : parameters.entrySet()) {
            Type expectedType = entry.getValue();
            Object arg = args[i];
            
            if (arg == null) {
                convertedArgs[i] = null; 
            } else {
                Class<?> expectedClass = (expectedType instanceof Class) ? (Class<?>) expectedType : null;
                
                if (expectedClass != null && expectedClass.isAssignableFrom(arg.getClass())) {
                    convertedArgs[i] = arg;
                } else if (arg instanceof Number) {
                    Number num = (Number) arg;
                    if (expectedType == Double.class || expectedType == double.class) {
                        convertedArgs[i] = num.doubleValue();
                    } else if (expectedType == Integer.class || expectedType == int.class) {
                        convertedArgs[i] = num.intValue();
                    } else if (expectedType == Long.class || expectedType == long.class) {
                        convertedArgs[i] = num.longValue();
                    } else if (expectedType == Float.class || expectedType == float.class) {
                        convertedArgs[i] = num.floatValue();
                    } else {
                        convertedArgs[i] = arg; 
                    }
                } else {
                    convertedArgs[i] = arg; 
                }
            }
            i++;
        }
        
        return methodInstance.invoke(providerInstance, convertedArgs);
    }
}