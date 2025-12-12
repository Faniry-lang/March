package march.dev.process;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;

public class TestLlmResponseCoercion {

    public static class DummyService {
        public double[] sum(double[] values) {
            double s = 0;
            for (double d : values) s += d;
            return values;
        }
    }

    @Test
    public void testStringifiedArrayToDoubleArray() throws Exception {
        Method m = DummyService.class.getMethod("sum", double[].class);
        Map<String, Type> params = new LinkedHashMap<>();
        params.put("values", double[].class);

        Tool tool = new Tool("sum", "sum numbers", "dummy", m, params, double[].class, java.util.List.of());
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool);

        ObjectMapper mapper = new ObjectMapper();

        LlmResponse resp = new LlmResponse(1, "thought", "", true, "sum", Map.of("values", "[1, 2, 3.5]"));

        Object[] args = resp.getOrderedAndTypedArgs(registry, mapper);
        assertNotNull(args);
        assertEquals(1, args.length);
        assertTrue(args[0] instanceof double[]);
        double[] arr = (double[]) args[0];
        assertEquals(3, arr.length);
        assertEquals(1.0, arr[0]);
        assertEquals(2.0, arr[1]);
        assertEquals(3.5, arr[2]);
    }
}
