package com.rianlucassb.liftform.infraestructure.config.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String EXCHANGE = "video-analysis.exchange";
    public static final String QUEUE    = "video-analysis.queue";
    public static final String ROUTING  = "video-analysis.uploaded";

    public static final String DEAD_LETTER_QUEUE = "video-analysis.dlq";
    public static final String DEAD_LETTER_EXCHANGE = "video-analysis.dlx";
    public static final String DEAD_LETTER_ROUTING = "video-analysis.dlq";

    @Bean
    public DirectExchange videoAnalysisExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue videoAnalysisQueue() {
        return QueueBuilder.durable(QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING)
            .build();
    }

    @Bean
    public Binding videoAnalysis(Queue videoAnalysisQueue, DirectExchange videoAnalysisExchange) {
        return BindingBuilder
            .bind(videoAnalysisQueue)
            .to(videoAnalysisExchange)
            .with(ROUTING);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new JacksonJsonMessageConverter());

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) log.error("Message NOT confirmed: {}", cause);
        });

        return template;
    }
}