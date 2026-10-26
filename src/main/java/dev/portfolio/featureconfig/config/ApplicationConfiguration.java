package dev.portfolio.featureconfig.config;

import dev.portfolio.featureconfig.application.FeatureConfigurationService;
import dev.portfolio.featureconfig.application.FeatureConfigurationStore;
import dev.portfolio.featureconfig.application.InMemoryFeatureConfigurationStore;
import dev.portfolio.featureconfig.grpc.FeatureConfigurationGrpcService;
import dev.portfolio.featureconfig.grpc.GrpcServerLifecycle;
import dev.portfolio.featureconfig.grpc.TenantCredentialProvider;
import dev.portfolio.featureconfig.grpc.TenantSecurityInterceptor;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(
            name = "feature-config.spanner.enabled",
            havingValue = "false",
            matchIfMissing = true)
    FeatureConfigurationStore inMemoryFeatureConfigurationStore() {
        return new InMemoryFeatureConfigurationStore();
    }

    @Bean
    FeatureConfigurationService featureConfigurationService(
            FeatureConfigurationStore store, Clock clock) {
        return new FeatureConfigurationService(store, clock);
    }

    @Bean
    FeatureConfigurationGrpcService featureConfigurationGrpcService(
            FeatureConfigurationService service) {
        return new FeatureConfigurationGrpcService(service);
    }

    @Bean
    TenantCredentialProvider tenantCredentialProvider(
            @Value("${feature-config.tenant-api-keys}") String tenantApiKeys) {
        return new TenantCredentialProvider(tenantApiKeys);
    }

    @Bean
    TenantSecurityInterceptor tenantSecurityInterceptor(TenantCredentialProvider credentials) {
        return new TenantSecurityInterceptor(credentials);
    }

    @Bean
    GrpcServerLifecycle grpcServerLifecycle(
            @Value("${feature-config.grpc-port:9090}") int port,
            FeatureConfigurationGrpcService service,
            TenantSecurityInterceptor interceptor) {
        return new GrpcServerLifecycle(port, service, interceptor);
    }
}
