# Smart Email Assistant

An AI-powered email assistant built with **Spring Boot 3.4** and **Spring AI 1.0**. Paste any email and it will:

- **Draft a reply** in the tone you choose — professional, friendly, concise or formal — without inventing names, dates or commitments that aren't in the original email.
- **Summarize the email** in 2–3 lines and **extract its action items** (tasks, requests, deadlines) as a list.

It talks to any **OpenAI-compatible** chat endpoint, so it runs on OpenAI out of the box — or on **Groq's free tier** just by changing environment variables (see [Run it free with Groq](#run-it-free-with-groq)).

A small demo web UI is served by the app itself at `/` — no separate frontend build.

---

## Features

| Endpoint | Description |
|---|---|
| `POST /api/email/generate-reply` | Draft a tone-aware reply to an email |
| `POST /api/email/summarize` | 2–3 line summary + extracted action items |
| `GET /api/health` | Liveness check — works with **no API key configured** |
| `GET /` | Demo UI (paste email → generate reply / summarize) |

- Input validation: an empty email returns **400** with a clean JSON error.
- If `OPENAI_API_KEY` is missing or the provider rejects a call, the AI endpoints return **502** with a readable JSON message — never a stack trace.
- All configuration comes from environment variables. No API key exists anywhere in this repository.

## Tech stack

- Java 17
- Spring Boot 3.4.5 (Web, Validation)
- Spring AI 1.0.0 (`spring-ai-starter-model-openai`)
- Maven (wrapper included)
- Docker (multi-stage build) for deployment

## Project structure

```
src/main/java/com/ayush/emailassistant/
├── SmartEmailAssistantApplication.java
├── controller/
│   ├── EmailController.java      # /api/email endpoints
│   └── HealthController.java     # /api/health
├── service/
│   └── EmailAssistantService.java  # prompts, tone handling, provider error translation
├── dto/                          # request/response records
└── exception/                    # BadRequest (400), AiService (502), global JSON error handler
src/main/resources/
├── application.properties        # env-driven configuration
└── static/index.html             # demo UI
```

---

## Run locally

### Prerequisites

- JDK 17+
- An API key from OpenAI **or** Groq (free) — the app starts without one, but the AI endpoints will return 502 until a key is set.

### 1. Set the environment variables

**Option A — OpenAI**

```bash
export OPENAI_API_KEY="sk-..."          # your OpenAI key
# optional: export AI_MODEL="gpt-4o-mini"   (this is the default)
```

**Option B — Groq (free tier)** — see [below](#run-it-free-with-groq) for how to get a free key.

```bash
export OPENAI_API_KEY="gsk_..."                          # your Groq key
export OPENAI_BASE_URL="https://api.groq.com/openai/v1"
export AI_MODEL="llama-3.3-70b-versatile"
```

On Windows (PowerShell) use `$env:OPENAI_API_KEY="..."` instead of `export`.

### 2. Build and run

```bash
./mvnw clean package -DskipTests     # or: mvn clean package -DskipTests
java -jar target/smart-email-assistant-1.0.0.jar
```

The app starts on **http://localhost:8080** (or on `$PORT` when set). Open that URL in a browser for the demo UI.

### Configuration reference

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `OPENAI_API_KEY` | For AI endpoints | — | API key for the chosen provider (OpenAI or Groq) |
| `OPENAI_BASE_URL` | No | `https://api.openai.com/v1` | Base URL of an OpenAI-compatible endpoint, ending at the version segment (`…/v1`) |
| `AI_MODEL` | No | `gpt-4o-mini` | Chat model name (e.g. `llama-3.3-70b-versatile` on Groq) |
| `PORT` | No | `8080` | HTTP port (set automatically by Render and most PaaS hosts) |

---

## Run it free with Groq

Groq offers a generous free tier with very fast inference, and its API is OpenAI-compatible — this app works with it with **zero code changes**:

1. Go to [console.groq.com](https://console.groq.com), sign up (free), and create an API key under **API Keys**.
2. Run the app with:

   ```bash
   export OPENAI_API_KEY="gsk_your_groq_key"
   export OPENAI_BASE_URL="https://api.groq.com/openai/v1"
   export AI_MODEL="llama-3.3-70b-versatile"
   java -jar target/smart-email-assistant-1.0.0.jar
   ```

That's it — the same endpoints and UI now run on `llama-3.3-70b-versatile` hosted by Groq.

---

## API examples

### Health

```bash
curl http://localhost:8080/api/health
```

```json
{ "status": "ok" }
```

### Generate a reply

```bash
curl -X POST http://localhost:8080/api/email/generate-reply \
  -H "Content-Type: application/json" \
  -d '{
        "emailContent": "Hi, this is Priya from HR. Please submit your onboarding documents by Friday, 21 June. Let me know if you face any issue. Thanks!",
        "tone": "professional"
      }'
```

```json
{
  "reply": "Hi Priya,\n\nThank you for letting me know. I will submit my onboarding documents by Friday, 21 June. I will reach out if I face any issue.\n\nBest regards"
}
```

`tone` is optional (default `professional`) and accepts `professional`, `friendly`, `concise`, `formal`.

### Summarize

```bash
curl -X POST http://localhost:8080/api/email/summarize \
  -H "Content-Type: application/json" \
  -d '{ "emailContent": "Hi, this is Priya from HR. Please submit your onboarding documents by Friday, 21 June. Let me know if you face any issue. Thanks!" }'
```

```json
{
  "summary": "Priya from HR asks for onboarding documents to be submitted by Friday, 21 June, and offers help if there are any issues.",
  "actionItems": [
    "Submit onboarding documents by Friday, 21 June",
    "Inform Priya if you face any issue"
  ]
}
```

### Error responses

Empty email → **400**

```json
{ "error": "emailContent: emailContent must not be empty" }
```

Unsupported tone → **400**

```json
{ "error": "Unsupported tone 'angry'. Supported tones: professional, friendly, concise, formal." }
```

No API key configured → **502**

```json
{ "error": "AI service is not configured: the OPENAI_API_KEY environment variable is missing. Set OPENAI_API_KEY (and optionally OPENAI_BASE_URL / AI_MODEL) and restart the application." }
```

Provider rejected the request (e.g. invalid key) → **502** with the provider's message, e.g.

```json
{ "error": "AI provider request failed: Invalid API Key" }
```

---

## Docker

```bash
docker build -t smart-email-assistant .
docker run -p 8080:8080 \
  -e OPENAI_API_KEY="gsk_..." \
  -e OPENAI_BASE_URL="https://api.groq.com/openai/v1" \
  -e AI_MODEL="llama-3.3-70b-versatile" \
  smart-email-assistant
```

## Deploy on Render

1. Push this repository to GitHub.
2. In Render: **New → Web Service**, connect the repository.
3. Choose the **Docker** environment (Render will use the included `Dockerfile`) — or the **Java** environment with build command `./mvnw package -DskipTests` and start command `java -jar target/smart-email-assistant-1.0.0.jar`.
4. Add the environment variables in the Render dashboard:
   - `OPENAI_API_KEY` = your Groq (or OpenAI) key
   - For Groq: `OPENAI_BASE_URL=https://api.groq.com/openai/v1` and `AI_MODEL=llama-3.3-70b-versatile`
   - `PORT` is injected by Render automatically; the app already reads it.
5. Deploy, then verify: `https://<your-service>.onrender.com/api/health` should return `{"status":"ok"}`, and the demo UI loads at the root URL.

> Note: Render's free web services spin down when idle, so the first request after a pause can take ~30–60 seconds.

## Tests

```bash
./mvnw test
```

The included tests cover the health endpoint and input validation, and run **without** any live API key.

---

Built by **Ayush Garg** — Java · Spring Boot · Spring AI
