# Client Feature Configuration Service

A Java 17 service for external client applications to read and update tenant-isolated feature
configuration over gRPC. The service implements optimistic concurrency, idempotent commands,
tenant-bound API-key authentication, and a Google Cloud Spanner storage adapter.

This is an independent portfolio project built with synthetic tenants. It is not affiliated with
EPAM or a client, has not been deployed to Google Cloud, and does not claim production usage.

## Client problem and bounded outcome

Client application teams need to roll features out by tenant and region without allowing one
tenant to read another tenant's settings or letting retries apply an update twice. The bounded
solution provides two gRPC operations:

- `PutConfiguration`: create or update a feature with an expected version and request ID.
- `GetConfiguration`: read one tenant's feature configuration.

The full product framing is in [the PRD](docs/PRD.md). Requirements and acceptance criteria are
mapped to implementation and tests in [requirements traceability](docs/REQUIREMENTS.md).

## Architecture

```text
external client
    |
    | gRPC + x-tenant-id + x-api-key
    v
tenant security interceptor
    |
    v
gRPC mapping -> application service -> storage port
                                      |-- in-memory adapter (local/tests)
                                      +-- Cloud Spanner adapter (configured runtime)
```

The application layer normalizes identifiers and regions, validates rollout percentages, hashes
canonical command facts, and passes one atomic operation to the storage port. The Spanner adapter
runs the version check, feature mutation, and command-receipt insert in one read-write transaction.
An identical retry returns the saved response; a reused request ID with different facts is rejected.

See [design](docs/DESIGN.md), [security](docs/SECURITY.md), and
[ADR 0001](docs/decisions/0001-spanner-transaction-and-command-receipts.md).

## Build and test

Prerequisite: Java 17+. A system Maven installation is not required.

```powershell
.\mvnw.cmd --batch-mode clean verify
```

On Linux or macOS:

```bash
./mvnw --batch-mode clean verify
```

The build compiles the protobuf contract, runs JUnit tests, produces a JaCoCo report, checks a 65%
line-coverage floor for the core application/gRPC code, and packages the Spring Boot application.
The Spanner adapter and configuration wiring are excluded from that floor because they require an
emulator or Google Cloud integration environment; the DDL contract is still checked in the unit
suite. GitHub Actions runs the same Maven verification command.

Current measured verification: 14 automated tests pass, core application/gRPC line coverage is
94.38% (151 of 160 lines), and the packaged application remained running through a five-second
startup smoke check. Generated protobuf classes, Spring wiring, the server lifecycle, and the
Spanner adapter are outside that core coverage figure.

## Local run

The default mode uses in-memory storage and a conspicuous local-only credential.

```powershell
.\mvnw.cmd spring-boot:run
```

With `grpcurl` installed, import the proto and call the service:

```bash
grpcurl -plaintext \
  -H 'x-tenant-id: demo-tenant' \
  -H 'x-api-key: local-development-only' \
  -import-path src/main/proto -proto feature_configuration.proto \
  -d '{"tenantId":"demo-tenant","featureKey":"new-checkout","enabled":true,"rolloutPercentage":25,"allowedRegions":["SE"],"expectedVersion":"0","requestId":"request-0001"}' \
  localhost:9090 featureconfig.v1.FeatureConfigurationService/PutConfiguration
```

The in-memory mode is intentionally ephemeral and is not a production persistence claim.

## Cloud Spanner configuration

Apply [`src/main/resources/spanner/schema.sql`](src/main/resources/spanner/schema.sql) to a database,
then configure:

```text
SPANNER_ENABLED=true
SPANNER_PROJECT_ID=<project>
SPANNER_INSTANCE_ID=<instance>
SPANNER_DATABASE_ID=<database>
TENANT_API_KEYS=tenant-a=<secret>,tenant-b=<secret>
```

For the Cloud Spanner emulator, also set `SPANNER_EMULATOR_HOST=localhost:9010`. The emulator still
needs an instance, database, and schema created through `gcloud spanner`; those provisioning steps
are intentionally not hidden inside application startup. No Google Cloud resources were provisioned
for this portfolio project.

## Verification scope

Automated tests cover:

- create and versioned update behavior;
- stale-writer rejection;
- identical command replay and changed-payload request-ID conflict;
- identifier, rollout, region, and request-ID validation;
- gRPC create/read flow and status mapping;
- missing credentials and cross-tenant request rejection;
- Cloud Spanner DDL key and version fields.

Detailed cases and manual emulator checks are in [test cases](docs/TEST_CASES.md).

## Quality-speed decisions

- A synchronous gRPC API and Spanner transaction give a small, reviewable correctness boundary.
- Static tenant API keys are adequate for a local portfolio boundary, not for production identity.
- Allowed regions are stored as a bounded canonical string to keep the sample schema compact; a
  production model could normalize regions or use a typed array after query-pattern analysis.
- The application starts with in-memory storage unless Spanner is explicitly enabled, preventing a
  local demo from silently implying a cloud deployment.

## Limitations

- No production deployment, traffic, latency, availability, or customer-impact claim is made.
- Cloud Spanner code is compiled and the DDL is tested, but this repository does not publish an
  emulator-backed integration result.
- API-key values are process configuration; production should use workload identity, Secret Manager,
  TLS/mTLS, key rotation, and centralized authorization.
- No list API, audit-retention policy, streaming update channel, percentage-bucketing algorithm,
  administrative UI, or client SDK is included.
- In-memory state is lost on restart. The default profile is for deterministic local evaluation.

## Documentation

- [Product requirements](docs/PRD.md)
- [Requirements and verification](docs/REQUIREMENTS.md)
- [Technical design](docs/DESIGN.md)
- [Test cases](docs/TEST_CASES.md)
- [Security boundary](docs/SECURITY.md)
- [Operations](docs/OPERATIONS.md)
- [Architecture decision record](docs/decisions/0001-spanner-transaction-and-command-receipts.md)

## License

Released under the MIT License.
