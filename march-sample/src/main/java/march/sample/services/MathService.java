package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class MathService {

    @LlmTool(description = "Compute factorial of n (supports moderately large n up to 20)")
    public String factorial(int n) {
        if (n < 0) return "n must be non-negative";
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return String.format("%d! = %d", n, result);
    }

    @LlmTool(description = "Return the nth Fibonacci number and an explanation of the steps")
    public String fibonacciSteps(int n) {
        if (n < 0) return "n must be non-negative";
        if (n == 0) return "Fib(0)=0";
        if (n == 1) return "Fib(1)=1";
        long a = 0, b = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("Fib series: 0, 1");
        for (int i = 2; i <= n; i++) {
            long c = a + b;
            sb.append(", ").append(c);
            a = b;
            b = c;
        }
        sb.append(String.format("\nResult: Fib(%d) = %d", n, b));
        return sb.toString();
    }
}
