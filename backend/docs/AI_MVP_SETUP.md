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
MYLOG_SAFETY_CLASSIFIER_PROVIDER=groq
MYLOG_JOBS_ENABLED=true
MYLOG_WRITING_SUGGESTIONS_ENABLED=true
MYLOG_WRITING_SUGGESTIONS_AI_ENABLED=true
MYLOG_AI_INPUT_USD_PER_MILLION=0.075
MYLOG_AI_OUTPUT_USD_PER_MILLION=0.30
```

The API needs the key for safety screening and writing suggestions; the worker needs it for
saved-entry analysis. Journal create/update and draft suggestions now make a synchronous Groq
safety request after the local high-risk rules. Configure the API timeout and provider capacity
accordingly. `MYLOG_SAFETY_CLASSIFIER_TIMEOUT_MS` defaults to 2000; the current local MVP sets
it to 10000.
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

`MYLOG_AI_MVP_ENABLED=true` with `MYLOG_SAFETY_CLASSIFIER_PROVIDER=groq` selects Groq for
safety classification without marking the provider/output review flags as approved. Local
high-risk phrase rules still run first. The Groq result is restricted to LOW, MODERATE, HIGH or
CRITICAL; invalid output, timeout and provider failure fail closed. The fixed confidence value
is an MVP policy marker, not a calibrated model probability. This path records policy version
`mvp-groq-safety-v1`. For isolated comparison, `MYLOG_SAFETY_CLASSIFIER_PROVIDER=rules` restores
the earlier deterministic baseline (`mvp-rules-v1`). Neither path is a trained or evaluated
classifier. Enabling the MVP does not
rescreen older blocked entries in a shared database.

The backend sends journal content to Groq safety only when the user has granted the current
`AI_PROCESSING` consent. Without consent, local high-risk rules still run, the journal is saved,
and ordinary AI analysis remains blocked. Draft suggestions already require consent before
screening. The dedicated `MYLOG_SAFETY_CLASSIFIER_URL`/`TOKEN` adapter remains available when
the MVP is disabled, so a future trained classifier can replace the Groq adapter.

The remaining review and end-to-end checks are tracked in
[AI_MVP_REAL_USER_READINESS.md](AI_MVP_REAL_USER_READINESS.md). Set
`MYLOG_JOBS_ENABLED=false` and `MYLOG_WRITING_SUGGESTIONS_ENABLED=false` to stop AI requests;
restart API and worker after changing them.
