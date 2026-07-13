package com.rianlucassb.liftform.infraestructure.config.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String EXCHANGE = "video-analysis.exchange";

    public static final String EXTRACTION_QUEUE = "video-analysis.queue";
    public static final String EXTRACTION_ROUTING_KEY = "video-analysis.uploaded";
    public static final String EXTRACTION_DLX = "video-analysis.dlx";
    public static final String EXTRACTION_DLQ = "video-analysis.dlq";
    public static final String EXTRACTION_DLQ_ROUTING_KEY = "video-analysis.dlq";

    // Scoring leg is a pure extractor<->scorer hand-off; the API only ever consumes its DLQ.
    public static final String SCORING_DLX = "video-analysis.scoring.dlx";
    public static final String SCORING_DLQ = "video-analysis.scoring.dlq";
    public static final String SCORING_DLQ_ROUTING_KEY = "video-analysis.scoring.dlq";

    public static final String RESULTS_QUEUE = "video-analysis.results.queue";
    public static final String RESULTS_ROUTING_KEY = "video-analysis.finished";
    public static final String RESULTS_DLX = "video-analysis.results.dlx";
    public static final String RESULTS_DLQ = "video-analysis.results.dlq";
    public static final String RESULTS_DLQ_ROUTING_KEY = "video-analysis.results.dlq";

    @Bean
    public DirectExchange videoAnalysisExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue videoAnalysisQueue() {
        return QueueBuilder.durable(EXTRACTION_QUEUE)
            .withArgument("x-dead-letter-exchange", EXTRACTION_DLX)
            .withArgument("x-dead-letter-routing-key", EXTRACTION_DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public Binding videoAnalysis(Queue videoAnalysisQueue, DirectExchange videoAnalysisExchange) {
        return BindingBuilder
            .bind(videoAnalysisQueue)
            .to(videoAnalysisExchange)
            .with(EXTRACTION_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXTRACTION_DLX);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(EXTRACTION_DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(EXTRACTION_DLQ_ROUTING_KEY);
    }

    // ── Scoring DLQ: dedicated to this leg, not shared with extraction ─────────

    @Bean
    public DirectExchange scoringDeadLetterExchange() {
        return new DirectExchange(SCORING_DLX);
    }

    @Bean
    public Queue scoringDeadLetterQueue() {
        return QueueBuilder.durable(SCORING_DLQ).build();
    }

    @Bean
    public Binding scoringDeadLetterBinding() {
        return BindingBuilder.bind(scoringDeadLetterQueue())
                .to(scoringDeadLetterExchange())
                .with(SCORING_DLQ_ROUTING_KEY);
    }

    // ── Results: scorer -> API leg, API is the consumer and owns its own DLQ ───

    @Bean
    public Queue resultsQueue() {
        return QueueBuilder.durable(RESULTS_QUEUE)
                .withArgument("x-dead-letter-exchange", RESULTS_DLX)
                .withArgument("x-dead-letter-routing-key", RESULTS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding resultsBinding(Queue resultsQueue, DirectExchange videoAnalysisExchange) {
        return BindingBuilder
                .bind(resultsQueue)
                .to(videoAnalysisExchange)
                .with(RESULTS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange resultsDeadLetterExchange() {
        return new DirectExchange(RESULTS_DLX);
    }

    @Bean
    public Queue resultsDeadLetterQueue() {
        return QueueBuilder.durable(RESULTS_DLQ).build();
    }

    @Bean
    public Binding resultsDeadLetterBinding() {
        return BindingBuilder.bind(resultsDeadLetterQueue())
                .to(resultsDeadLetterExchange())
                .with(RESULTS_DLQ_ROUTING_KEY);
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new JacksonJsonMessageConverter());
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 8000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}