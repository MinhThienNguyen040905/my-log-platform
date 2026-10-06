# M8 staging and release runbook

Use synthetic data only in staging. The release owner records date, image digest, Flyway version, test report, scan reports, backup identifier, restore evidence and sign-off in the release ticket. Do not put credentials, journal content or signed export links in that ticket.

## Before rollout

1. Run `./mvnw.cmd verify` and `./scripts/check-migrations.ps1` in `backend/`; CI must pass dependency and secret scans. Run a container vulnerability scan on the exact image digest. Resolve critical findings or document a reviewed exception.
2. Confirm production/staging secrets are injected by a secret manager, CORS origins are exact HTTPS origins, SMTP and Cloudinary configuration match the target environment, and the application has no local/test key fallback. Confirm admin MFA and provider retention terms before enabling their paths.
3. Take and identify a PostgreSQL backup. Restore it into an isolated environment, validate Flyway version, representative row counts and ability to decrypt a synthetic record using the backed-up key version. Record restore time and deletion/backup retention handling.
4. Run Flyway once as a controlled pre-deploy job from the release image: `java -jar app.jar --spring.profiles.active=staging,migrate` (or `prod,migrate`) with DB credentials from the secret manager. The `migrate` profile has no HTTP server or workers and exits after startup Flyway validation/migration. Ensure only one such job runs and require exit code 0. Staging/prod application instances default to Flyway disabled. Validate migration checksum/history and schema permissions before app rollout. Never edit an applied migration or auto-rollback a destructive schema change.
5. Deploy API with `MYLOG_APP_PROFILE=api` and worker with `MYLOG_APP_PROFILE=worker`, using the same immutable image digest. Set worker switches explicitly (`MYLOG_JOBS_ENABLED`, `MYLOG_REPORTS_ENABLED`, `MYLOG_EXPORTS_ENABLED`, `MYLOG_DELETION_ENABLED`). Restrict worker network ingress to health probes. `all` is for local/test only.
6. Probe `/actuator/health/liveness` and `/actuator/health/readiness`; readiness includes PostgreSQL in staging/prod. Keep detailed health, metrics and DB access on internal networks. Observe HTTP error/latency, Hikari pool, outbox/AI queue depth and age, dead jobs, safety dependency failures, deletion retries and disk usage.
7. Run a synthetic end-to-end demo: register/verify/login; journal/check-in; consent; approved safety route; AI job if approved provider exists; dashboard/report; export; deletion cancellation and processing. Confirm 401/403/404 owner and permission cases.

## AI rollout gate

The adapters added by ADR-0009 are disabled by default. Before setting
`MYLOG_SAFETY_CLASSIFIER_URL` or `MYLOG_AI_PROVIDER=openai` on a worker:

1. Review the classifier artifact and holdout evaluation, including HIGH/CRITICAL misses,
   Vietnamese/English slices, negation and quotations. Record model version, checksum,
   approver and rollback artifact. Serve inference only on a private authenticated TLS endpoint.
2. Approve an effective `safety_policy_versions` row matching rule version,
   classifier provider `MYLOG_INTERNAL`, classifier model version and confidence threshold.
   Review safety resources and the separate `CONSTRAIN` response path before enabling those flows.
3. Verify provider no-training, retention, processing region and deletion terms against ADR-0004.
   Supply `MYLOG_AI_MODEL` and `MYLOG_AI_API_KEY` from a secret manager only to the worker.
   Set `MYLOG_AI_INPUT_USD_PER_MILLION` and `MYLOG_AI_OUTPUT_USD_PER_MILLION` to the
   reviewed model prices so usage estimates are meaningful; update them when prices change.
   The adapter sets `store=false`; that flag alone does not approve a provider.
4. Pass a reviewed output safety evaluation and synthetic end-to-end/staging exercise.
   The current validator is a baseline only. Record the decision before enabling ordinary
   analysis for user journal content. Set `MYLOG_AI_PROVIDER_DATA_APPROVED=true` and
   `MYLOG_AI_OUTPUT_SAFETY_APPROVED=true` only after the corresponding review evidence is
   recorded. If any gate is unmet, keep jobs/provider disabled.

## Incident actions

- Provider timeout or unsafe output: disable `MYLOG_JOBS_ENABLED`, preserve journal/outbox, inspect aggregate error codes and policy version. Do not copy raw prompts or output into logs/tickets. Resume only after safety review.
- Queue stuck or worker crash: inspect lease age and dead/retry counts, restart worker after DB readiness, verify claim/reclaim with a synthetic job. Do not manually mark a job successful without checking content version.
- Deletion retry: inspect checkpoint and Cloudinary status through approved admin tooling; never reset account to ACTIVE to bypass cleanup. Preserve the minimal pseudonymous audit and rerun the idempotent worker after fixing dependency.
- Database incident: stop writes, restore into an isolated environment first, reconcile data created since backup. A restored backup can reintroduce data previously deleted; rerun deletion reconciliation before serving traffic.
- Rollback: roll back the application image only if it is compatible with the migrated schema. Forward-fix incompatible schema with a new Flyway migration; do not reverse an applied destructive migration in place.

Release remains blocked until the open M8 checklist items, prior milestone external gates, privacy/safety sign-off and staging evidence are complete.
