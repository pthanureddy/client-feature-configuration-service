package dev.portfolio.featureconfig.config;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import dev.portfolio.featureconfig.application.FeatureConfigurationStore;
import dev.portfolio.featureconfig.spanner.CloudSpannerFeatureConfigurationStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "feature-config.spanner.enabled", havingValue = "true")
public class CloudSpannerConfiguration {
    @Bean(destroyMethod = "close")
    Spanner spanner(
            @Value("${feature-config.spanner.project-id}") String projectId,
            @Value("${feature-config.spanner.emulator-host:}") String emulatorHost) {
        SpannerOptions.Builder builder = SpannerOptions.newBuilder().setProjectId(projectId);
        if (!emulatorHost.isBlank()) {
            builder.setEmulatorHost(emulatorHost);
        }
        return builder.build().getService();
    }

    @Bean
    DatabaseClient databaseClient(
            Spanner spanner,
            @Value("${feature-config.spanner.project-id}") String projectId,
            @Value("${feature-config.spanner.instance-id}") String instanceId,
            @Value("${feature-config.spanner.database-id}") String databaseId) {
        return spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));
    }

    @Bean
    FeatureConfigurationStore cloudSpannerFeatureConfigurationStore(DatabaseClient databaseClient) {
        return new CloudSpannerFeatureConfigurationStore(databaseClient);
    }
}
