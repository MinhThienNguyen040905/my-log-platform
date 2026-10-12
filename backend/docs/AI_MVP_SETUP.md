# Groq AI MVP setup

The operational product is the Spring backend and Next.js web app. Both journal analysis
after save and on-demand writing suggestions use Groq through the backend. The Expo mobile
directory is still a starter app with no journal or authentication flow. The Python
`recommend-model` and `safety-model` directories are separate preparation/training tools;
neither serves requests in this MVP.

## Backend configuration

Set these in the backend environment for each API and worker deployment. Keep the key on the
server; never add it to web or mobile configuration.

```properties
MYLOG_AI_PROVIDER=groq
MYLOG_AI_MODEL=openai/gpt-oss-20b
MYLOG_AI_API_KEY=<Groq key>
MYLOG_AI_MVP_ENABLED=true
MYLOG_JOBS_ENABLED=true
MYLOG_WRITING_SUGGESTIONS_ENABLED=true
MYLOG_WRITING_SUGGESTIONS_AI_ENABLED=true
MYLOG_AI_INPUT_USD_PER_MILLION=0.075
MYLOG_AI_OUTPUT_USD_PER_MILLION=0.30
```

The API needs the key for writing suggestions; the worker needs it for saved-entry analysis.
For a split deployment, configure `MYLOG_APP_PROFILE=api` and `worker` accordingly. The
prices above are estimates for the named model; check [Groq's model page](https://console.groq.com/docs/model/openai/gpt-oss-20b)
when using cost reports. Restart API and worker after configuration changes.

AI controls are visible in the web without a public environment flag. Each user explicitly
grants version `groq-v1` of `AI_PROCESSING` during onboarding or in Settings. The UI explains
that content is sent to Groq and links to Groq's data policy. Users can withdraw the grant.
`MODEL_TRAINING` remains a separate choice and is not granted by AI processing consent.
Drafts are sent only when the user requests a suggestion. Saved entries are analyzed by the
worker after screening and consent checks.

## MVP safety scope

`MYLOG_AI_MVP_ENABLED=true` selects a rule-based safety baseline and permits Groq calls
without marking the provider/output review flags as approved. It works in all Spring profiles.
The baseline blocks explicit risk phrases and several ambiguous phrases, but it is neither a
trained nor an evaluated classifier and may miss risky content. It records policy version
`mvp-rules-v1`. Structured output validation remains in place. Enabling the MVP does not
rescreen older blocked entries in a shared database.

The remaining review and end-to-end checks are tracked in
[AI_MVP_REAL_USER_READINESS.md](AI_MVP_REAL_USER_READINESS.md). Set
`MYLOG_JOBS_ENABLED=false` and `MYLOG_WRITING_SUGGESTIONS_ENABLED=false` to stop AI requests;
restart API and worker after changing them.
