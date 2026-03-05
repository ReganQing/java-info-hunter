package com.ron.javainfohunter.processor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Consumer Configuration for Processor Module
 *
 * <p>This class configures all RabbitMQ consumer components including:</p>
 * <ul>
 *   <li>Message Converter: JSON serialization for message consumption</li>
 *   <li>Exchanges: Direct exchanges for receiving and routing messages</li>
 *   <li>Queues: Message queues for different processing stages</li>
 *   <li>Bindings: Routing keys connecting exchanges to queues</li>
 * </ul>
 *
 * <p><b>Exchange Architecture:</b></p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    Exchange Architecture                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │                                                               │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │      crawler.direct (Consume from Crawler Module)     │  │
 * │  ├──────────────────────────────────────────────────────┤  │
 * │  │  Routing Key           │  Bound Queue                 │  │
 * │  ├────────────────────────┼─────────────────────────────┤  │
 * │  │  raw.content           │  processor.raw.content.queue │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │                                                               │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │       processor.direct (Internal Processing)          │  │
 * │  ├──────────────────────────────────────────────────────┤  │
 * │  │  Routing Key           │  Bound Queue                 │  │
 * │  ├────────────────────────┼─────────────────────────────┤  │
 * │  │  analysis              │  processor.analysis.queue    │  │
 * │  │  summary               │  processor.summary.queue     │  │
 * │  │  classification        │  processor.classification... │  │
 * │  │  aggregated            │  processor.aggregated.queue  │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │                                                               │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │       dead.letter.direct (Dead Letter Exchange)       │  │
 * │  ├──────────────────────────────────────────────────────┤  │
 * │  │  Routing Key           │  Bound Queue                 │  │
 * │  ├────────────────────────┼─────────────────────────────┤  │
 * │  │  raw.content.dlq       │  processor.raw.content.dlq   │  │
 * │  │  analysis.dlq          │  processor.analysis.dlq      │  │
 * │  │  summary.dlq           │  processor.summary.dlq       │  │
 * │  │  classification.dlq    │  processor.classification.dlq│  │
 * │  │  aggregated.dlq        │  processor.aggregated.dlq    │  │
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
public class RabbitMQConsumerConfig {

    // ========================================================================
    // Exchange Names
    // ========================================================================

    /**
     * Crawler module exchange - consume from this exchange
     */
    public static final String CRAWLER_EXCHANGE = "crawler.direct";

    /**
     * Processor module internal exchange
     */
    public static final String PROCESSOR_EXCHANGE = "processor.direct";

    /**
     * Shared dead letter exchange for failed messages
     */
    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.direct";

    // ========================================================================
    // Queue Names
    // ========================================================================

    /**
     * Queue for consuming raw content from crawler module
     */
    public static final String RAW_CONTENT_QUEUE = "processor.raw.content.queue";

    /**
     * Queue for analysis agent processing
     */
    public static final String ANALYSIS_QUEUE = "processor.analysis.queue";

    /**
     * Queue for summary agent processing
     */
    public static final String SUMMARY_QUEUE = "processor.summary.queue";

    /**
     * Queue for classification agent processing
     */
    public static final String CLASSIFICATION_QUEUE = "processor.classification.queue";

    /**
     * Queue for aggregated processing results
     */
    public static final String AGGREGATED_QUEUE = "processor.aggregated.queue";

    // Dead Letter Queue Names
    public static final String RAW_CONTENT_DLQ = "processor.raw.content.dlq";
    public static final String ANALYSIS_DLQ = "processor.analysis.dlq";
    public static final String SUMMARY_DLQ = "processor.summary.dlq";
    public static final String CLASSIFICATION_DLQ = "processor.classification.dlq";
    public static final String AGGREGATED_DLQ = "processor.aggregated.dlq";

    // ========================================================================
    // Routing Keys
    // ========================================================================

    /**
     * Routing key for raw content from crawler (crawler module publishes this)
     */
    public static final String RAW_CONTENT_ROUTING_KEY = "raw.content";

    /**
     * Routing key for analysis messages
     */
    public static final String ANALYSIS_ROUTING_KEY = "analysis";

    /**
     * Routing key for summary messages
     */
    public static final String SUMMARY_ROUTING_KEY = "summary";

    /**
     * Routing key for classification messages
     */
    public static final String CLASSIFICATION_ROUTING_KEY = "classification";

    /**
     * Routing key for aggregated results
     */
    public static final String AGGREGATED_ROUTING_KEY = "aggregated";

    // Dead Letter Routing Keys
    public static final String RAW_CONTENT_DLQ_ROUTING_KEY = "raw.content.dlq";
    public static final String ANALYSIS_DLQ_ROUTING_KEY = "analysis.dlq";
    public static final String SUMMARY_DLQ_ROUTING_KEY = "summary.dlq";
    public static final String CLASSIFICATION_DLQ_ROUTING_KEY = "classification.dlq";
    public static final String AGGREGATED_DLQ_ROUTING_KEY = "aggregated.dlq";

    // ========================================================================
    // Exchanges
    // ========================================================================

    /**
     * Crawler exchange - consume raw content from crawler module.
     * This exchange is declared by the crawler module.
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
     * Processor internal exchange for routing messages to agent processing queues.
     *
     * @return DirectExchange for processor internal messages
     */
    @Bean
    public DirectExchange processorExchange() {
        return new DirectExchange(
            PROCESSOR_EXCHANGE,
            true,  // durable
            false  // auto-delete
        );
    }

    /**
     * Dead letter exchange for handling failed messages.
     * Shared with crawler module.
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
     * Queue for consuming raw content from crawler module.
     * Binds to crawler.direct exchange with raw.content routing key.
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
     * Queue for analysis agent processing.
     * Messages are sent here for content analysis using AI agents.
     *
     * @return Queue for analysis processing
     */
    @Bean
    public Queue analysisQueue() {
        return QueueBuilder
            .durable(ANALYSIS_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ANALYSIS_DLQ_ROUTING_KEY)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 86400000)  // 24 hours TTL
            .build();
    }

    /**
     * Queue for summary agent processing.
     * Messages are sent here for text summarization using AI agents.
     *
     * @return Queue for summary processing
     */
    @Bean
    public Queue summaryQueue() {
        return QueueBuilder
            .durable(SUMMARY_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", SUMMARY_DLQ_ROUTING_KEY)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 86400000)  // 24 hours TTL
            .build();
    }

    /**
     * Queue for classification agent processing.
     * Messages are sent here for content classification and tagging using AI agents.
     *
     * @return Queue for classification processing
     */
    @Bean
    public Queue classificationQueue() {
        return QueueBuilder
            .durable(CLASSIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", CLASSIFICATION_DLQ_ROUTING_KEY)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 86400000)  // 24 hours TTL
            .build();
    }

    /**
     * Queue for aggregated processing results.
     * Messages are sent here after all agent processing is complete.
     *
     * @return Queue for aggregated results
     */
    @Bean
    public Queue aggregatedQueue() {
        return QueueBuilder
            .durable(AGGREGATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", AGGREGATED_DLQ_ROUTING_KEY)
            .withArgument("x-max-length", 100000)
            .withArgument("x-message-ttl", 43200000)  // 12 hours TTL
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
     * Dead letter queue for analysis processing failures.
     *
     * @return Dead letter queue for analysis
     */
    @Bean
    public Queue analysisDLQ() {
        return QueueBuilder
            .durable(ANALYSIS_DLQ)
            .withArgument("x-max-length", 25000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    /**
     * Dead letter queue for summary processing failures.
     *
     * @return Dead letter queue for summary
     */
    @Bean
    public Queue summaryDLQ() {
        return QueueBuilder
            .durable(SUMMARY_DLQ)
            .withArgument("x-max-length", 25000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    /**
     * Dead letter queue for classification processing failures.
     *
     * @return Dead letter queue for classification
     */
    @Bean
    public Queue classificationDLQ() {
        return QueueBuilder
            .durable(CLASSIFICATION_DLQ)
            .withArgument("x-max-length", 25000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    /**
     * Dead letter queue for aggregated results processing failures.
     *
     * @return Dead letter queue for aggregated results
     */
    @Bean
    public Queue aggregatedDLQ() {
        return QueueBuilder
            .durable(AGGREGATED_DLQ)
            .withArgument("x-max-length", 50000)
            .withArgument("x-message-ttl", 604800000)  // 7 days TTL
            .build();
    }

    // ========================================================================
    // Bindings
    // ========================================================================

    /**
     * Bind raw content queue to crawler exchange.
     * This binding allows the processor to consume messages published by the crawler module.
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
     * Bind analysis queue to processor exchange.
     *
     * @param analysisQueue the analysis queue
     * @param processorExchange the processor exchange
     * @return Binding
     */
    @Bean
    public Binding analysisBinding(
        @Qualifier("analysisQueue") Queue analysisQueue,
        @Qualifier("processorExchange") DirectExchange processorExchange
    ) {
        return BindingBuilder
            .bind(analysisQueue)
            .to(processorExchange)
            .with(ANALYSIS_ROUTING_KEY);
    }

    /**
     * Bind summary queue to processor exchange.
     *
     * @param summaryQueue the summary queue
     * @param processorExchange the processor exchange
     * @return Binding
     */
    @Bean
    public Binding summaryBinding(
        @Qualifier("summaryQueue") Queue summaryQueue,
        @Qualifier("processorExchange") DirectExchange processorExchange
    ) {
        return BindingBuilder
            .bind(summaryQueue)
            .to(processorExchange)
            .with(SUMMARY_ROUTING_KEY);
    }

    /**
     * Bind classification queue to processor exchange.
     *
     * @param classificationQueue the classification queue
     * @param processorExchange the processor exchange
     * @return Binding
     */
    @Bean
    public Binding classificationBinding(
        @Qualifier("classificationQueue") Queue classificationQueue,
        @Qualifier("processorExchange") DirectExchange processorExchange
    ) {
        return BindingBuilder
            .bind(classificationQueue)
            .to(processorExchange)
            .with(CLASSIFICATION_ROUTING_KEY);
    }

    /**
     * Bind aggregated queue to processor exchange.
     *
     * @param aggregatedQueue the aggregated queue
     * @param processorExchange the processor exchange
     * @return Binding
     */
    @Bean
    public Binding aggregatedBinding(
        @Qualifier("aggregatedQueue") Queue aggregatedQueue,
        @Qualifier("processorExchange") DirectExchange processorExchange
    ) {
        return BindingBuilder
            .bind(aggregatedQueue)
            .to(processorExchange)
            .with(AGGREGATED_ROUTING_KEY);
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
     * Bind analysis DLQ to dead letter exchange.
     *
     * @param analysisDLQ the analysis dead letter queue
     * @param deadLetterExchange the dead letter exchange
     * @return Binding
     */
    @Bean
    public Binding analysisDLQBinding(
        @Qualifier("analysisDLQ") Queue analysisDLQ,
        @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
            .bind(analysisDLQ)
            .to(deadLetterExchange)
            .with(ANALYSIS_DLQ_ROUTING_KEY);
    }

    /**
     * Bind summary DLQ to dead letter exchange.
     *
     * @param summaryDLQ the summary dead letter queue
     * @param deadLetterExchange the dead letter exchange
     * @return Binding
     */
    @Bean
    public Binding summaryDLQBinding(
        @Qualifier("summaryDLQ") Queue summaryDLQ,
        @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
            .bind(summaryDLQ)
            .to(deadLetterExchange)
            .with(SUMMARY_DLQ_ROUTING_KEY);
    }

    /**
     * Bind classification DLQ to dead letter exchange.
     *
     * @param classificationDLQ the classification dead letter queue
     * @param deadLetterExchange the dead letter exchange
     * @return Binding
     */
    @Bean
    public Binding classificationDLQBinding(
        @Qualifier("classificationDLQ") Queue classificationDLQ,
        @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
            .bind(classificationDLQ)
            .to(deadLetterExchange)
            .with(CLASSIFICATION_DLQ_ROUTING_KEY);
    }

    /**
     * Bind aggregated DLQ to dead letter exchange.
     *
     * @param aggregatedDLQ the aggregated dead letter queue
     * @param deadLetterExchange the dead letter exchange
     * @return Binding
     */
    @Bean
    public Binding aggregatedDLQBinding(
        @Qualifier("aggregatedDLQ") Queue aggregatedDLQ,
        @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
            .bind(aggregatedDLQ)
            .to(deadLetterExchange)
            .with(AGGREGATED_DLQ_ROUTING_KEY);
    }

    // ========================================================================
    // Message Converter
    // ========================================================================

    /**
     * JSON message converter for RabbitMQ.
     * Uses Jackson for serialization/deserialization of message payloads.
     * Compatible with the crawler module's message format.
     *
     * @return MessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
