package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;

/**
 * Functional interface for message publishing callback.
 *
 * <p>This interface is used to publish messages after transaction commit,
 * ensuring that messages are only sent for successfully committed data.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@FunctionalInterface
interface MessagePublisher {
    /**
     * Publish processed message to downstream queue.
     *
     * @param news the saved news entity
     * @param message the processed content message
     */
    void publish(News news, ProcessedContentMessage message);
}
