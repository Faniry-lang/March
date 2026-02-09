- review copilot code
- debug llmClient is null

- Add MockLlmClient + CI offline profile (lets you iterate without API keys).
- Improve OpenRouter client timeouts/retries + clearer auth messages.
- Add unit tests for JsonUtils and LlmResponse coercion using the mock client.
- Add function-schema generation for tools and strengthen system prompt.
- Implement token-budgeting summarizer + retention policy.
- Add structured logging/metrics and security hardening.
- Optional: embeddings-based tool ranking.