package com.orapay.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisherInstance;

    public void publishEvent(BaseDomainEvent domainEventInstance) {
        applicationEventPublisherInstance.publishEvent(domainEventInstance);
    }
}
