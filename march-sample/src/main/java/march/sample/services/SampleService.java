package march.sample.services;

import march.dev.annotations.LlmContextProvider;
import march.dev.annotations.LlmTool;

@LlmContextProvider
public class SampleService {

    @LlmTool(name = "Greeting function", description = "Use this function to answer user\'s greetings")
    public String greetings(String name) {
        return "Hello "+name+", nice to meet you from March team!";
    }

    @LlmTool(name = "Farwell function", description = "Use this function to say farwell to user")
    public String farwell(String name) {
        return "Farwell "+name+" my friend, we'll meet again";
    }

}