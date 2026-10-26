# Product Requirements Document

## Problem

External client teams need a small control-plane service that stores tenant-specific feature
configuration. Retries must not duplicate commands, concurrent administrators must not overwrite
newer state unknowingly, and tenant identity must be checked before application logic executes.

## Users

- Client application developer: reads configuration for one tenant and feature.
- Client platform operator: creates or changes rollout settings.
- Platform engineer: configures persistence, credentials, and operational checks.

## Required outcome

1. Accept a versioned feature configuration through gRPC.
2. Return the committed configuration and its monotonically increasing version.
3. Make a retry with identical command facts safe.
4. Reject request-ID reuse with different facts.
5. Prevent an authenticated tenant from addressing another tenant.
6. Provide a local storage mode and a Google Cloud Spanner adapter behind one port.
7. Make build, verification, security boundaries, and trade-offs reviewable.

## Non-functional requirements

- Validate inputs before storage access.
- Keep command mutation and receipt creation atomic in the Spanner adapter.
- Store tenant ID first in both Spanner primary keys.
- Return stable gRPC status codes for invalid, missing, and conflicting operations.
- Run deterministic unit tests in CI without cloud credentials.

## Out of scope

Production identity, TLS termination, user-facing administration, dynamic percentage bucketing,
event streaming, multi-region deployment, SLOs, billing, and production load testing.

## Success evidence

The Maven build compiles the protobuf and Java sources, automated tests pass, the coverage gate
passes for the core application/gRPC boundary, and CI runs the same verification command.
