package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider()
public class WeatherService {

    public WeatherService() {
    }

    @LlmTool(description = "Get current weather for a city", access = {"jarvis"})
    public String currentWeather(String city) {
        // For testing, return a canned response using the city parameter
        if (city == null || city.isEmpty()) {
            return "No city provided.";
        }
        return String.format("Current weather in %s: Sunny, 24°C (sample)", city);
    }
}
