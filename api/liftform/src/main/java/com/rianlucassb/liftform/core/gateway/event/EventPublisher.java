package com.rianlucassb.liftform.core.gateway.event;

import com.rianlucassb.liftform.core.domain.event.DomainEvent;

public interface EventPublisher {
    void publish(DomainEvent event);
}