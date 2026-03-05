package com.ron.javainfohunter.processor.service;

import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.processor.dto.RawContentMessage;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Service for aggregating results from multiple AI agents into a unified processed content message.
 *
 * <p>This service is responsible for:</p>
 * <ul>
 *   <li>Collecting results from analysis, summary, and classification agents</li>
 *   <li>Building a unified {@link ProcessedContentMessage} from raw content and agent results</li>
 *   <li>Storing the processed content for downstream consumption</li>
 * </ul>
 *
 * <p><b>Message Flow:</b></p>
 * <pre>
 * Raw Content Message → Agent Processing → ResultAggregator → Processed Content Message
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public interface ResultAggregator {

    /**
     * Aggregate results from multiple agents into a unified processed content message.
     *
     * <p>This method extracts relevant data from each agent result and combines it
     * with the original raw content to create a complete {@link ProcessedContentMessage}.</p>
     *
     * @param content the original raw content message
     * @param agentResults map of agent type to its execution result
     * @return a completion stage that completes with the aggregated processed content message
     */
    CompletionStage<ProcessedContentMessage> aggregate(
            RawContentMessage content,
            Map<AgentResult.AgentType, AgentResult> agentResults
    );

    /**
     * Store a processed content message for downstream consumption.
     *
     * <p>This method handles persistence of the processed content, including:</p>
     * <ul>
     *   <li>Database storage</li>
     *   <li>Embedding generation for similarity search</li>
     *   <li>Publishing to downstream queues</li>
     * </ul>
     *
     * @param message the processed content message to store
     * @return a completion stage that completes when storage is finished
     */
    CompletionStage<Void> store(ProcessedContentMessage message);
}
