package march.dev.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import march.dev.data.Tool;
import march.dev.data.ToolRegistry;
import march.dev.llm.LlmClient;
import march.dev.process.LlmResponse;
import march.dev.process.SystemResponse;
import march.dev.utils.JsonUtils;
import march.dev.utils.MethodRunner;

public abstract class Agent {

    protected LlmClient llmClient;
    protected ToolRegistry toolRegistry;
    protected MethodRunner methodRunner;
    protected ObjectMapper objectMapper;
    protected String history = "";
    protected String id;

    public Agent(LlmClient llmClient, ToolRegistry toolRegistry, MethodRunner methodRunner,
            ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.methodRunner = methodRunner;
        this.objectMapper = objectMapper;
    }

    public abstract String getSystemInstruction();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return "gemini-2.5-flash";
    }

    public String getLlmResponse(String prompt, String modelName) {
        return llmClient.generate(prompt, modelName);
    }

    public String chat(String userMessage) throws Exception {

        if (!history.contains("[SYSTEM_CONFIG]")) {
            history += this.getSystemPrompt();
        }
        history += "[USER_MESSAGE]\n" + userMessage;
        history += " [CHAIN_OF_THOUGHT]\n";
        history += " [START]... \n";

        int errorCount = 0;

        while (true) {
            LlmResponse response = null;
            try {
                String llmResponseString = getLlmResponse(history, getModel());
                String cleanedLlmResponseString = JsonUtils.cleanLlmResponse(llmResponseString);
                history += " [MODEL_RESPONSE]\n" + cleanedLlmResponseString + "\n";
                response = objectMapper.readValue(cleanedLlmResponseString, LlmResponse.class);

                if (response.isFunctionCall()) {
                    Tool tool = toolRegistry.get(response.getToolName());
                    Object[] args = response.getOrderedAndTypedArgs(toolRegistry, objectMapper);
                    Object result = methodRunner.execute(tool, args);
                    String resultJson = objectMapper.writeValueAsString(result);

                    SystemResponse systemResponse = new SystemResponse(response.getStep(), response.getToolName(),
                            resultJson, args);
                    String systemResponseJson = objectMapper.writeValueAsString(systemResponse);
                    history += " [BACKEND_RESPONSE]\n" + systemResponseJson + "\n";
                    continue;
                }

                history += " ...[END]\n";
                return response.getModelAnswer();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur: " + e.getMessage());
                response = new LlmResponse(-1, "Une erreur s'est produite " + e.getMessage(), "", false, "", null);
                history += " [BACKEND_RESPONSE]\n[ERROR]\n" + objectMapper.writeValueAsString(response) + "\n";
                errorCount++;

                if (errorCount > 3) {
                    return "Erreur lors de la génération de la réponse...";
                }

                continue;
            }
        }
    }

    public String getSystemPrompt() throws Exception {
        String toolJson = "";

        try {
            toolJson = objectMapper.writeValueAsString(toolRegistry.getToolsForAgent(this.id));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new Exception("Error during tool serialization: " + e.getMessage());
        }

        String systemPrompt = """
                [SYSTEM_CONFIG]
                You are March's default AI Agent. March is a micro framework for building
                AI Agents in Java, and its purpose is to give developers the possibility to
                use it in their own projects. Your goal is to answer any question from the user.\n

                [SYSTEM_TOOLS_LIST]
                """;

        if (this.getSystemInstruction() != null) {
            systemPrompt = this.getSystemInstruction() + "\n" + systemPrompt;
        }

        systemPrompt += toolJson + "\n";

        String responseFormatPrompt = """
                [SYSTEM_RESPONSE_FORMAT]
                All of your answers must follow this structure:
                {
                    "step": ,
                    "modelThought": ,
                    "modelAnswer": ,
                    "functionCall": ,
                    "toolName": ,
                    "arguments": {}
                }

                You must return nothing else except a JSON object with this
                structure.
                Do NOT return any Markdown code block delimiters, NO backticks (```), and NO comments.
                Your response must start directly with the opening brace '{' and end with the closing brace '}'.
                The "step" attribute is the step number of the request, from 1 to n.
                All comments you make must be in "modelThought";
                the answer for the user will be in "modelAnswer" (empty if it's not the final answer).
                The "functionCall" attribute is a boolean (either true or false, it CANNOT BE EMPTY);
                if the user's request does not require a function call, this will always be false.
                The "toolName" attribute is the name of the tool you have chosen to answer the user's request.
                IMPORTANT: If no tool can satisfy the request, do not try to create answers
                from scratch; just state that you do not have the necessary tools to execute the request.
                "toolName" MUST be a tool name from the list cited earlier (I mean the "name" attribute); you must NOT invent one.
                Finally, the "arguments" attribute is an array of arguments for the tool you have chosen. The arguments
                come from previous function calls you have made or directly from the user's message. They must be in the same
                order as the "params" attribute of the tool object.

                [SYSTEM_RESPONSE_EXAMPLE]
                {
                    "step": 2,
                    "modelThought": "The user wants the list of an employee's leave days. I will use the 'getCongeByEmployeId' tool with the argument they provided. If not, I will search the history to find the ID.",
                    "modelAnswer": "",
                    "functionCall": true,
                    "toolName": "getCongeByEmployeId",
                    "arguments": {
                        "employeId": 22
                    }
                }

                [SYSTEM_TOOL_ARGUMENTS_NAME]
                It is very IMPORTANT that the names of the arguments you provide in your response are the same
                as those listed in the "params" attribute of the Tool. If you are unsure about their names, you can
                call tools related to the Tool like "getToolParams" to be sure of their names.\n

                    """;

        systemPrompt += responseFormatPrompt;

        String stepProcessPrompt = """
                [SYSTEM_STEP_PROCESS]
                After reading the user's request, you will analyze the list of tools
                provided to you and establish a plan for the execution order of the tools to
                satisfy the user's request, as the request may not be immediately satisfied by a single tool but by a chain of tools.\n

                [SYSTEM_STEP_PROCESS_EXAMPLE]
                For example, the user asks for the list of employees who are on leave during this week.
                This request could be broken down into several steps depending on the tools you
                have available. If, after deep analysis, you conclude that it is impossible to
                satisfy the request, you must tell the user that you do not have enough
                tools to satisfy their request.

                However, if you have found a plan, you will specify in "modelThought" the description
                of the step as well as a guide for the next step to help you know what you will
                need to do next after finishing one step when you read this.\n
                """;

        systemPrompt += stepProcessPrompt;

        String finalAnswerPrompt = """
                [SYSTEM_FINAL_ANSWER]
                At each step, you will receive a response from the backend after tool execution. You will analyze each time
                whether the user's request is satisfied. If yes, then you put the final value of the answer in "modelAnswer"
                from the history and you will set "functionCall": false, and "toolName" empty.
                Your final answer must be composed from all other responses in markdown format.
                Tables are preferable to lists, if possible. (Tables displayable in markdown format)\n
                """;

        systemPrompt += finalAnswerPrompt;

        return systemPrompt;
    }
}
