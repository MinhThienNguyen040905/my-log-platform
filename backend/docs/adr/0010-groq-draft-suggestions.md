# ADR-0010: Groq for ephemeral writing suggestions

- Status: Accepted
- Date: 2026-10-12
- Owners: backend team

## Context

ADR-0004 places provider calls after safety screening and generally in async workers. ADR-0009
keeps journal analysis in a worker. A suggestion for an unsaved draft must return during the
editor request, so the existing worker result path cannot serve this use case.

## Decision

- Keep journal analysis in its existing outbox/job worker. Add Groq as a configurable
  Chat Completions provider without changing the `JournalAnalyzer` application port.
- Keep draft suggestions in the authenticated backend API. Check `AI_PROCESSING` consent and
  the approved safety decision before making a synchronous, time-bounded Groq call. Drafts
  are not persisted by this endpoint, and `SAFETY_FLOW`, `CONSTRAIN`, and `FAIL_SAFE` never
  call the provider.
- Require explicit enablement and provider/output-safety approvals. Use Structured Outputs
  with server-side validation. Provider failures return `UNAVAILABLE` without logging the
  draft or raw provider response.
- Inject the Groq credential into the API instance only when draft generation is enabled.
  The worker still needs its own credential for journal analysis. Never send the credential
  to the browser.
- This adapter does not change the approved classifier/policy requirement or authorize
  sending real journal data before the provider data-handling review is complete.

## Consequences

Draft requests can incur provider latency and cost on the API path. The endpoint retains its
per-user quota and the provider call has a timeout. Output validation is a baseline and still
requires quality and safety evaluation before use with real data. The frontend must explicitly
connect the editor to this endpoint; backend configuration alone does not provide suggestions
in the UI.

## Migration and rollback

No schema migration is needed. Set `MYLOG_WRITING_SUGGESTIONS_AI_ENABLED=false` to return to
the fixed backend prompts, or `MYLOG_WRITING_SUGGESTIONS_ENABLED=false` to disable suggestions.
Set `MYLOG_AI_PROVIDER=none` and stop AI jobs to disable journal analysis calls.
