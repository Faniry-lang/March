# March Framework

> **Author**: Ranaivoson Mazà Faniriniaina (@Faniry-lang)

---

### ⚠️ Disclaimer
This is a **student project** created for fun and learning. It is primarily intended for personal use in my own projects. While not officially maintained for the public, you are more than welcome to clone it, explore the code, and try it out for yourself!

### 🚀 Roadmap / To-Dos
- [ ] Implement advanced **Summarized Context** (currently simple truncation).
- [ ] Implement **Smarter Tool Selection** using Embeddings (Vector search for relevant tools).
- [ ] Add support for more LLM providers (native OpenAI/Claude).

---

## 🌟 Overview

March is a minimal yet powerful multi-agent AI framework for Java developers. It allows you to build autonomous agents that can use tools, track their own costs, and be managed through a sleek command-line interface.

### Key components:
- **`march-core`**: The heart of the framework. Contains the base `Agent` logic, CLI commands, and metrics system.
- **`march-spring-boot`**: Integration layer for Spring Boot, providing dynamic agent scanning and auto-configuration.
- **`march-micronaut`**: Experimental Micronaut support.

---

## ✨ Features

- **Dynamic Agent Discovery**: Simply annotate your class with `@MarchAgent` and the framework will find it.
- **Tool-Calling Engine**: Register methods as tools using `@MarchTool` and agents will execute them autonomously.
- **Real-Time Cost Tracking**: Automatically logs API usage (via character count) and compute costs (processing time) into JSON metrics.
- **Interactive CLI**: A colorful terminal shell to list agents, view calculated usage costs (Day/Week/Month), and chat directly with agents.
- **Persistent Chat History**: Previous conversations are saved locally in `${user.home}/.march/chats/`.

---

## 🛠️ Getting Started

### 1. Prerequisites
- Java 17 or higher.
- Maven.
- An **OpenRouter API Key** (for LLM access).

### 2. Environment Setup
Set your API key as an environment variable:
```bash
# Windows
setx OPENROUTER_API_KEY "your_key_here"

# Linux/Mac
export OPENROUTER_API_KEY="your_key_here"
```

### 3. Installation
Clone the repository and install the framework modules to your local Maven repository:
```bash
cd march
mvn clean install -DskipTests
```

### 4. Running the Example Project
Navigate to the example Spring Boot project and start the shell:
```bash
cd ../spring-boot-project
mvn spring-boot:run
```

---

## 💻 Usage

### Defining an Agent
```java
@MarchAgent(name = "FinanceAdvisor")
public class FinanceAgent extends Agent {
    public FinanceAgent(String apiKey, ObjectMapper objectMapper, ToolRegistry toolRegistry, LlmClient llmClient) {
        super(16000, apiKey, objectMapper, toolRegistry, llmClient);
        this.setSystemPrompt("You are a professional financial advisor...");
    }

    @Override
    public CostPerPrompt onAgentUsage(DetailedPromptInfo info) {
        // Calculate your own apiCost and computeCost here
        return new CostPerPrompt();
    }
}
```

### CLI Commands
When the application starts, you'll see the **March CLI** banner.
- `agent list`: Shows all discovered agents and their actual calculated costs.
- `agent chat <AgentName>`: Start a conversation. Use `-m <model_name>` to override the model.
- `exit`: Close the shell.

---

## 📂 Project Structure
- **/march/core**: Pure Java implementation, framework-agnostic.
- **/march/spring-boot**: Spring Boot starter and AutoConfigs.
- **/spring-boot-project**: A demonstration project showcasing many agents and tools.

---
*Happy Coding!* 🚀
