package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class NLPService {

    @LlmTool(description = "Return sentiment: positive/neutral/negative and a short rationale")
    public String sentiment(String text) {
        if (text == null || text.isEmpty()) return "neutral";
        String t = text.toLowerCase();
        int score = 0;
        if (t.contains("good") || t.contains("great") || t.contains("excellent")) score++;
        if (t.contains("bad") || t.contains("terrible") || t.contains("poor")) score--;
        if (score > 0) return "positive: contains positive words";
        if (score < 0) return "negative: contains negative words";
        return "neutral: no strong sentiment clues";
    }
}
