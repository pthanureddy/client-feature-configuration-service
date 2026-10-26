# Operations

## Startup checks

1. Supply non-default `TENANT_API_KEYS`.
2. Confirm whether in-memory or Spanner storage is intended.
3. For Spanner, verify project, instance, database, schema, credentials, and endpoint.
4. Bind the gRPC port only behind the intended network/TLS boundary.

## Diagnosis

- `UNAUTHENTICATED`: check tenant header, key provisioning, and rotation state.
- `INVALID_ARGUMENT`: inspect the bounded field in the status description.
- `ABORTED` with version text: re-read state and retry with the current version after user review.
- `ABORTED` with request-ID text: do not retry with changed facts; issue a new request ID.
- Spanner failure: check endpoint, credentials, database existence, DDL, and transaction diagnostics.

Root-cause work should preserve the failing request facts, status code, and request ID while removing
secrets. Do not bypass version or tenant checks to clear an incident.

## Release gate

Run `mvnw clean verify`, review dependency and schema changes, confirm the protobuf change is
backward-compatible, and record any manual emulator result separately from automated CI evidence.
