package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@LlmContextProvider
public class SchedulerService {

    @LlmTool(description = "Create a friendly schedule summary given an appointment description and date-time")
    public String schedule(String who, String when) {
        LocalDateTime dt;
        try {
            dt = LocalDateTime.parse(when, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return "Invalid date format. Use ISO_DATE_TIME e.g. 2026-03-15T15:00:00";
        }
        return String.format("Scheduled %s on %s (local)", who, dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }
}
