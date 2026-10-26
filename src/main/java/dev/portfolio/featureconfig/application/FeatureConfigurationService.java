package dev.portfolio.featureconfig.application;

import dev.portfolio.featureconfig.domain.DomainException;
import dev.portfolio.featureconfig.domain.FeatureConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class FeatureConfigurationService {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9-]{2,62}");
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}");
    private static final Pattern REGION = Pattern.compile("[A-Z]{2}");

    private final FeatureConfigurationStore store;
    private final Clock clock;

    public FeatureConfigurationService(FeatureConfigurationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public ApplyResult put(PutConfigurationCommand command) {
        String tenantId = normalizedIdentifier(command.tenantId(), "tenant_id");
        String featureKey = normalizedIdentifier(command.featureKey(), "feature_key");
        if (command.rolloutPercentage() < 0 || command.rolloutPercentage() > 100) {
            invalid("rollout_percentage must be between 0 and 100");
        }
        if (command.expectedVersion() < 0) {
            invalid("expected_version must not be negative");
        }
        if (command.requestId() == null || !REQUEST_ID.matcher(command.requestId()).matches()) {
            invalid("request_id must be 8-128 safe characters");
        }

        List<String> regions = command.allowedRegions().stream()
                .map(region -> region.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (regions.size() > 20 || regions.stream().anyMatch(region -> !REGION.matcher(region).matches())) {
            invalid("allowed_regions must contain at most 20 ISO-style two-letter codes");
        }

        String canonical = String.join("|",
                tenantId,
                featureKey,
                Boolean.toString(command.enabled()),
                Integer.toString(command.rolloutPercentage()),
                String.join(",", regions),
                Long.toString(command.expectedVersion()));
        return store.apply(
                tenantId,
                featureKey,
                command.enabled(),
                command.rolloutPercentage(),
                regions,
                command.expectedVersion(),
                command.requestId(),
                sha256(canonical),
                clock.instant());
    }

    public FeatureConfiguration get(String tenantId, String featureKey) {
        String normalizedTenant = normalizedIdentifier(tenantId, "tenant_id");
        String normalizedFeature = normalizedIdentifier(featureKey, "feature_key");
        return store.find(normalizedTenant, normalizedFeature)
                .orElseThrow(() -> new DomainException(
                        DomainException.Reason.NOT_FOUND, "configuration was not found"));
    }

    private static String normalizedIdentifier(String value, String field) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(normalized).matches()) {
            invalid(field + " must match " + IDENTIFIER.pattern());
        }
        return normalized;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", impossible);
        }
    }

    private static void invalid(String message) {
        throw new DomainException(DomainException.Reason.INVALID_ARGUMENT, message);
    }
}
