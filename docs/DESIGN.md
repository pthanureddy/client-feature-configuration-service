# Technical Design

## Components

`FeatureConfigurationGrpcService` maps protobuf messages to application commands and maps domain
errors to gRPC statuses. `TenantSecurityInterceptor` validates metadata before the request reaches
the service. `FeatureConfigurationService` owns normalization, validation, canonical hashing, and
lookup behavior. `FeatureConfigurationStore` separates that logic from persistence.

The local adapter synchronizes its mutation boundary so unit tests model atomic version and receipt
checks. The Spanner adapter runs the same sequence inside one read-write transaction.

## Data model

`FeatureConfigurations` uses `(TenantId, FeatureKey)` as its primary key. `CommandReceipts` uses
`(TenantId, RequestId)` so a request ID cannot acquire two meanings inside one tenant. Receipt rows
store the response snapshot required for deterministic retry behavior.

## Algorithms and data structures

- Hash maps provide constant-time expected lookup in the local adapter.
- Sorted, de-duplicated region lists create a stable command representation.
- SHA-256 of canonical command facts distinguishes a valid retry from request-ID misuse.
- Expected-version comparison implements optimistic concurrency with a monotonic version.

## Failure model

Input failures return `INVALID_ARGUMENT`; missing configuration returns `NOT_FOUND`; version and
idempotency conflicts return `ABORTED`; invalid credentials return `UNAUTHENTICATED`. Storage or
transport failures are not translated into false success.

## Scale boundary

Tenant-first keys distribute the logical key space and keep single-configuration access explicit.
This repository does not claim benchmarked scale. Hot tenants, regional placement, staleness modes,
indexes, and request-volume distribution need measured production requirements before optimization.
