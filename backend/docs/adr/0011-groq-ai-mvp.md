# ADR-0011: Groq AI for the backend and web MVP

- Status: Accepted
- Date: 2026-10-12
- Owners: project team
- Amends: ADR-0003, ADR-0009 and ADR-0010 for the MVP configuration

## Context

The Spring backend and Next.js web app already have Groq adapters for saved-entry analysis
and on-demand draft suggestions. Requiring a trained safety classifier and recorded provider
and output approvals left those product flows unavailable. The project owner requested AI
across the operational product before the separate safety training work is complete.

## Decision

- Add explicit `MYLOG_AI_MVP_ENABLED` for any Spring profile. It selects the deterministic
  curated-rule safety baseline and permits the configured Groq adapters without recording
  false provider or output approvals. Default remains off for environments lacking Groq setup.
- Keep high-risk phrase screening, consent checks, output validation, per-user suggestion
  limits and the outbox worker path. Do not rescreen previously blocked entries when the
  MVP flag is first enabled on a shared database.
- Require version `groq-v1` of `AI_PROCESSING` consent. Web onboarding and Settings disclose
  Groq processing and allow granting or withdrawing that consent.
- Show AI controls in the web app. The mobile app remains an Expo starter and has no journal
  client yet; the Python recommendation and safety training tools remain separate.

## Consequences

The baseline is not an evaluated classifier and can miss risky content. Output checks are
not a complete clinical safety review. Provider data controls and authenticated end-to-end
scenarios still require review before broad real-user release. See
[AI_MVP_REAL_USER_READINESS.md](../AI_MVP_REAL_USER_READINESS.md).

## Rollback

Disable `MYLOG_JOBS_ENABLED` and `MYLOG_WRITING_SUGGESTIONS_ENABLED`, then restart API and
worker. `MYLOG_AI_MVP_ENABLED=false` restores the approved classifier/policy path.
