package march.sample.services;

import java.util.Arrays;
import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class StatsService {

    @LlmTool(description = "Compute mean, median, std dev, and 95% CI for a small list of numbers")
    public String analyze(double[] values) {
        if (values == null || values.length == 0) return "[]";
        Arrays.sort(values);
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / values.length;
        double median = values[values.length/2];
        double variance = 0;
        for (double v : values) variance += Math.pow(v - mean, 2);
        variance /= values.length;
        double std = Math.sqrt(variance);
        double se = std / Math.sqrt(values.length);
        double ci95 = 1.96 * se;
        return String.format("{\"mean\":%.3f,\"median\":%.3f,\"std\":%.3f,\"ci95\":%.3f}", mean, median, std, ci95);
    }
}
