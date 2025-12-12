package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class SearchService {

    @LlmTool(description = "Simulate a web search for a query and return a long formatted result list")
    public String webSearch(String query, int results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= results; i++) {
            sb.append(String.format("%d. Result for '%s' - summary line %d.\n", i, query, i));
            sb.append("Details: This is a simulated long description that explains why this result is relevant.\n\n");
        }
        return sb.toString();
    }
}
