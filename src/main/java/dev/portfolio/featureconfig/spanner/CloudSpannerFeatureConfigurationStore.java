package dev.portfolio.featureconfig.spanner;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Struct;
import dev.portfolio.featureconfig.application.ApplyResult;
import dev.portfolio.featureconfig.application.FeatureConfigurationStore;
import dev.portfolio.featureconfig.domain.DomainException;
import dev.portfolio.featureconfig.domain.FeatureConfiguration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CloudSpannerFeatureConfigurationStore implements FeatureConfigurationStore {
    private static final List<String> CONFIG_COLUMNS = List.of(
            "TenantId", "FeatureKey", "Enabled", "RolloutPercentage",
            "AllowedRegions", "Version", "UpdatedAt");
    private static final List<String> RECEIPT_COLUMNS = List.of(
            "PayloadHash", "FeatureKey", "Enabled", "RolloutPercentage",
            "AllowedRegions", "Version", "UpdatedAt");

    private final DatabaseClient database;

    public CloudSpannerFeatureConfigurationStore(DatabaseClient database) {
        this.database = database;
    }

    @Override
    public ApplyResult apply(
            String tenantId,
            String featureKey,
            boolean enabled,
            int rolloutPercentage,
            List<String> allowedRegions,
            long expectedVersion,
            String requestId,
            String payloadHash,
            Instant now) {
        return database.readWriteTransaction().run(transaction -> {
            Struct priorReceipt = transaction.readRow(
                    "CommandReceipts", Key.of(tenantId, requestId), RECEIPT_COLUMNS);
            if (priorReceipt != null) {
                if (!priorReceipt.getString("PayloadHash").equals(payloadHash)) {
                    throw new DomainException(
                            DomainException.Reason.IDEMPOTENCY_CONFLICT,
                            "request_id was already used with different command facts");
                }
                return new ApplyResult(configurationFromReceipt(tenantId, priorReceipt), true);
            }

            Struct currentRow = transaction.readRow(
                    "FeatureConfigurations", Key.of(tenantId, featureKey), CONFIG_COLUMNS);
            long currentVersion = currentRow == null ? 0 : currentRow.getLong("Version");
            if (expectedVersion != currentVersion) {
                throw new DomainException(
                        DomainException.Reason.VERSION_CONFLICT,
                        "expected version " + expectedVersion + " but current version is " + currentVersion);
            }

            long newVersion = currentVersion + 1;
            String regionValue = String.join(",", allowedRegions);
            Timestamp updatedAt = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());
            transaction.buffer(Mutation.newInsertOrUpdateBuilder("FeatureConfigurations")
                    .set("TenantId").to(tenantId)
                    .set("FeatureKey").to(featureKey)
                    .set("Enabled").to(enabled)
                    .set("RolloutPercentage").to((long) rolloutPercentage)
                    .set("AllowedRegions").to(regionValue)
                    .set("Version").to(newVersion)
                    .set("UpdatedAt").to(updatedAt)
                    .build());
            transaction.buffer(Mutation.newInsertBuilder("CommandReceipts")
                    .set("TenantId").to(tenantId)
                    .set("RequestId").to(requestId)
                    .set("PayloadHash").to(payloadHash)
                    .set("FeatureKey").to(featureKey)
                    .set("Enabled").to(enabled)
                    .set("RolloutPercentage").to((long) rolloutPercentage)
                    .set("AllowedRegions").to(regionValue)
                    .set("Version").to(newVersion)
                    .set("UpdatedAt").to(updatedAt)
                    .build());
            FeatureConfiguration saved = new FeatureConfiguration(
                    tenantId, featureKey, enabled, rolloutPercentage, allowedRegions, newVersion, now);
            return new ApplyResult(saved, false);
        });
    }

    @Override
    public Optional<FeatureConfiguration> find(String tenantId, String featureKey) {
        Struct row = database.singleUse().readRow(
                "FeatureConfigurations", Key.of(tenantId, featureKey), CONFIG_COLUMNS);
        return Optional.ofNullable(row).map(this::configurationFromRow);
    }

    private FeatureConfiguration configurationFromRow(Struct row) {
        return new FeatureConfiguration(
                row.getString("TenantId"),
                row.getString("FeatureKey"),
                row.getBoolean("Enabled"),
                Math.toIntExact(row.getLong("RolloutPercentage")),
                regions(row.getString("AllowedRegions")),
                row.getLong("Version"),
                row.getTimestamp("UpdatedAt").toSqlTimestamp().toInstant());
    }

    private FeatureConfiguration configurationFromReceipt(String tenantId, Struct row) {
        return new FeatureConfiguration(
                tenantId,
                row.getString("FeatureKey"),
                row.getBoolean("Enabled"),
                Math.toIntExact(row.getLong("RolloutPercentage")),
                regions(row.getString("AllowedRegions")),
                row.getLong("Version"),
                row.getTimestamp("UpdatedAt").toSqlTimestamp().toInstant());
    }

    private static List<String> regions(String value) {
        return value.isBlank() ? List.of() : Arrays.asList(value.split(","));
    }
}
