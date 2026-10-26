package dev.portfolio.featureconfig.application;

import dev.portfolio.featureconfig.domain.FeatureConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FeatureConfigurationStore {
    ApplyResult apply(
            String tenantId,
            String featureKey,
            boolean enabled,
            int rolloutPercentage,
            List<String> allowedRegions,
            long expectedVersion,
            String requestId,
            String payloadHash,
            Instant now);

    Optional<FeatureConfiguration> find(String tenantId, String featureKey);
}
