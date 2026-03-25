package com.ron.javainfohunter.processor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * RabbitMQ Listener Configuration
 *
 * <p>This configuration class provides additional logging for RabbitMQ listener setup.
 * The @EnableRabbit annotation is on the main ProcessorApplication class.</p>
 */
@Slf4j
@Configuration
public class RabbitListenerConfiguration {

    @PostConstruct
    public void init() {
        log.info("=================================================");
        log.info("RABBITMQ LISTENER CONFIGURATION LOADED");
        log.info("@EnableRabbit is on ProcessorApplication class");
        log.info("=================================================");
    }

    // This configuration class ensures that @RabbitListener annotations are processed
}
