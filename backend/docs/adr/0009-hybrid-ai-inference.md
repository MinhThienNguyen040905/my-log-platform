# ADR-0009: Hybrid AI inference and separately governed safety training

- Status: Accepted
- Date: 2026-10-04
- Owners: backend team; safety model and policy approval require the safety owner

## Context

The journal is sensitive. The existing ports, PostgreSQL outbox and job worker separate journal
storage from AI calls, but the safety classifier and journal analyzer previously had only
unavailable/fake adapters. Training a generative model and operating several model services
would add cost and operational risk before there is evaluation evidence that they are needed.

## Decision

- Keep the modular monolith and PostgreSQL transactional outbox/job path. A separate Python
  pipeline trains a small, versioned risk classifier and serves inference on a private,
  authenticated endpoint. Spring implements `RiskClassifier` with an HTTP adapter.
- Keep deterministic rules and the approved policy gate as independent controls. Model output
  alone never authorizes ordinary analysis. `MODERATE`/`CONSTRAIN` does not enter the ordinary
  reflection path until a constrained response path is reviewed and implemented.
- Initially use a configurable, worker-only Chat Completions API adapter for `JournalAnalyzer`.
  It is disabled by default. The API credential comes from a secret manager. No provider is
  approved merely because the adapter is present. Provider retention, processing region,
  deletion and no-training terms must satisfy ADR-0004 before user data is sent.
- Keep schema/output checks and require separate output safety evaluation before production.
  An LLM response, including a strict JSON response, is not itself proof of safety.
- Training is outside application requests and production database access. `AI_PROCESSING`
  does not authorize `MODEL_TRAINING`; any user-derived training data needs explicit consent,
  approved handling, and a deletion/withdrawal procedure. No raw journal enters model artifacts,
  logs, outbox or job payloads.
- Keep RAG over approved knowledge separate. Evaluate a pretrained embedding model before
  considering embedding fine-tuning.

## Consequences

- The safety service adds one synchronous, time-bounded internal call; failure preserves the
  journal and blocks generated output. The worker makes the external provider call outside a
  database transaction.
- Model/prompt/policy versions are tracked with existing safety and analysis provenance.
  Any later schema changes use new Flyway migrations; V1-V15 remain immutable.
- A reviewed safety corpus, classifier calibration, provider assessment, output safety suite,
  staging evidence and operational approval are required before production AI rollout.
- If third-party data terms cannot be approved, replace only the `JournalAnalyzer` adapter with
  a self-hosted model; the application contract, consent checks, outbox and jobs remain.

## Migration and rollback

The new adapters are disabled by default. Keep worker jobs disabled until policy/provider gates
are satisfied. Roll back by removing classifier URL and setting `MYLOG_AI_PROVIDER=none`, then
allow pending jobs to wait or fail according to the existing retry policy. No schema rewrite is
needed for this decision.
