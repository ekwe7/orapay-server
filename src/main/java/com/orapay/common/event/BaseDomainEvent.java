package com.orapay.common.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

public abstract class BaseDomainEvent extends ApplicationEvent {

    private final Instant eventOccurredAtTimestamp;

    public BaseDomainEvent(Object sourceComponentInstance) {
        super(sourceComponentInstance);
        this.eventOccurredAtTimestamp = Instant.now();
    }

    public Instant getEventOccurredAtTimestamp() {
        return eventOccurredAtTimestamp;
    }
}
