package com.orapay.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisherInstance;

    public void publishEvent(Object domainEventInstance) {
        applicationEventPublisherInstance.publishEvent(domainEventInstance);
    }
}
