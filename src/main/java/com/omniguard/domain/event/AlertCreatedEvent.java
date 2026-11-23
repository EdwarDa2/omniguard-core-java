package com.omniguard.domain.event;

import com.omniguard.domain.model.Alert;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
public class AlertCreatedEvent extends ApplicationEvent {

    private final Alert alert;
    private final Instant timestamp;

    public AlertCreatedEvent(Object source, Alert alert) {
        super(source);
        this.alert = alert;
        this.timestamp = Instant.now();
    }
}