package dev.portfolio.featureconfig.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portfolio.featureconfig.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureConfigurationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private FeatureConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new FeatureConfigurationService(
                new InMemoryFeatureConfigurationStore(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsConfigurationAtVersionOneAndNormalizesRegions() {
        ApplyResult result = service.put(command(false, 0, "request-0001", List.of("se", "DE", "SE")));

        assertThat(result.replayed()).isFalse();
        assertThat(result.configuration().version()).isEqualTo(1);
        assertThat(result.configuration().allowedRegions()).containsExactly("DE", "SE");
        assertThat(result.configuration().updatedAt()).isEqualTo(NOW);
    }

    @Test
    void updatesOnlyWhenExpectedVersionMatches() {
        service.put(command(false, 0, "request-0001", List.of("SE")));

        ApplyResult updated = service.put(command(true, 1, "request-0002", List.of("SE")));

        assertThat(updated.configuration().version()).isEqualTo(2);
        assertThat(updated.configuration().enabled()).isTrue();
    }

    @Test
    void rejectsStaleWriter() {
        service.put(command(false, 0, "request-0001", List.of("SE")));

        assertThatThrownBy(() -> service.put(command(true, 0, "request-0002", List.of("SE"))))
                .isInstanceOf(DomainException.class)
                .extracting(exception -> ((DomainException) exception).reason())
                .isEqualTo(DomainException.Reason.VERSION_CONFLICT);
    }

    @Test
    void replaysIdenticalCommandWithoutSecondMutation() {
        PutConfigurationCommand original = command(true, 0, "request-0001", List.of("SE"));
        ApplyResult first = service.put(original);
        ApplyResult replay = service.put(original);

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.configuration()).isEqualTo(first.configuration());
        assertThat(service.get("demo-tenant", "new-checkout").version()).isEqualTo(1);
    }

    @Test
    void rejectsRequestIdReusedWithDifferentFacts() {
        service.put(command(false, 0, "request-0001", List.of("SE")));

        assertThatThrownBy(() -> service.put(command(true, 0, "request-0001", List.of("SE"))))
                .isInstanceOf(DomainException.class)
                .extracting(exception -> ((DomainException) exception).reason())
                .isEqualTo(DomainException.Reason.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void rejectsInvalidRolloutAndIdentifiers() {
        assertThatThrownBy(() -> service.put(new PutConfigurationCommand(
                "Bad Tenant!", "new-checkout", true, 101, List.of("SE"), 0, "request-0001")))
                .isInstanceOf(DomainException.class)
                .extracting(exception -> ((DomainException) exception).reason())
                .isEqualTo(DomainException.Reason.INVALID_ARGUMENT);
    }

    @Test
    void reportsMissingConfiguration() {
        assertThatThrownBy(() -> service.get("demo-tenant", "new-checkout"))
                .isInstanceOf(DomainException.class)
                .extracting(exception -> ((DomainException) exception).reason())
                .isEqualTo(DomainException.Reason.NOT_FOUND);
    }

    private PutConfigurationCommand command(
            boolean enabled,
            long expectedVersion,
            String requestId,
            List<String> regions) {
        return new PutConfigurationCommand(
                "demo-tenant", "new-checkout", enabled, 25, regions, expectedVersion, requestId);
    }
}
