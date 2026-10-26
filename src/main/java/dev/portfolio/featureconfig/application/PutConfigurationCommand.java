package dev.portfolio.featureconfig.application;

import java.util.List;

public record PutConfigurationCommand(
        String tenantId,
        String featureKey,
        boolean enabled,
        int rolloutPercentage,
        List<String> allowedRegions,
        long expectedVersion,
        String requestId) {
}
