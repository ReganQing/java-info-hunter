package com.ron.javainfohunter.crawler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Crawler Module
 *
 * <p>This class configures all RabbitMQ components including:</p>
 * <ul>
 *   <li>Exchanges: Direct exchanges for routing messages</li>
 *   <li>Queues: Message queues for different processing stages</li>
 *   <li>Bindings: Routing keys connecting exchanges to queues</li>
 *   <li>Message Converter: JSON serialization</li>
 * </ul>
 *
 * <p><b>Exchange Architecture:</b></p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    Exchange Architecture                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │                                                               │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │           crawler.direct (Direct Exchange)            │  │
 * │  ├──────────────────────────────────────────────────────┤  │
 * │  │  Routing Key           │  Bound Queue                 │  │
 * │  ├────────────────────────┼─────────────────────────────┤  │
 * │  │  raw.content           │  crawler.raw.content.queue   │  │
 * │  │  content.encoded       │  crawler.content.encoded.queue│ │
 * │  │  crawl.result          │  crawler.crawl.result.queue  │  │
 * │  │  crawl.error           │  crawler.crawl.error.queue   │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │                                                               │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │       dead.letter.direct (Dead Letter Exchange)       │  │
 * │  ├──────────────────────────────────────────────────────┤  │
 * │  │  Routing Key           │  Bound Queue                 │  │
 * │  ├────────────────────────┼─────────────────────────────┤  │
 * │  │  raw.content.dlq       │  crawler.raw.content.dlq     │  │
 * │  │  content.encoded.dlq   │  crawler.content.encoded.dlq │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │                                                               │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see org.springframework.amqp.core.Exchange
 * @see org.springframework.amqp.core.Queue
 * @see org.springframework.amqp.core.Binding
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ========================================================================
    // Exchange Names
    // ========================================================================

    public static final String CRAWLER_EXCHANGE = "crawler.direct";
    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.direct";

    // ========================================================================
    // Queue Names
    // ========================================================================

    /**
     * Queue for raw RSS content messages.
     * NOTE: Uses processor's queue name for direct message delivery.
     * Processor module listens to this queue.
     */
    public static final String RAW_CONTENT_QUEUE = "processor.raw.content.queue";
    public static final String CONTENT_ENCODED_QUEUE = "crawler.content.encoded.queue";
    public static final String CRAWL_RESULT_QUEUE = "crawler.crawl.result.queue";
    public static final String CRAWL_ERROR_QUEUE = "crawler.crawl.error.queue";

    // Dead Letter Queue Names
    public static final String RAW_CONTENT_DLQ = "crawler.raw.content.dlq";
    public static final String CONTENT_ENCODED_DLQ = "crawler.content.encoded.dlq";

    // ========================================================================
    // Routing Keys
    // ========================================================================

    public static final String RAW_CONTENT_ROUTING_KEY = "raw.content";
    public static final String CONTENT_ENCODED_ROUTING_KEY = "content.encoded";
    public static final String CRAWL_RESULT_ROUTING_KEY = "crawl.result";
    public static final String CRAWL_ERROR_ROUTING_KEY = "crawl.error";

    // Dead Letter Routing Keys
    public static final String RAW_CONTENT_DLQ_ROUTING_KEY = "raw.content.dlq";
    public static final String CONTENT_ENCODED_DLQ_ROUTING_KEY = "content.encoded.dlq";

    // ========================================================================
    // Exchanges
    // ========================================================================

    /**
     * Main crawler exchange for routing messages to processing queues.
     *
     * @return DirectExchange for crawler messages
     */
    @Bean
    public DirectExchange crawlerExchange() {
        return new DirectExchange(
            CRAWLER_EXCHANGE,
            true,  // durable
            false  // auto-delete
        );
    }

    /**
     * Dead letter exchange for handling failed messages.
     *
     * @return DirectExchange for dead letter messages
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(
            DEAD_LETTER_EXCHANGE,
            true,  // durable
            false  // auto-delete
        );
    }

    // ========================================================================
    // Queues
    // ========================================================================

    /**
     * Queue for raw RSS content messages.
     * Messages that fail processing will be routed to the dead letter queue.
     *
     * @return Queue for raw content
     */
    @Bean
    public Queue rawContentQueue() {
        return QueueBuilder
            .durable(RAW_CONTENT_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", RAW_CONTENT_DLQ_ROUTING_KEY)
            .withArgument("x-max-length", 100000)  // Max 100k messages
            .withArgument("x-message-ttl", 86400000)  // 24 hours TTL
            .build();
    }

    /**
     * Queue for encoded content messages (HTML cleaned, structured).
     *
     * @return Queue for encoded content
     */
    @Bean
    public Queue contentEncodedQueue() {
        return QueueBuilder
            .durable(CONTENT_ENCODED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", CONTENT_ENCODED_DLQ_ROUTING_KEY)
            .withArgument("x-max-length", 100000)
            .withArgument("x-message-ttl", 86400000)
            .build();
    }

    /**
     * Queue for crawl result notifications.
     *
     * @return Queue for crawl results
     */
    @Bean
    public Queue crawlResultQueue() {
        return QueueBuilder
            .durable(CRAWL_RESULT_QUEUE)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 43200000)  // 12 hours TTL
            .build();
    }

    /**
     * Queue for crawl error notifications.
     *
     * @return Queue for crawl errors
     */
    @Bean
    public Queue crawlErrorQueue() {
        return QueueBuilder
            .durable(CRAWL_ERROR_QUEUE)
            .withArgument("x-max-length", 10000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    // ========================================================================
    // Dead Letter Queues
    // ========================================================================

    /**
     * Dead letter queue for raw content processing failures.
     *
     * @return Dead letter queue for raw content
     */
    @Bean
    public Queue rawContentDLQ() {
        return QueueBuilder
            .durable(RAW_CONTENT_DLQ)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    /**
     * Dead letter queue for encoded content processing failures.
     *
     * @return Dead letter queue for encoded content
     */
    @Bean
    public Queue contentEncodedDLQ() {
        return QueueBuilder
            .durable(CONTENT_ENCODED_DLQ)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    // ========================================================================
    // Bindings
    // ========================================================================

    /**
     * Bind raw content queue to crawler exchange.
     *
     * @param rawContentQueue the raw content queue
     * @param crawlerExchange the crawler exchange
     * @return Binding
     */
    @Bean
    public Binding rawContentBinding(
        @Qualifier("rawContentQueue") Queue rawContentQueue,
        @Qualifier("crawlerExchange") DirectExchange crawlerExchange
    ) {
        return BindingBuilder
            .bind(rawContentQueue)
            .to(crawlerExchange)
            .with(RAW_CONTENT_ROUTING_KEY);
    }

    /**
     * Bind encoded content queue to crawler exchange.
     *
     * @param contentEncodedQueue the encoded content queue
     * @param crawlerExchange the crawler exchange
     * @return Binding
     */
    @Bean
    public Binding contentEncodedBinding(
        @Qualifier("contentEncodedQueue") Queue contentEncodedQueue,
        @Qualifier("crawlerExchange") DirectExchange crawlerExchange
    ) {
        return BindingBuilder
            .bind(contentEncodedQueue)
            .to(crawlerExchange)
            .with(CONTENT_ENCODED_ROUTING_KEY);
    }

    /**
     * Bind crawl result queue to crawler exchange.
     *
     * @param crawlResultQueue the crawl result queue
     * @param crawlerExchange the crawler exchange
     * @return Binding
     */
    @Bean
    public Binding crawlResultBinding(
        @Qualifier("crawlResultQueue") Queue crawlResultQueue,
        @Qualifier("crawlerExchange") DirectExchange crawlerExchange
    ) {
        return BindingBuilder
            .bind(crawlResultQueue)
            .to(crawlerExchange)
            .with(CRAWL_RESULT_ROUTING_KEY);
    }

    /**
     * Bind crawl error queue to crawler exchange.
     *
     * @param crawlErrorQueue the crawl error queue
     * @param crawlerExchange the crawler exchange
     * @return Binding
     */
    @Bean
    public Binding crawlErrorBinding(
        @Qualifier("crawlErrorQueue") Queue crawlErrorQueue,
        @Qualifier("crawlerExchange") DirectExchange crawlerExchange
    ) {
        return BindingBuilder
            .bind(crawlErrorQueue)
            .to(crawlerExchange)
            .with(CRAWL_ERROR_ROUTING_KEY);
    }

    // ========================================================================
    // Dead Letter Bindings
    // ========================================================================

    /**
     * Bind raw content DLQ to dead letter exchange.
     *
     * @param rawContentDLQ the raw content dead letter queue
     * @param deadLetterExchange the dead letter exchange
     * @return Binding
     */
    @Bean
    public Binding rawContentDLQBinding(
        @Qualifier("rawContentDLQ") Queue rawContentDLQ,
        @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
            .bind(rawContentDLQ)
            .to(deadLetterExchange)
            .with(RAW_CONTENT_DLQ_ROUTING_KEY);
    }

    /**
     * Bind encoded content DLQ to dead letter exchange.
     *
     * @param contentEncodedDLQ the encoded content dead letter queue
     * @param deadLetterExchange the dead letter exchange
     * @return Binding
     */
    @Bean
    public Binding contentEncodedDLQBinding(
        @Qualifier("contentEncodedDLQ") Queue contentEncodedDLQ,
        @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
            .bind(contentEncodedDLQ)
            .to(deadLetterExchange)
            .with(CONTENT_ENCODED_DLQ_ROUTING_KEY);
    }

    // ========================================================================
    // Message Converter
    // ========================================================================

    /**
     * JSON message converter for RabbitMQ.
     * Uses Jackson for serialization/deserialization.
     *
     * @return MessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // Configure converter to not use type header for better compatibility
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(false);
        return converter;
    }

    /**
     * Configure RabbitTemplate with JSON converter and publisher confirms.
     *
     * @param connectionFactory the connection factory
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        // Note: Publisher confirm callback is configured in ContentPublisher
        // to properly complete the CompletableFuture for each message

        // Enable publisher returns for unroutable messages
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Message returned from RabbitMQ - Exchange: {}, RoutingKey: {}, ReplyText: {}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyText()
            );
        });

        return rabbitTemplate;
    }
}
