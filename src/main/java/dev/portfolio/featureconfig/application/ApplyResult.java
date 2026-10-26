package dev.portfolio.featureconfig.application;

import dev.portfolio.featureconfig.domain.FeatureConfiguration;

public record ApplyResult(FeatureConfiguration configuration, boolean replayed) {
}
