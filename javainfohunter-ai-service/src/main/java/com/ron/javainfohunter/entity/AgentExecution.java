package com.ron.javainfohunter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Agent Execution Entity
 *
 * Tracks AI agent executions for debugging, analytics, and optimization.
 * Records execution context, performance metrics, and results.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "agent_executions", indexes = {
    @Index(name = "idx_agent_executions_agent_id", columnList = "agent_id"),
    @Index(name = "idx_agent_executions_agent_type", columnList = "agent_type"),
    @Index(name = "idx_agent_executions_execution_id", columnList = "execution_id"),
    @Index(name = "idx_agent_executions_status", columnList = "status"),
    @Index(name = "idx_agent_executions_start_time", columnList = "start_time"),
    @Index(name = "idx_agent_executions_task_type", columnList = "task_type"),
    @Index(name = "idx_agent_executions_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_agent_executions_agent_time", columnList = "agent_id, start_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecution {

    /**
     * Execution status enum
     */
    public enum ExecutionStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Coordination pattern enum
     */
    public enum CoordinationPattern {
        CHAIN,
        PARALLEL,
        MASTER_WORKER,
        STANDALONE
    }

    /**
     * Primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Registered agent ID (e.g., 'crawler-agent', 'analysis-agent')
     */
    @NotBlank(message = "Agent ID cannot be blank")
    @Size(max = 100, message = "Agent ID must not exceed 100 characters")
    @Column(nullable = false, length = 100, name = "agent_id")
    private String agentId;

    /**
     * Agent type (BaseAgent, ReActAgent, ToolCallAgent)
     */
    @NotBlank(message = "Agent type cannot be blank")
    @Size(max = 50, message = "Agent type must not exceed 50 characters")
    @Column(nullable = false, length = 50, name = "agent_type")
    private String agentType;

    /**
     * Human-readable agent name
     */
    @Size(max = 255, message = "Agent name must not exceed 255 characters")
    @Column(name = "agent_name")
    private String agentName;

    /**
     * Unique execution identifier (UUID)
     */
    @NotBlank(message = "Execution ID cannot be blank")
    @Size(max = 100, message = "Execution ID must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100, name = "execution_id")
    private String executionId;

    /**
     * Type of task being executed
     */
    @Size(max = 100, message = "Task type must not exceed 100 characters")
    @Column(name = "task_type")
    private String taskType;

    /**
     * Coordination pattern used
     */
    @Enumerated(EnumType.STRING)
    @Size(max = 50, message = "Coordination pattern must not exceed 50 characters")
    @Column(length = 50, name = "coordination_pattern")
    private CoordinationPattern coordinationPattern;

    /**
     * Input parameters and context (JSONB)
     */
    @Lob
    @Column(columnDefinition = "JSONB", name = "input_data")
    private String inputData;

    /**
     * Agent output and results (JSONB)
     */
    @Lob
    @Column(columnDefinition = "JSONB", name = "output_data")
    private String outputData;

    /**
     * Error stack trace if execution failed
     */
    @Lob
    @Column(columnDefinition = "TEXT", name = "error_trace")
    private String errorTrace;

    /**
     * Execution status
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status cannot be null")
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    /**
     * Execution start timestamp
     */
    @NotNull(message = "Start time cannot be null")
    @Column(nullable = false, name = "start_time")
    private Instant startTime;

    /**
     * Execution end timestamp
     */
    @Column(name = "end_time")
    private Instant endTime;

    /**
     * Execution duration in milliseconds
     */
    @Column(name = "duration_milliseconds")
    private Integer durationMilliseconds;

    /**
     * Total number of steps taken
     */
    @Column(name = "total_steps")
    private Integer totalSteps;

    /**
     * Total tokens used (if applicable)
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    /**
     * Estimated LLM API cost in USD
     */
    @Column(precision = 10, scale = 4, name = "estimated_cost_usd")
    private BigDecimal estimatedCostUsd;

    /**
     * List of tools used during execution (stored as text array in DB)
     */
    @Column(name = "tools_used", columnDefinition = "text[]")
    private String[] toolsUsed;

    /**
     * Number of tool calls made
     */
    @Column(name = "tool_call_count")
    private Integer toolCallCount;

    /**
     * Reference to raw content (optional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_content_id", foreignKey = @ForeignKey(name = "fk_agent_execution_raw_content"))
    private RawContent rawContent;

    /**
     * Reference to news (optional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", foreignKey = @ForeignKey(name = "fk_agent_execution_news"))
    private News news;

    /**
     * Parent execution ID for chained/coordinated executions
     */
    @Size(max = 100, message = "Parent execution ID must not exceed 100 characters")
    @Column(name = "parent_execution_id")
    private String parentExecutionId;

    /**
     * Correlation ID for tracking related executions
     */
    @Size(max = 100, message = "Correlation ID must not exceed 100 characters")
    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * Current retry count
     */
    @NotNull(message = "Retry count cannot be null")
    @Column(nullable = false)
    private Integer retryCount = 0;

    /**
     * Maximum number of retries allowed
     */
    @NotNull(message = "Max retries cannot be null")
    @Column(nullable = false)
    private Integer maxRetries = 3;

    /**
     * Timestamp when this record was created
     */
    @NotNull(message = "Created at cannot be null")
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    /**
     * Timestamp when this record was last updated
     */
    @NotNull(message = "Updated at cannot be null")
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    /**
     * Lifecycle callback: Set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (startTime == null) {
            startTime = now;
        }
        if (status == null) {
            status = ExecutionStatus.RUNNING;
        }
    }

    /**
     * Lifecycle callback: Update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        // Calculate duration if endTime is set
        if (endTime != null && startTime != null && durationMilliseconds == null) {
            durationMilliseconds = (int) (endTime.toEpochMilli() - startTime.toEpochMilli());
        }
    }

    /**
     * Check if execution is currently running
     *
     * @return true if status is RUNNING
     */
    public boolean isRunning() {
        return status == ExecutionStatus.RUNNING;
    }

    /**
     * Check if execution completed successfully
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return status == ExecutionStatus.COMPLETED;
    }

    /**
     * Check if execution failed
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }

    /**
     * Check if execution was cancelled
     *
     * @return true if status is CANCELLED
     */
    public boolean isCancelled() {
        return status == ExecutionStatus.CANCELLED;
    }

    /**
     * Check if execution is finished (not running)
     *
     * @return true if status is not RUNNING
     */
    public boolean isFinished() {
        return status != ExecutionStatus.RUNNING;
    }

    /**
     * Mark execution as completed
     */
    public void markAsCompleted() {
        this.status = ExecutionStatus.COMPLETED;
        this.endTime = Instant.now();
        this.errorTrace = null;
    }

    /**
     * Mark execution as failed with error trace
     *
     * @param errorTrace Error stack trace
     */
    public void markAsFailed(String errorTrace) {
        this.status = ExecutionStatus.FAILED;
        this.endTime = Instant.now();
        this.errorTrace = errorTrace;
    }

    /**
     * Mark execution as cancelled
     */
    public void markAsCancelled() {
        this.status = ExecutionStatus.CANCELLED;
        this.endTime = Instant.now();
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;
    }

    /**
     * Check if more retries are available
     *
     * @return true if retry count < max retries
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Calculate duration in seconds
     *
     * @return Duration in seconds, or null if not available
     */
    public Double getDurationInSeconds() {
        if (durationMilliseconds == null) {
            return null;
        }
        return durationMilliseconds / 1000.0;
    }

    /**
     * Check if this execution is part of a coordinated workflow
     *
     * @return true if has parent execution or correlation ID
     */
    public boolean isCoordinated() {
        return parentExecutionId != null || correlationId != null;
    }

    /**
     * Add tool to the list of used tools
     *
     * @param tool Tool name
     */
    public void addTool(String tool) {
        if (toolCallCount == null) {
            toolCallCount = 0;
        }
        toolCallCount++;

        if (toolsUsed == null) {
            toolsUsed = new String[]{tool};
        } else {
            String[] newTools = Arrays.copyOf(toolsUsed, toolsUsed.length + 1);
            newTools[toolsUsed.length] = tool;
            toolsUsed = newTools;
        }
    }

    /**
     * Update total steps
     *
     * @param steps Number of steps
     */
    public void updateTotalSteps(Integer steps) {
        this.totalSteps = steps;
    }

    /**
     * Update token usage and cost
     *
     * @param tokens Number of tokens used
     * @param cost Estimated cost in USD
     */
    public void updateTokenUsage(Integer tokens, BigDecimal cost) {
        this.tokensUsed = tokens;
        this.estimatedCostUsd = cost;
    }
}
