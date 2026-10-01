# M8 threat model (2026-10-01)

Scope: backend HTTP API, workers, PostgreSQL, Redis, AI provider boundary and Cloudinary. Data classification: journal, identity, profile, AI narrative and exports are highly sensitive. Review this document after a new provider, storage adapter or public endpoint is added.

| Boundary | Credible abuse/failure | Current control | Release check |
|---|---|---|---|
| Login/session | Credential stuffing, token theft/replay, enumeration | IP/email quotas, BCrypt, short JWT, hashed refresh rotation and family reuse detection, generic auth errors | Re-run race/replay tests, inspect production auth logs without credentials, exercise key rotation |
| Journal/check-in | Cross-account read, injection in AI prompt, excessive writes | Owner-scoped queries, ciphertext at rest, idempotency/version checks, 60 journal/tag writes per user per 15 minutes | Run endpoint authorization matrix and load test with synthetic journals |
| Export/deletion | Download URL leak, stale session, incomplete purge | Password reauthentication, 60-second signed owner-scoped download, no-store, encrypted 24-hour artifact, revocation and deletion checkpoints | Test Cloudinary destroy with sandbox account and restore/deletion interaction |
| Admin | Permission escalation, access to sensitive content | Permission checks, metadata audit, deny-by-default routes; admin has no general raw-journal read path | Require MFA for production admin and independent permission review |
| AI/safety | Unsafe output, provider retention, consent race, timeout | Fail-safe policy gate, consent check at job execution, output validation, versioned jobs; no live provider configured | Approve policy/resources, evaluate labeled safety corpus, verify provider retention/opt-out |
| Cloudinary | Public asset enumeration, orphaned assets | Signed delivery, DB ownership, deletion adapter | Verify access controls and destroy with sandbox account |
| Database/Supabase | Direct Data API bypass, overprivileged app credential, lost backup | RLS plus revoked anon/authenticated grants (V15), TLS connection, Flyway history | Least-privilege credential review, restore drill, backup retention after deletion |
| Worker | Duplicate claim, crash after external call, poisoned job | PostgreSQL leases, retry/checkpoints, idempotent handlers and dead state | Crash/reclaim and provider timeout drills under load |

Residual blockers: no approved safety contacts/policy or real classifier, no approved external AI provider, no production admin MFA, and no staging evidence or restore drill. These are release blockers, not test fixtures to bypass.
