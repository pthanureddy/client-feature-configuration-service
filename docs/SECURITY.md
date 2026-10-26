# Security Boundary

## Implemented

- Tenant ID and API key are required in gRPC metadata.
- API keys are compared with `MessageDigest.isEqual`.
- The authenticated tenant must equal the tenant in the protobuf request.
- Identifiers, regions, request IDs, versions, and percentages are bounded before persistence.
- No credential is written to logs or source as a production secret.
- The Spanner adapter uses the typed client API and bound values rather than string-built SQL.

## Deliberate limitations

The default credential is clearly local-only. Production use requires TLS or mTLS, workload or user
identity, Secret Manager, rotation, rate limiting, audit export, authorization policy, and tested
incident procedures. The service does not process personal data and uses synthetic tenant names.

The application must not be exposed publicly with its default configuration.
