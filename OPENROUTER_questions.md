# OpenRouter Integration Questions

Below are the focused questions I asked about OpenRouter so we can design the refactor to use OpenRouter's function-calling and messaging standards.

1. Function-calling API shape
   - Does OpenRouter support a `functions` top-level parameter like OpenAI (an array of function schemas) and return a function call in the response (e.g., `choices[0].message.function_call` with `name` + `arguments`)? If yes, show an example of the request body and a representative response JSON for a function call.

2. Messages vs single-prompt
   - Should we send a structured `messages` array (role/content objects) to OpenRouter, or does it prefer a single `prompt` string? Do you recommend using the structured messages array for function-calling and multi-step thinking?

3. Function schema expectations
   - What exact JSON Schema structure does OpenRouter expect for function parameters? (e.g., does it accept `parameters: {type: "object", properties: {...}, required: [...]}` exactly like OpenAI function-calling?) Any differences to note?

4. Streaming / chunked function_call content
   - When the model returns a function call, does OpenRouter provide the `arguments` as a single string or in chunked partial pieces during streaming? If streaming, what pattern should we follow to reconstruct `arguments` reliably?

5. Usage / token reporting
   - Does OpenRouter return usage/token counts (prompt_tokens, completion_tokens, total_tokens) in responses? If so, where in the response JSON, so we can decrement token budgets precisely?

6. Error & quota handling
   - What are the canonical HTTP error codes and JSON error shape (especially for quota: 402, rate-limit: 429, auth: 401/403)? Provide example payloads for 402 and 429 if available so we can parse recommended reductions (like "can only afford X tokens").

7. `max_tokens` parameter name and behavior
   - What is the exact key we should include to request a max output length? (`max_tokens`, `max_output_tokens`, something else?) Are there model-specific context limits we should be aware of (e.g., model has max context 8192 tokens)?

8. Function-calling invocation flow
   - For multi-step tool orchestration, do you recommend:
     A) letting the model produce function calls repeatedly (model → function → model) by re-inserting function output as assistant messages, or
     B) managing orchestration server-side and calling model only as needed?
     Which pattern aligns best with OpenRouter's design?

9. Function call return handling
   - When a function call is returned, does OpenRouter set a special `finish_reason` (e.g., `"function_call"`)? Is there any nuance in the response path we should handle (e.g., nested `choices` array, alternate indices)?

10. Input size and batching
    - For sending function schemas, is there a recommended size/length limit (practical guidance) for the `functions` array? Should we limit to top-N functions (we plan to do that), and do you have a recommended default N?

11. Authentication & headers
    - Any required headers besides `Authorization: Bearer <key>`? (You previously added `HTTP-Referer` and `X-Title` — are there other recommended headers or rate-limit headers to monitor?)

12. Guidance for retries/backoff
    - Any preferred backoff strategy or recommended `Retry-After` parsing for 429 or 503? Should we honor `Retry-After` headers? Any best practices from OpenRouter docs?

13. Model function argument types
    - Does OpenRouter support typed function parameters (e.g., distinguishing integer vs number vs string) and enforce them? Or does it accept flexible values and rely on the caller to coerce?

14. Preferred content path & response extraction
    - What's the best JSON path to extract model text for chat completions? (e.g., `choices[0].message.content` or `choices[0].message.function_call.arguments`). Confirm the exact node names and types.

15. Additional OpenRouter-specific features
    - Any additional OpenRouter-specific guidance or features we should use (e.g., `response_format`, `metadata`, `user` field, `message_id`, or streaming SSE endpoints)?

---

File created for review in the repository root: `OPENROUTER_questions.md`.
