# AIService-main

Microservice for PeaceApp AI features.

The first phase runs in mock mode by default:

```bash
AI_MOCK_ENABLED=true
```

When `AI_MOCK_ENABLED=false`, the service checks that `OPENAI_API_KEY` exists, but real OpenAI calls are not implemented in this phase.
