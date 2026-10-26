package dev.portfolio.featureconfig.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.context.SmartLifecycle;

public final class GrpcServerLifecycle implements SmartLifecycle {
    private final Server server;
    private volatile boolean running;
    private Thread awaitThread;

    public GrpcServerLifecycle(
            int port,
            FeatureConfigurationGrpcService service,
            TenantSecurityInterceptor securityInterceptor) {
        server = ServerBuilder.forPort(port)
                .addService(io.grpc.ServerInterceptors.intercept(service, securityInterceptor))
                .build();
    }

    @Override
    public void start() {
        try {
            server.start();
            running = true;
            awaitThread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }, "grpc-server-await");
            awaitThread.setDaemon(false);
            awaitThread.start();
        } catch (IOException exception) {
            throw new IllegalStateException("could not start gRPC server", exception);
        }
    }

    @Override
    public void stop() {
        server.shutdown();
        try {
            server.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            server.shutdownNow();
            if (awaitThread != null) {
                try {
                    awaitThread.join(TimeUnit.SECONDS.toMillis(5));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
