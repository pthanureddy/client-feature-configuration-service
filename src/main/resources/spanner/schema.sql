CREATE TABLE FeatureConfigurations (
  TenantId STRING(63) NOT NULL,
  FeatureKey STRING(63) NOT NULL,
  Enabled BOOL NOT NULL,
  RolloutPercentage INT64 NOT NULL,
  AllowedRegions STRING(128) NOT NULL,
  Version INT64 NOT NULL,
  UpdatedAt TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp = false)
) PRIMARY KEY (TenantId, FeatureKey);

CREATE TABLE CommandReceipts (
  TenantId STRING(63) NOT NULL,
  RequestId STRING(128) NOT NULL,
  PayloadHash STRING(64) NOT NULL,
  FeatureKey STRING(63) NOT NULL,
  Enabled BOOL NOT NULL,
  RolloutPercentage INT64 NOT NULL,
  AllowedRegions STRING(128) NOT NULL,
  Version INT64 NOT NULL,
  UpdatedAt TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp = false)
) PRIMARY KEY (TenantId, RequestId);
