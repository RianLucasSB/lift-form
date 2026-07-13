package com.rianlucassb.liftform.infraestructure.adapter.messaging;

import com.rianlucassb.liftform.core.domain.event.DomainEvent;
import com.rianlucassb.liftform.core.gateway.event.EventPublisher;
import com.rianlucassb.liftform.infraestructure.config.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.EXTRACTION_ROUTING_KEY,
            event,
            message -> {
                message.getMessageProperties().setMessageId(event.getEventId().toString());
                message.getMessageProperties().setTimestamp(Date.from(event.getOccurredAt()));
                message.getMessageProperties().setContentType("application/json");
                return message;
            }
        );
    }
}
