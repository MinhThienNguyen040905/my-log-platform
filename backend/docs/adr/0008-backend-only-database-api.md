# ADR-0008: Backend-only access to application tables

- Status: Accepted
- Date: 2026-10-01
- Owners: backend/operations team

## Context

Supabase grants `anon` and `authenticated` access to tables in the `public` schema by default. Read-only inspection after V14 found those roles had `SELECT` on all application tables while RLS was disabled. This bypassed the backend's owner checks and API authorization.

## Decision

- The Spring backend is the only application data API. Direct Supabase Data API access to application tables is denied.
- V15 revokes table privileges from `PUBLIC`, `anon` and `authenticated` and enables RLS without client policies on all existing `public` tables. The backend's current database owner connection bypasses RLS; backend queries still require explicit owner scope.
- New migrations must revoke direct client-role grants and enable RLS for every new table before deployment. Check live Supabase privileges and RLS status after applying migrations.
- Per-user RLS based on a connection-pool session variable remains a separate future design. It needs reliable set/reset semantics and dedicated integration tests before a non-owner backend role can be used.

## Consequences

- Existing direct PostgREST/GraphQL clients for these tables lose access. The documented frontend path is through Spring APIs.
- Database owner or BYPASSRLS roles can still access tables; credentials for those roles must stay server-side. RLS here blocks direct client roles and does not replace feature owner checks.
- Rollback would require a reviewed new migration restoring only explicitly approved grants and policies. Do not re-enable broad public-schema defaults.

