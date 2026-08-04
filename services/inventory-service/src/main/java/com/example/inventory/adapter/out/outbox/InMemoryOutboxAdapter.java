package com.example.inventory.adapter.out.outbox;

import com.example.inventory.application.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class InMemoryOutboxAdapter implements OutboxPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryOutboxAdapter.class);
    private final List<Object> publishedEvents = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(Object domainEvent) {
        log.info("Publishing domain event to outbox: {}", domainEvent);
        publishedEvents.add(domainEvent);
    }

    public List<Object> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
