package com.rianlucassb.liftform.infraestructure.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.core.domain.event.DomainEvent;
import com.rianlucassb.liftform.core.domain.exception.EventPublishingException;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;
import com.rianlucassb.liftform.infraestructure.config.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                event.getEventType(),
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(event.getEventId().toString());
                    message.getMessageProperties().setTimestamp(Date.from(event.getOccurredAt()));
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                }
            );
        } catch (JsonProcessingException e) {
            throw new EventPublishingException("Failed to serialize event: " + event.getEventType(), e);
        }
    }
}