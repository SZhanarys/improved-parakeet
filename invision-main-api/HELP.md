# Main API

## Hackathon startup

### One-time setup

1. Copy `.env.local.example` to `.env.local`
2. Put the real API key into `AI_API_KEY`

Example:

```env
AI_API_KEY=your_real_api_key
AI_BASE_URL=https://api.openai.com/v1
AI_MODEL=gpt-4.1-mini
```

### Backend only

Run:

```bat
start-main-api.bat
```

This script reads `.env.local`, exports the variables for that process, and starts Spring Boot.

### Full demo

From the repository root run:

```bat
start-hackathon.bat
```

This starts:
- `invision-main-api`
- Flutter web in Edge

## AI configuration

External AI is configured in `src/main/resources/application.yml`:

```yaml
ai:
  enabled: true
  base-url: ${AI_BASE_URL:https://api.openai.com/v1}
  model: ${AI_MODEL:gpt-4.1-mini}
  api-key: ${AI_API_KEY:}
```

## Notes

- If `ai.enabled=true` and `AI_API_KEY` is present, the backend will call the external AI service.
- If the key is missing or the remote call fails, the backend falls back to local scoring and local text normalization.
- Candidate list loading was kept fast by using already-saved normalized skills instead of calling AI for every candidate on list open.
