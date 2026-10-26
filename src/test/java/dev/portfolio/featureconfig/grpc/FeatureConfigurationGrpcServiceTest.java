package dev.portfolio.featureconfig.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.portfolio.featureconfig.application.FeatureConfigurationService;
import dev.portfolio.featureconfig.application.InMemoryFeatureConfigurationStore;
import dev.portfolio.featureconfig.v1.FeatureConfigurationServiceGrpc;
import dev.portfolio.featureconfig.v1.GetConfigurationRequest;
import dev.portfolio.featureconfig.v1.PutConfigurationRequest;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureConfigurationGrpcServiceTest {
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String name = InProcessServerBuilder.generateName();
        FeatureConfigurationService applicationService = new FeatureConfigurationService(
                new InMemoryFeatureConfigurationStore(),
                Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC));
        TenantSecurityInterceptor security = new TenantSecurityInterceptor(
                new TenantCredentialProvider("demo-tenant=test-secret,other-tenant=other-secret"));
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(ServerInterceptors.intercept(
                        new FeatureConfigurationGrpcService(applicationService), security))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void authenticatedTenantCanCreateAndReadConfiguration() {
        var stub = authenticatedStub("demo-tenant", "test-secret");
        var created = stub.putConfiguration(PutConfigurationRequest.newBuilder()
                .setTenantId("demo-tenant")
                .setFeatureKey("new-checkout")
                .setEnabled(true)
                .setRolloutPercentage(20)
                .addAllowedRegions("SE")
                .setExpectedVersion(0)
                .setRequestId("request-0001")
                .build());

        var found = stub.getConfiguration(GetConfigurationRequest.newBuilder()
                .setTenantId("demo-tenant")
                .setFeatureKey("new-checkout")
                .build());

        assertThat(created.getConfiguration().getVersion()).isEqualTo(1);
        assertThat(found.getConfiguration().getEnabled()).isTrue();
    }

    @Test
    void missingCredentialsAreRejected() {
        var stub = FeatureConfigurationServiceGrpc.newBlockingStub(channel);

        assertThatThrownBy(() -> stub.getConfiguration(GetConfigurationRequest.newBuilder()
                .setTenantId("demo-tenant")
                .setFeatureKey("new-checkout")
                .build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        exception -> assertThat(exception.getStatus().getCode())
                                .isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    @Test
    void payloadTenantCannotCrossAuthenticatedBoundary() {
        var stub = authenticatedStub("demo-tenant", "test-secret");

        assertThatThrownBy(() -> stub.getConfiguration(GetConfigurationRequest.newBuilder()
                .setTenantId("other-tenant")
                .setFeatureKey("new-checkout")
                .build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        exception -> assertThat(exception.getStatus().getCode())
                                .isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void invalidCommandMapsToInvalidArgument() {
        var stub = authenticatedStub("demo-tenant", "test-secret");

        assertThatThrownBy(() -> stub.putConfiguration(PutConfigurationRequest.newBuilder()
                .setTenantId("demo-tenant")
                .setFeatureKey("new-checkout")
                .setRolloutPercentage(101)
                .setRequestId("request-0001")
                .build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        exception -> assertThat(exception.getStatus().getCode())
                                .isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    private FeatureConfigurationServiceGrpc.FeatureConfigurationServiceBlockingStub authenticatedStub(
            String tenantId, String apiKey) {
        Metadata headers = new Metadata();
        headers.put(TenantSecurityInterceptor.TENANT_HEADER, tenantId);
        headers.put(TenantSecurityInterceptor.API_KEY_HEADER, apiKey);
        return FeatureConfigurationServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }
}
