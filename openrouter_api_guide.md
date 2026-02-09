### 1. Function Call API Structure

#### Top-level Parameter (`functions` vs. `tools`)

OpenRouter uses the top-level parameter **`tools`** in the request body, which is an array of function schemas, rather than a separate `functions` parameter.

Function specification is generally compatible with OpenAI's function call parameter. Each item in the `tools` array is of type `"function"` and contains a `function` object defining the name, description, and parameters (JSON schema). The `tools` parameter must be included in every request (both initial request and when sending tool results).

#### Response Structure for Function Calls

OpenRouter does **not** directly return a `choices.message.function_call` object. Instead, it uses the modern tool-based format: the model's response contains a **`tool_calls`** array in the `message` object if the model decides to use a function.

The `tool_calls` object contains an ID (`id`), type (`"function"`), and a `function` object that includes the `name` and `arguments` (provided as a JSON string).

Additionally, when a function is requested, the LLM's `finish_reason` is typically `tool_calls`.

---

### Example Request and Representative Response

#### Example Request Body (Step 1: Initial request with tools)

To indicate available functions, include the top-level **`tools`** parameter containing an array of function definitions:

```json
{
 "model": "google/gemini-2.0-flash-001",
 "messages": [
  {
   "role": "user",
   "content": "What are the titles of some James Joyce books?"
  }
 ],
 "tools": [
  {
   "type": "function",
   "function": {
    "name": "search_gutenberg_books",
    "description": "Search for books in the Project Gutenberg library",
    "parameters": {
     "type": "object",
     "properties": {
      "search_terms": {
       "type": "array",
       "items": {"type": "string"},
       "description": "List of search terms to find books"
      }
     },
     "required": ["search_terms"]
    }
   }
  }
 ]
}
```

#### Example Representative Response (Model response with a function call)

If the model decides to call the tool, it responds with a message where `content` is `null` and the **`tool_calls`** array is present, containing the function name and required arguments:

```json
{
 "choices": [
   {
    "message": {
     "role": "assistant",
     "content": null,
     "tool_calls": [
      {
       "id": "call_abc123",
       "type": "function",
       "function": {
        "name": "search_gutenberg_books",
        "arguments": "{\"search_terms\": [\"James\", \"Joyce\"]}"
       }
      }
     ]
    }
   }
 ]
}
```

After receiving this response, the client is responsible for executing the function locally and returning the result to the LLM in a subsequent request using the `"tool"` role.

---

### 2. Messages vs. Single-Prompt

OpenRouter uses the structured `messages` array (objects with `role`/`content`) for model interaction, especially for advanced features. Examples in documentation for inference requests (Steps 1 and 3) use this structured format.

**For function calling and multi-step reasoning, it is strongly recommended to use the structured `messages` array.**

Reasons to use `messages`:

1. **Function Calling:** Sequential use of `messages` preserves full conversation context.
   - Initial inference (Step 1) is sent with `user` role.
   - Model responses with `tool_calls` are added to `messages`.
   - Tool execution results are returned to the model in `messages` with `tool` role and `tool_call_id`.
   - This ensures full context is maintained for final response generation.

2. **Interleaved Thinking:** Allows reasoning between tool calls.
   - Lets the model reason on a tool's output before deciding the next step.
   - Best practices emphasize **context preservation** across multiple tool interactions.
   - Using structured `messages` enables complex multi-tool workflows.

In short, for any tool interaction or conversation history, the **structured `messages` array** is the recommended input method.

---

### 3. Function Schema Expectations

OpenRouter standardizes tool call interfaces across supported models.

The expected JSON Schema structure for function parameters is **compatible with OpenAI function call format**.

Expected JSON Schema under `"parameters"`:

1. **Main Object (`parameters`):**
   - Must have `"type": "object"`
2. **Properties (`properties`):**
   - Object defining the function arguments. Each property specifies its `"type"`, optional `"description"`, `"enum"`, or `"default"`.
3. **Required Fields (`required`):**
   - Array of property names required for function execution.

Example parameter structure:

```json
"parameters": {
  "type": "object",
  "properties": {
    "search_terms": {
      "type": "array",
      "items": {"type": "string"},
      "description": "List of search terms to find books"
    }
  },
  "required": ["search_terms"]
}
```

---

### 4. Streaming / Chunked Function Call Content

OpenRouter supports streaming tool call responses.

When using streaming (`stream: true`), function arguments are delivered in **partial chunks** within the `delta` object.

To reconstruct arguments reliably:

1. **Accumulate deltas:** Track `data.choices.delta.tool_calls`.
2. **Buffer:** Add each chunk to a structure (e.g., `toolCalls` array).
3. **Process on completion:** When `finish_reason` is `'tool_calls'`, parse and execute the full arguments.

Example with optional fields and enums:

```json
"parameters": {
  "type": "object",
  "properties": {
    "location": {
      "type": "string",
      "description": "City name, zip code, or coordinates (lat,lng). Examples: 'New York', '10001', '40.7128,-74.0060'"
    },
    "units": {
      "type": "string",
      "enum": ["celsius", "fahrenheit"],
      "description": "Temperature unit preference",
      "default": "celsius"
    }
  },
  "required": ["location"]
}
```

`tools` must be included in every request for schema validation.

---

### 5. Usage / Token Reporting

Using advanced features like Interleaved Thinking **increases token usage** due to extra reasoning steps. Sources do not provide final API response structures with token counts (`prompt_tokens`, `completion_tokens`, `total_tokens`). Refer to full OpenRouter API docs for exact token accounting.

---

### 6. Error & Quota Handling

Sources focus on tool calling, streaming, and agentic loops.

No canonical HTTP error codes (401, 402, 403, 429) or quota payload examples are provided. Consult full API documentation for error and quota handling.

---

### 7. `max_tokens` Parameter Name and Behavior

Sources do not specify the exact key (`max_tokens`, `max_output_tokens`, etc.) for limiting output length.

Requests include parameters like:
- `model`
- `messages`
- `tools`
- `stream`

But no explicit token limit key is shown.

---

### 8. Function-Calling Invocation Flow

Recommended approach aligns with OpenRouter design and simple agentic loops: **let the model generate function calls repeatedly**.

Flow:
1. Client sends initial request with available tools.
2. Model returns one or more tool calls.
3. Client executes tools locally.
4. Client updates `messages` with assistant message (`tool_calls`) and tool results (`tool`).
5. Client resends updated `messages`.
6. Repeat if more tool calls are needed.

Supports **Interleaved Thinking**.

Code snippet (Pattern A):

```javascript
async function callLLM(messages) {
  const result = await openRouter.chat.send({
    model: '{{MODEL}}',
    tools, // Include in every request
    messages,
    stream: false,
  });
  messages.push(result.choices.message);
  return result;
}

while (iterationCount < maxIterations) {
  iterationCount++;
  const response = await callLLM(messages);

  if (response.choices.message.toolCalls) {
    messages.push(await getToolResponse(response));
  } else {
    break;
  }
}
```

---

### 9. Function Call Return Handling

`finish_reason` is `tool_calls` when a function is returned.

Access via:

```javascript
const message = result.choices.message;
if (message.tool_calls) {
  const argumentsJsonString = message.tool_calls.function.arguments;
  const parsedArgs = JSON.parse(argumentsJsonString);
} else {
  const modelText = message.content;
}
```

---

### 10. Input Size and Batching

No practical limit for `tools` array is mentioned. Clear descriptions and structured parameter schemas are recommended.

---

### 11. Authentication & Headers

Required headers:
```
Content-Type: application/json
```
Authorization header implied. No explicit `HTTP-Referer` or rate limit headers mentioned.

---

### 12. Guidance for Retries/Backoff

No guidance on backoff strategies, `Retry-After`, or HTTP 429/503 handling in the sources. Consult full API docs.

---

### 13. Model Function Argument Types

OpenRouter supports typed function parameters via **JSON Schema**:
- `string`, `array` (with `items`), `object`, `integer`, etc.
- Model generates arguments as JSON string, client parses for typed usage.

Example:
```json
{"search_terms": ["James", "Joyce"]}
```

---

### 14. Preferred Content Path & Response Extraction

Extract final text from non-tool calls:
```
result.choices.message.content
```

Tool calls: `content` is null, data in `message.tool_calls`.

---

### 15. Additional OpenRouter-specific Features

| Feature / Parameter | Usage | Context |
| :--- | :--- | :--- |
| **Tool Choice (`tool_choice`)** | Control tool usage. `"auto"` (default), `"none"`, or specific tool. | `tool_choice: "auto"` or specific function object. |
| **Parallel Tool Calls** | Allow multiple tools simultaneously. Default `true`. | Set `parallel_tool_calls: false` for sequential execution. |
| **Interleaved Thinking** | Insert reasoning steps between tool calls. | Increases token usage and latency. |
| **Structured Outputs** | Output structuring. | Parameter name or JSON example not provided. |
| **App Attribution** | Attribute application requests. | No header or field instructions provided. |

Consult the full OpenRouter API docs for exact parameters and message formats.

