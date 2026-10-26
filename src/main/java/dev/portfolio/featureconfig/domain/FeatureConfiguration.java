package dev.portfolio.featureconfig.domain;

import java.time.Instant;
import java.util.List;

public record FeatureConfiguration(
        String tenantId,
        String featureKey,
        boolean enabled,
        int rolloutPercentage,
        List<String> allowedRegions,
        long version,
        Instant updatedAt) {

    public FeatureConfiguration {
        allowedRegions = List.copyOf(allowedRegions);
    }
}
