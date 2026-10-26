package dev.portfolio.featureconfig.domain;

public final class DomainException extends RuntimeException {
    private final Reason reason;

    public DomainException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        INVALID_ARGUMENT,
        NOT_FOUND,
        VERSION_CONFLICT,
        IDEMPOTENCY_CONFLICT
    }
}
