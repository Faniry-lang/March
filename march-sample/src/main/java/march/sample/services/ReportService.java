package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class ReportService {

    @LlmTool(description = "Generate a technical report of approximately the given word count on a topic")
    public String generateReport(String topic, int approxWords) {
        StringBuilder sb = new StringBuilder();
        sb.append("Report: ").append(topic).append("\n\n");
        sb.append("Introduction:\n");
        sb.append("This short report covers ").append(topic).append(". ");
        sb.append("[Content omitted for brevity in sample output].\n\n");
        sb.append("Conclusion:\n");
        sb.append("In summary, consider further benchmarking and testing.");
        return sb.toString();
    }
}
