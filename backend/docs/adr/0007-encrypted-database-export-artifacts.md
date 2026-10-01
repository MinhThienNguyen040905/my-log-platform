# ADR-0007: Encrypted temporary export artifacts in PostgreSQL

- Status: Accepted
- Date: 2026-10-01
- Owners: backend team

## Context

EXP-001 requires private CSV/PDF exports with short-lived download links. ADR-0006 covers journal images in Cloudinary and explicitly leaves export storage undecided. Export files contain an unusually dense copy of personal data. The current release has no private object-storage adapter for non-image files.

## Decision

- Store each temporary export artifact as an application-encrypted `BYTEA` payload in `export_requests`, using a fresh envelope key and authenticated owner/row AAD.
- Limit a file to 5 MB and retain a ready artifact for at most 24 hours. The worker clears ciphertext and encryption metadata on expiry and removes request metadata after 30 days. Only one active export per user is allowed.
- Issue a 60-second HMAC-signed relative download URL after password reauthentication. Download still requires a valid access JWT, owner-scoped lookup, a valid signature and an unexpired artifact. Responses use `Cache-Control: no-store`.
- Export generation runs outside a database transaction. Claim, result persistence and cleanup use short transactions. A failed job retries up to three times.
- Export data is assembled through feature application facades; the export module does not read another module's JPA entities.

## Consequences

- PostgreSQL stores encrypted temporary bytes, increasing DB size and backup volume. The size cap and expiry limit bound this cost for the current release. This is not suitable for very large exports.
- Expiry cleanup is enabled with the export worker. The worker starts when identity is enabled by default and can be paused with `mylog.exports.enabled=false`; operators monitor failed jobs and expired artifact count.
- Export links are capability URLs, so query strings must not be logged or sent to third parties. Authentication remains mandatory even when the URL leaks.
- Future larger exports may move to dedicated private object storage with a new ADR and migration. Existing encrypted rows can expire naturally; rollback disables the worker and API without decrypting stored artifacts.

