package dev.portfolio.featureconfig.spanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CloudSpannerSchemaTest {
    @Test
    void schemaDefinesTenantFirstPrimaryKeysAndVersionedConfiguration() throws IOException {
        try (var input = getClass().getResourceAsStream("/spanner/schema.sql")) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("CREATE TABLE FeatureConfigurations")
                    .contains("PRIMARY KEY (TenantId, FeatureKey)")
                    .contains("CREATE TABLE CommandReceipts")
                    .contains("PRIMARY KEY (TenantId, RequestId)")
                    .contains("Version INT64 NOT NULL")
                    .doesNotContain("FLOAT32");
        }
    }
}
