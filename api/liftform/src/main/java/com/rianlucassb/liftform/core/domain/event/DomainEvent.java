package com.rianlucassb.liftform.core.domain.event;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();

    public UUID getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public abstract String getEventType();
}