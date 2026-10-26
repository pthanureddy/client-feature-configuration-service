package dev.portfolio.featureconfig.grpc;

import dev.portfolio.featureconfig.application.ApplyResult;
import dev.portfolio.featureconfig.application.FeatureConfigurationService;
import dev.portfolio.featureconfig.application.PutConfigurationCommand;
import dev.portfolio.featureconfig.domain.DomainException;
import dev.portfolio.featureconfig.v1.FeatureConfigurationServiceGrpc;
import dev.portfolio.featureconfig.v1.GetConfigurationRequest;
import dev.portfolio.featureconfig.v1.GetConfigurationResponse;
import dev.portfolio.featureconfig.v1.PutConfigurationRequest;
import dev.portfolio.featureconfig.v1.PutConfigurationResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public final class FeatureConfigurationGrpcService
        extends FeatureConfigurationServiceGrpc.FeatureConfigurationServiceImplBase {
    private final FeatureConfigurationService service;

    public FeatureConfigurationGrpcService(FeatureConfigurationService service) {
        this.service = service;
    }

    @Override
    public void putConfiguration(
            PutConfigurationRequest request,
            StreamObserver<PutConfigurationResponse> responseObserver) {
        try {
            requireTenant(request.getTenantId());
            ApplyResult result = service.put(new PutConfigurationCommand(
                    request.getTenantId(),
                    request.getFeatureKey(),
                    request.getEnabled(),
                    request.getRolloutPercentage(),
                    request.getAllowedRegionsList(),
                    request.getExpectedVersion(),
                    request.getRequestId()));
            responseObserver.onNext(PutConfigurationResponse.newBuilder()
                    .setConfiguration(toProto(result.configuration()))
                    .setReplayed(result.replayed())
                    .build());
            responseObserver.onCompleted();
        } catch (DomainException exception) {
            responseObserver.onError(status(exception).asRuntimeException());
        }
    }

    @Override
    public void getConfiguration(
            GetConfigurationRequest request,
            StreamObserver<GetConfigurationResponse> responseObserver) {
        try {
            requireTenant(request.getTenantId());
            responseObserver.onNext(GetConfigurationResponse.newBuilder()
                    .setConfiguration(toProto(service.get(request.getTenantId(), request.getFeatureKey())))
                    .build());
            responseObserver.onCompleted();
        } catch (DomainException exception) {
            responseObserver.onError(status(exception).asRuntimeException());
        }
    }

    private static void requireTenant(String requestTenant) {
        String authenticatedTenant = TenantSecurityInterceptor.AUTHENTICATED_TENANT.get();
        if (authenticatedTenant == null || !authenticatedTenant.equals(requestTenant)) {
            throw new DomainException(
                    DomainException.Reason.INVALID_ARGUMENT,
                    "request tenant must match the authenticated tenant");
        }
    }

    private static dev.portfolio.featureconfig.v1.FeatureConfiguration toProto(
            dev.portfolio.featureconfig.domain.FeatureConfiguration configuration) {
        return dev.portfolio.featureconfig.v1.FeatureConfiguration.newBuilder()
                .setTenantId(configuration.tenantId())
                .setFeatureKey(configuration.featureKey())
                .setEnabled(configuration.enabled())
                .setRolloutPercentage(configuration.rolloutPercentage())
                .addAllAllowedRegions(configuration.allowedRegions())
                .setVersion(configuration.version())
                .setUpdatedAt(configuration.updatedAt().toString())
                .build();
    }

    private static Status status(DomainException exception) {
        return switch (exception.reason()) {
            case INVALID_ARGUMENT -> Status.INVALID_ARGUMENT.withDescription(exception.getMessage());
            case NOT_FOUND -> Status.NOT_FOUND.withDescription(exception.getMessage());
            case VERSION_CONFLICT, IDEMPOTENCY_CONFLICT ->
                    Status.ABORTED.withDescription(exception.getMessage());
        };
    }
}
