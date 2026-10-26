package dev.portfolio.featureconfig.grpc;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public final class TenantCredentialProvider {
    private final Map<String, String> credentials;

    public TenantCredentialProvider(String configuredCredentials) {
        credentials = Arrays.stream(configuredCredentials.split(","))
                .map(String::strip)
                .filter(entry -> !entry.isEmpty())
                .map(entry -> entry.split("=", 2))
                .filter(parts -> parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank())
                .collect(Collectors.toUnmodifiableMap(parts -> parts[0].strip(), parts -> parts[1].strip()));
        if (credentials.isEmpty()) {
            throw new IllegalArgumentException("at least one tenant API key must be configured");
        }
    }

    public boolean matches(String tenantId, String suppliedKey) {
        if (tenantId == null || suppliedKey == null) {
            return false;
        }
        String expected = credentials.get(tenantId);
        return expected != null && java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                suppliedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
