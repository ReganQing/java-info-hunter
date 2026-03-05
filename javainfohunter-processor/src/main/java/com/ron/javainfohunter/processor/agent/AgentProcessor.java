package com.ron.javainfohunter.processor.agent;

import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.dto.RawContentMessage;

/**
 * Interface for AI agent processors in the content processing pipeline.
 *
 * <p>Each agent processor is responsible for a specific type of content analysis:
 * <ul>
 *   <li>{@link AgentResult.AgentType#ANALYSIS} - Deep content analysis (sentiment, topics, entities)</li>
 *   <li>{@link AgentResult.AgentType#SUMMARY} - Text summarization</li>
 *   <li>{@link AgentResult.AgentType#CLASSIFICATION} - Content categorization and tagging</li>
 * </ul>
 *
 * <p>Implementations of this interface are automatically discovered and registered
 * with the {@link com.ron.javainfohunter.processor.service.ContentRoutingService}
 * for parallel processing of incoming content.</p>
 *
 * <p><b>Thread Safety:</b></p>
 * Implementations must be thread-safe as they will be invoked concurrently
 * by the routing service using virtual threads.
 *
 * <p><b>Example Implementation:</b></p>
 * <pre>
 * &#64;Component
 * public class SummaryAgentProcessor implements AgentProcessor {
 *
 *     private final ChatClient chatClient;
 *
 *     public SummaryAgentProcessor(ChatClient chatClient) {
 *         this.chatClient = chatClient;
 *     }
 *
 *     &#64;Override
 *     public AgentResult process(RawContentMessage content) {
 *         // Generate summary using AI
 *         String summary = chatClient.prompt()
 *             .user("Summarize: " + content.getRawContent())
 *             .call()
 *             .content();
 *
 *         return AgentResult.success(
 *             AgentType.SUMMARY,
 *             content.getContentHash(),
 *             Map.of("summary", summary),
 *             System.currentTimeMillis() - startTime
 *         );
 *     }
 *
 *     &#64;Override
 *     public AgentType getAgentType() {
 *         return AgentType.SUMMARY;
 *     }
 * }
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see com.ron.javainfohunter.processor.dto.AgentResult
 * @see com.ron.javainfohunter.processor.service.ContentRoutingService
 */
public interface AgentProcessor {

    /**
     * Process raw content with this agent.
     *
     * <p>This method is invoked concurrently for each piece of content.
     * Implementations should:</p>
     * <ol>
     *   <li>Extract relevant information from the content</li>
     *   <li>Perform the agent-specific analysis using AI or other methods</li>
     *   <li>Return a structured result with success/failure status</li>
     * </ol>
     *
     * <p><b>Timing:</b> Implementations should track and report their execution time
     * for monitoring and performance analysis.</p>
     *
     * @param content the raw content message to process, containing title,
     *                raw content, metadata, and content hash
     * @return agent processing result with success status, output data,
     *         and timing information
     * @throws Exception if processing fails catastrophically (will be caught
     *                   and converted to a failed result)
     */
    AgentResult process(RawContentMessage content);

    /**
     * Get the agent type identifier for this processor.
     *
     * <p>The agent type is used for result aggregation and routing.
     * Each processor should return a unique, constant agent type.</p>
     *
     * @return the agent type (ANALYSIS, SUMMARY, or CLASSIFICATION)
     */
    AgentResult.AgentType getAgentType();

}
