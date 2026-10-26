package dev.portfolio.featureconfig.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public final class TenantSecurityInterceptor implements ServerInterceptor {
    public static final Context.Key<String> AUTHENTICATED_TENANT = Context.key("authenticated-tenant");
    public static final Metadata.Key<String> TENANT_HEADER =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final TenantCredentialProvider credentials;

    public TenantSecurityInterceptor(TenantCredentialProvider credentials) {
        this.credentials = credentials;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String tenantId = headers.get(TENANT_HEADER);
        String apiKey = headers.get(API_KEY_HEADER);
        if (!credentials.matches(tenantId, apiKey)) {
            call.close(Status.UNAUTHENTICATED.withDescription("valid tenant credentials are required"), new Metadata());
            return new ServerCall.Listener<>() { };
        }
        return Contexts.interceptCall(
                Context.current().withValue(AUTHENTICATED_TENANT, tenantId), call, headers, next);
    }
}
