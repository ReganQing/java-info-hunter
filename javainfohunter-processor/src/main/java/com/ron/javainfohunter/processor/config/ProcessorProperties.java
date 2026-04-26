package com.ron.javainfohunter.processor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Processor module.
 *
 * <p>This class encapsulates all configurable parameters for content processing,
 * including agent-specific settings (analysis, summary, classification) and
 * embedding generation configuration.</p>
 *
 * <p><b>Configuration Structure:</b></p>
 * <pre>
 * javainfohunter:
 *   processor:
 *     enabled: true
 *     agents:
 *       analysis:
 *         enabled: true
 *         timeout: 30000
 *       summary:
 *         enabled: true
 *         timeout: 30000
 *         max-summary-length: 500
 *       classification:
 *         enabled: true
 *         timeout: 30000
 *     embedding:
 *       enabled: true
 *       model: "text-embedding-v3"
 *       dimensions: 1024
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "javainfohunter.processor")
public class ProcessorProperties {

    /**
     * Enable/disable the processor module.
     * When disabled, incoming content messages will be acknowledged without processing.
     */
    private boolean enabled = true;

    /**
     * Agent-specific configuration settings.
     */
    @Valid
    private AgentConfig agents = new AgentConfig();

    /**
     * Embedding generation configuration.
     */
    @Valid
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * RabbitMQ listener configuration.
     */
    @Valid
    private QueueConfig queue = new QueueConfig();

    /**
     * Processing concurrency configuration.
     */
    @Valid
    private ProcessingConfig processing = new ProcessingConfig();

    /**
     * Agent configuration properties.
     *
     * <p>Contains nested configuration for each agent type, allowing
     * individual control over enabled status and timeout settings.</p>
     */
    @Data
    public static class AgentConfig {
        /**
         * Analysis agent configuration.
         * Performs sentiment analysis, topic extraction, and entity recognition.
         */
        private AnalysisConfig analysis = new AnalysisConfig();

        /**
         * Summary agent configuration.
         * Generates concise summaries and extracts key points.
         */
        private SummaryConfig summary = new SummaryConfig();

        /**
         * Classification agent configuration.
         * Categorizes content and assigns relevant tags.
         */
        private ClassificationConfig classification = new ClassificationConfig();
    }

    /**
     * Analysis agent configuration properties.
     */
    @Data
    public static class AnalysisConfig {
        /**
         * Enable/disable the analysis agent.
         */
        private boolean enabled = true;

        /**
         * Maximum processing time in milliseconds.
         * Default: 120000ms (2 minutes)
         */
        @Min(5_000)
        @Max(600_000)
        private long timeout = 120000;

        /**
         * Maximum estimated token count for content sent to the analysis agent.
         * Content exceeding this limit will be truncated at a sentence boundary.
         * Default: 4000 tokens (conservative estimate for qwen-max)
         */
        @Min(256)
        @Max(20_000)
        private int maxContentTokens = 4000;
    }

    /**
     * Summary agent configuration properties.
     */
    @Data
    public static class SummaryConfig {
        /**
         * Enable/disable the summary agent.
         */
        private boolean enabled = true;

        /**
         * Maximum processing time in milliseconds.
         * Default: 120000ms (2 minutes)
         */
        @Min(5_000)
        @Max(600_000)
        private long timeout = 120000;

        /**
         * Maximum length of generated summary in characters.
         * Default: 500 characters
         */
        @Min(100)
        @Max(2_000)
        private int maxSummaryLength = 500;

        /**
         * Maximum estimated token count for content before chunking is triggered.
         * Content below this limit is processed in a single API call.
         * Content above this limit is split into chunks for map-reduce processing.
         * Default: 8000 tokens
         */
        @Min(512)
        @Max(40_000)
        private int maxContentTokens = 8000;

        /**
         * Token limit per chunk when using map-reduce processing.
         * Default: 6000 tokens (leaves room for system prompt + user prompt)
         */
        @Min(256)
        @Max(20_000)
        private int chunkTokenLimit = 6000;

        @AssertTrue(message = "chunkTokenLimit must not exceed maxContentTokens")
        public boolean isChunkTokenLimitValid() {
            return chunkTokenLimit <= maxContentTokens;
        }
    }

    /**
     * Classification agent configuration properties.
     */
    @Data
    public static class ClassificationConfig {
        /**
         * Enable/disable the classification agent.
         */
        private boolean enabled = true;

        /**
         * Maximum processing time in milliseconds.
         * Default: 120000ms (2 minutes)
         */
        @Min(5_000)
        @Max(600_000)
        private long timeout = 120000;

        /**
         * Maximum estimated token count for content sent to the classification agent.
         * Content exceeding this limit will be truncated at a sentence boundary.
         * Default: 2000 tokens
         */
        @Min(128)
        @Max(20_000)
        private int maxContentTokens = 2000;
    }

    /**
     * Embedding generation configuration properties.
     */
    @Data
    public static class EmbeddingConfig {
        /**
         * Enable/disable embedding generation.
         */
        private boolean enabled = true;

        /**
         * Embedding model identifier.
         * Default: "text-embedding-v3" (Alibaba DashScope)
         */
        @NotBlank
        private String model = "text-embedding-v3";

        /**
         * Dimensionality of the embedding vectors.
         * Default: 1024 dimensions
         */
        @Min(128)
        @Max(4096)
        private int dimensions = 1024;
    }

    @Data
    public static class QueueConfig {
        /**
         * Input queue name for raw crawler content.
         */
        @NotBlank
        private String inputQueue = "processor.raw.content.queue";

        /**
         * Dead letter queue name for failed raw content messages.
         */
        @NotBlank
        private String deadLetterQueue = "processor.dead.letter.queue";

        /**
         * Maximum retry attempts before sending to DLQ.
         */
        @Min(0)
        @Max(10)
        private int maxRetries = 3;

        /**
         * Initial number of RabbitMQ consumers.
         */
        @Min(1)
        @Max(16)
        private int concurrency = 2;

        /**
         * Maximum number of RabbitMQ consumers.
         */
        @Min(1)
        @Max(32)
        private int maxConcurrency = 4;

        /**
         * Messages prefetched per consumer.
         */
        @Min(1)
        @Max(200)
        private int prefetch = 3;

        @AssertTrue(message = "maxConcurrency must be greater than or equal to concurrency")
        public boolean isConcurrencyRangeValid() {
            return maxConcurrency >= concurrency;
        }
    }

    @Data
    public static class ProcessingConfig {
        /**
         * Maximum concurrent external AI/API calls.
         */
        @Min(1)
        @Max(32)
        private int apiConcurrencyLimit = 6;

        /**
         * Maximum queued in-memory content hashes awaiting aggregation.
         */
        @Min(100)
        @Max(50_000)
        private int maxQueueSize = 1000;
    }

}
