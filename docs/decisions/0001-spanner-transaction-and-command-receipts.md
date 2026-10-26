# ADR 0001: Spanner transaction with command receipts

## Status

Accepted for this bounded portfolio service.

## Context

Clients retry commands when deadlines or networks fail. A retry must not create a second semantic
update, while concurrent operators must not overwrite a version they did not read.

## Decision

Use one read-write transaction to read a tenant/request receipt, read the current feature version,
write the next configuration, and insert a receipt snapshot. Hash canonical command facts so the
same request ID can be replayed only when its meaning is unchanged.

## Consequences

The correctness rule is explicit and testable. Receipt storage grows with commands and needs a
retention policy in a real system. Spanner transaction retries and hot-key distribution require
production measurements; no performance claim is made here.
