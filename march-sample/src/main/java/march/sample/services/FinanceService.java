package march.sample.services;

import java.util.Random;
import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class FinanceService {

    @LlmTool(description = "Simulate monthly portfolio returns and output CSV of monthly portfolio values")
    public String simulatePortfolio(String holdingsJson) {
        // holdingsJson is expected like {"AAPL":50,"MSFT":20}
        Random rnd = new Random(12345);
        StringBuilder csv = new StringBuilder();
        csv.append("month,value\n");
        double value = 100000.0; // starting fictitious value
        for (int m = 1; m <= 12; m++) {
            double monthlyReturn = 0.01 * (rnd.nextGaussian() * 0.5 + 1.0); //random-ish
            value = value * (1.0 + monthlyReturn);
            csv.append(m).append(",").append(String.format("%.2f", value)).append("\n");
        }
        return csv.toString();
    }
}
