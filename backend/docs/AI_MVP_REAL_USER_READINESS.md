# AI MVP: release status and open checks

Last reviewed: 2026-10-12. The user requested Groq AI across the operational backend and
web product. The Groq MVP is enabled in the current local configuration and the same
configuration path is available to API and worker deployments in other Spring profiles.
This document records the remaining evidence; it does not claim external approval.

## Implemented

- The web exposes authenticated, on-demand draft suggestions and displays saved-entry
  analysis. Backend consent and safety checks precede Groq calls.
- Onboarding and Settings offer versioned `AI_PROCESSING` consent with disclosure of Groq
  processing and a withdrawal control. The backend requires consent version `groq-v1` in
  MVP mode. `MODEL_TRAINING` remains separate.
- The MVP uses curated risk rules and a deterministic classifier fallback, recording
  `mvp-rules-v1` without creating a fictitious approved database policy.
- Groq responses pass schema and output validation. A synthetic Groq smoke request passed;
  no real journal text was used for that check.
- Web TypeScript, lint and build passed. Backend unit/context tests passed; Testcontainers
  integration tests were skipped because Docker was unavailable.

## Remaining checks before broad real-user release

1. Evaluate the baseline against a multilingual corpus, especially misses in high-risk,
   slang, negation and ambiguous cases; replace it with an evaluated classifier/policy.
2. Review generated reflections and questions for harmful or misleading output.
3. Confirm Groq account data controls, retention and processing terms, including Zero Data
   Retention status. See [Groq's data documentation](https://console.groq.com/docs/your-data).
4. Review the `groq-v1` consent wording and withdrawal behavior with the project owner.
5. Run PostgreSQL integration tests and authenticated end-to-end staging scenarios for
   consent grant/revoke, high risk, stale entry version, provider failure and output rejection.

The current MVP baseline is explicitly enabled by `MYLOG_AI_MVP_ENABLED`. The separate
`MYLOG_AI_PROVIDER_DATA_APPROVED` and `MYLOG_AI_OUTPUT_SAFETY_APPROVED` flags remain false
until their corresponding reviews have actually happened. See [AI_MVP_SETUP.md](AI_MVP_SETUP.md).
