# ADR-0012: Groq safety classification for the MVP

- Status: Accepted
- Date: 2026-10-12
- Owners: project team
- Supersedes: the deterministic safety classifier selection in ADR-0011

## Context

The product owner wants to use the existing Groq account to finish the MVP before replacing
safety classification with an internally trained model. The deterministic MVP baseline cannot
recognize many indirect or contextual expressions. A Groq result is still not an evaluated
safety classifier.

## Decision

- Keep the `RiskClassifier` application port. With `MYLOG_AI_MVP_ENABLED=true` and
  `MYLOG_SAFETY_CLASSIFIER_PROVIDER=groq`, call Groq through a backend adapter. The existing
  curated HIGH/CRITICAL rules run first; Groq classifies remaining text into LOW, MODERATE,
  HIGH or CRITICAL with strict structured output. The policy marker is `mvp-groq-safety-v1`.
- Send journal text to Groq safety only after versioned `AI_PROCESSING` consent. Without consent,
  save the journal and block ordinary analysis. Draft suggestions already check consent first.
- Treat timeout, refusal, malformed output and provider failure as `FAIL_SAFE`. Do not fall back
  to a permissive rule classifier after Groq fails. A fixed confidence marker satisfies the MVP
  policy contract; it is not a probability or evidence of model quality.
- Preserve the internal HTTP classifier adapter and policy gate for a later trained model.
  `MYLOG_SAFETY_CLASSIFIER_PROVIDER=rules` remains available for isolated comparison.

## Consequences

Journal create/update and draft suggestions incur an additional synchronous provider call,
latency and cost. Groq sees journal content only with consent, but provider data controls and
safety quality still need review before broad real-user release. See
[AI MVP readiness](../AI_MVP_REAL_USER_READINESS.md).

## Rollback

Set `MYLOG_SAFETY_CLASSIFIER_PROVIDER=rules` to restore the previous MVP classifier, or disable
the AI MVP and configure the approved internal classifier/policy path. Restart the API. Neither
change automatically rescreens older blocked entries.
