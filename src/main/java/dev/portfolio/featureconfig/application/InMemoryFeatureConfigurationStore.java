package dev.portfolio.featureconfig.application;

import dev.portfolio.featureconfig.domain.DomainException;
import dev.portfolio.featureconfig.domain.FeatureConfiguration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryFeatureConfigurationStore implements FeatureConfigurationStore {
    private final Map<String, FeatureConfiguration> configurations = new HashMap<>();
    private final Map<String, Receipt> receipts = new HashMap<>();

    @Override
    public synchronized ApplyResult apply(
            String tenantId,
            String featureKey,
            boolean enabled,
            int rolloutPercentage,
            List<String> allowedRegions,
            long expectedVersion,
            String requestId,
            String payloadHash,
            Instant now) {
        String receiptKey = tenantId + "\u0000" + requestId;
        Receipt receipt = receipts.get(receiptKey);
        if (receipt != null) {
            if (!receipt.payloadHash().equals(payloadHash)) {
                throw new DomainException(
                        DomainException.Reason.IDEMPOTENCY_CONFLICT,
                        "request_id was already used with different command facts");
            }
            return new ApplyResult(receipt.configuration(), true);
        }

        String configKey = tenantId + "\u0000" + featureKey;
        FeatureConfiguration current = configurations.get(configKey);
        long currentVersion = current == null ? 0 : current.version();
        if (expectedVersion != currentVersion) {
            throw new DomainException(
                    DomainException.Reason.VERSION_CONFLICT,
                    "expected version " + expectedVersion + " but current version is " + currentVersion);
        }

        FeatureConfiguration saved = new FeatureConfiguration(
                tenantId,
                featureKey,
                enabled,
                rolloutPercentage,
                allowedRegions,
                currentVersion + 1,
                now);
        configurations.put(configKey, saved);
        receipts.put(receiptKey, new Receipt(payloadHash, saved));
        return new ApplyResult(saved, false);
    }

    @Override
    public synchronized Optional<FeatureConfiguration> find(String tenantId, String featureKey) {
        return Optional.ofNullable(configurations.get(tenantId + "\u0000" + featureKey));
    }

    private record Receipt(String payloadHash, FeatureConfiguration configuration) {
    }
}
