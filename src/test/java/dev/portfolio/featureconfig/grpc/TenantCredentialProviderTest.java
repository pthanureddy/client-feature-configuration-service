package dev.portfolio.featureconfig.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantCredentialProviderTest {
    @Test
    void matchesOnlyConfiguredTenantAndKeyPair() {
        TenantCredentialProvider provider = new TenantCredentialProvider("tenant-a=secret-a,tenant-b=secret-b");

        assertThat(provider.matches("tenant-a", "secret-a")).isTrue();
        assertThat(provider.matches("tenant-a", "secret-b")).isFalse();
        assertThat(provider.matches("tenant-c", "secret-a")).isFalse();
    }

    @Test
    void rejectsEmptyConfiguration() {
        assertThatThrownBy(() -> new TenantCredentialProvider(" , "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
