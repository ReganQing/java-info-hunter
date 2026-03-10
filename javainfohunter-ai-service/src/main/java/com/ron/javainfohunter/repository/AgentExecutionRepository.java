package com.ron.javainfohunter.repository;

import com.ron.javainfohunter.entity.AgentExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Repository for Agent Execution entities
 *
 * Provides data access operations for AI agent execution tracking.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface AgentExecutionRepository extends JpaRepository<AgentExecution, Long> {

    /**
     * Find agent executions by agent ID
     *
     * @param agentId Agent ID
     * @param pageable Pagination parameters
     * @return Page of agent executions
     */
    Page<AgentExecution> findByAgentId(String agentId, Pageable pageable);

    /**
     * Find agent executions by status
     *
     * @param status Execution status
     * @return List of agent executions with the status
     */
    List<AgentExecution> findByStatus(AgentExecution.ExecutionStatus status);

    /**
     * Find agent executions by status with pagination
     *
     * @param status Execution status
     * @param pageable Pagination parameters
     * @return Page of agent executions
     */
    Page<AgentExecution> findByStatus(AgentExecution.ExecutionStatus status, Pageable pageable);

    /**
     * Find agent execution by execution ID
     *
     * @param executionId Unique execution identifier
     * @return Optional containing the agent execution if found
     */
    List<AgentExecution> findByExecutionId(String executionId);

    /**
     * Find agent executions by task type
     *
     * @param taskType Task type
     * @param pageable Pagination parameters
     * @return Page of agent executions
     */
    Page<AgentExecution> findByTaskType(String taskType, Pageable pageable);

    /**
     * Find agent executions by coordination pattern
     *
     * @param pattern Coordination pattern
     * @param pageable Pagination parameters
     * @return Page of agent executions
     */
    Page<AgentExecution> findByCoordinationPattern(AgentExecution.CoordinationPattern pattern, Pageable pageable);

    /**
     * Find agent executions by correlation ID
     * (For tracking related executions across coordinated agents)
     *
     * @param correlationId Correlation ID
     * @return List of related agent executions
     */
    List<AgentExecution> findByCorrelationId(String correlationId);

    /**
     * Find agent executions by parent execution ID
     *
     * @param parentExecutionId Parent execution ID
     * @return List of child agent executions
     */
    List<AgentExecution> findByParentExecutionId(String parentExecutionId);

    /**
     * Find agent executions for a specific raw content
     *
     * @param rawContentId Raw content ID
     * @return List of agent executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.rawContent.id = :rawContentId")
    List<AgentExecution> findByRawContentId(@Param("rawContentId") Long rawContentId);

    /**
     * Find agent executions for a specific news item
     *
     * @param newsId News ID
     * @return List of agent executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.news.id = :newsId")
    List<AgentExecution> findByNewsId(@Param("newsId") Long newsId);

    /**
     * Find agent executions within a time range
     *
     * @param startTime Start time
     * @param endTime End time
     * @param pageable Pagination parameters
     * @return Page of agent executions within the time range
     */
    Page<AgentExecution> findByStartTimeBetweenOrderByStartTimeDesc(
        Instant startTime, Instant endTime, Pageable pageable
    );

    /**
     * Find recent agent executions
     *
     * @param since Time threshold
     * @param pageable Pagination parameters
     * @return Page of recent agent executions
     */
    Page<AgentExecution> findByStartTimeAfterOrderByStartTimeDesc(Instant since, Pageable pageable);

    /**
     * Find agent executions by agent ID and status
     *
     * @param agentId Agent ID
     * @param status Execution status
     * @param pageable Pagination parameters
     * @return Page of agent executions
     */
    Page<AgentExecution> findByAgentIdAndStatus(String agentId, AgentExecution.ExecutionStatus status, Pageable pageable);

    /**
     * Find running agent executions
     *
     * @return List of currently running executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.status = 'RUNNING'")
    List<AgentExecution> findRunningExecutions();

    /**
     * Find stuck executions (running longer than specified duration)
     *
     * @param maxDurationMillis Maximum allowed duration in milliseconds
     * @return List of potentially stuck executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.status = 'RUNNING' " +
           "AND ae.startTime < :threshold")
    List<AgentExecution> findStuckExecutions(@Param("threshold") Instant threshold);

    /**
     * Count agent executions by agent ID
     *
     * @param agentId Agent ID
     * @return Number of executions for the agent
     */
    long countByAgentId(String agentId);

    /**
     * Count agent executions by status
     *
     * @param status Execution status
     * @return Number of executions with the status
     */
    long countByStatus(AgentExecution.ExecutionStatus status);

    /**
     * Count agent executions by agent ID and status
     *
     * @param agentId Agent ID
     * @param status Execution status
     * @return Number of executions
     */
    long countByAgentIdAndStatus(String agentId, AgentExecution.ExecutionStatus status);

    /**
     * Get agent execution statistics for a specific agent
     *
     * @param agentId Agent ID
     * @return Statistics object
     */
    @Query("SELECT new com.ron.javainfohunter.dto.AgentExecutionStats(" +
           "ae.agentId, COUNT(ae), " +
           "SUM(CASE WHEN ae.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ae.status = 'FAILED' THEN 1 ELSE 0 END), " +
           "AVG(CAST(ae.durationMilliseconds AS double)), SUM(ae.tokensUsed), " +
           "CAST(SUM(ae.estimatedCostUsd) AS double)) " +
           "FROM AgentExecution ae WHERE ae.agentId = :agentId")
    Object getAgentStatistics(@Param("agentId") String agentId);

    /**
     * Get execution statistics by agent type
     *
     * @return List of objects with agent type statistics
     */
    @Query("SELECT ae.agentType, COUNT(ae), " +
           "SUM(CASE WHEN ae.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ae.status = 'FAILED' THEN 1 ELSE 0 END), " +
           "AVG(ae.durationMilliseconds) " +
           "FROM AgentExecution ae " +
           "WHERE ae.startTime > :since " +
           "GROUP BY ae.agentType")
    List<Object[]> getStatisticsByAgentType(@Param("since") Instant since);

    /**
     * Find failed executions eligible for retry
     *
     * @param agentId Agent ID (optional)
     * @param maxRetries Maximum retry count threshold
     * @param pageable Pagination parameters
     * @return Page of failed executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.status = 'FAILED' " +
           "AND ae.retryCount < :maxRetries " +
           "AND (:agentId IS NULL OR ae.agentId = :agentId)")
    Page<AgentExecution> findFailedExecutionsForRetry(
        @Param("agentId") String agentId,
        @Param("maxRetries") Integer maxRetries,
        Pageable pageable
    );

    /**
     * Find executions with errors
     *
     * @param pageable Pagination parameters
     * @return Page of failed executions with error traces
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.status = 'FAILED' " +
           "AND ae.errorTrace IS NOT NULL ORDER BY ae.endTime DESC")
    Page<AgentExecution> findExecutionsWithErrors(Pageable pageable);

    /**
     * Find slow executions
     *
     * @param minDurationMillis Minimum duration threshold
     * @param pageable Pagination parameters
     * @return Page of slow executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.durationMilliseconds > :minDurationMillis " +
           "ORDER BY ae.durationMilliseconds DESC")
    Page<AgentExecution> findSlowExecutions(@Param("minDurationMillis") Long minDurationMillis, Pageable pageable);

    /**
     * Find expensive executions (by cost)
     *
     * @param minCostUsd Minimum cost threshold
     * @param pageable Pagination parameters
     * @return Page of expensive executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE ae.estimatedCostUsd > :minCostUsd " +
           "ORDER BY ae.estimatedCostUsd DESC")
    Page<AgentExecution> findExpensiveExecutions(@Param("minCostUsd") BigDecimal minCostUsd, Pageable pageable);

    /**
     * Get token usage statistics
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of objects with token statistics
     */
    @Query("SELECT ae.agentId, SUM(ae.tokensUsed), SUM(ae.estimatedCostUsd), COUNT(ae) " +
           "FROM AgentExecution ae " +
           "WHERE ae.startTime BETWEEN :startDate AND :endDate " +
           "GROUP BY ae.agentId")
    List<Object[]> getTokenUsageStatistics(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find executions with tool usage
     *
     * @param toolName Tool name
     * @return List of executions that used the specified tool
     */
    @Query("SELECT ae FROM AgentExecution ae JOIN ae.toolsUsed tool WHERE tool = :toolName")
    List<AgentExecution> findByToolUsed(@Param("toolName") String toolName);

    /**
     * Get coordination pattern usage statistics
     *
     * @return List of objects with coordination pattern statistics
     */
    @Query("SELECT ae.coordinationPattern, COUNT(ae), " +
           "AVG(ae.durationMilliseconds), AVG(ae.totalSteps) " +
           "FROM AgentExecution ae WHERE ae.coordinationPattern IS NOT NULL " +
           "GROUP BY ae.coordinationPattern")
    List<Object[]> getCoordinationPatternStatistics();

    /**
     * Find agent executions with duration statistics
     *
     * @param agentId Agent ID
     * @return Statistics object with min, max, avg duration
     */
    @Query("SELECT MIN(ae.durationMilliseconds), MAX(ae.durationMilliseconds), AVG(ae.durationMilliseconds) " +
           "FROM AgentExecution ae WHERE ae.agentId = :agentId AND ae.status = 'COMPLETED'")
    Object[] getDurationStatistics(@Param("agentId") String agentId);

    /**
     * Bulk update status for stuck executions
     *
     * @param threshold Time threshold for stuck executions
     * @param newStatus New status to set
     * @return Number of updated records
     */
    @Query("UPDATE AgentExecution ae SET ae.status = :newStatus, ae.endTime = CURRENT_TIMESTAMP " +
           "WHERE ae.status = 'RUNNING' AND ae.startTime < :threshold")
    int markStuckExecutionsAsFailed(@Param("threshold") Instant threshold, @Param("newStatus") AgentExecution.ExecutionStatus newStatus);

    /**
     * Find recent executions for dashboard
     *
     * @param limit Maximum number of results
     * @return List of recent executions
     */
    @Query("SELECT ae FROM AgentExecution ae ORDER BY ae.startTime DESC")
    List<AgentExecution> findRecentExecutions(Pageable pageable);

    /**
     * Delete old execution records
     *
     * @param beforeDate Delete executions older than this date
     * @return Number of deleted records
     */
    @Query("DELETE FROM AgentExecution ae WHERE ae.startTime < :beforeDate " +
           "AND ae.status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    int deleteOldExecutions(@Param("beforeDate") Instant beforeDate);

    /**
     * Find executions by multiple filters
     *
     * @param agentId Agent ID (optional)
     * @param status Execution status (optional)
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @param pageable Pagination parameters
     * @return Page of filtered executions
     */
    @Query("SELECT ae FROM AgentExecution ae WHERE " +
           "(:agentId IS NULL OR ae.agentId = :agentId) AND " +
           "(:status IS NULL OR ae.status = :status) AND " +
           "(:startDate IS NULL OR ae.startTime >= :startDate) AND " +
           "(:endDate IS NULL OR ae.startTime <= :endDate)")
    Page<AgentExecution> findByFilters(
        @Param("agentId") String agentId,
        @Param("status") AgentExecution.ExecutionStatus status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    /**
     * Get average execution time by agent
     *
     * @param since Time threshold
     * @return List of agents with average execution times
     */
    @Query("SELECT ae.agentId, AVG(ae.durationMilliseconds), COUNT(ae) " +
           "FROM AgentExecution ae " +
           "WHERE ae.startTime > :since AND ae.status = 'COMPLETED' " +
           "GROUP BY ae.agentId " +
           "ORDER BY AVG(ae.durationMilliseconds) DESC")
    List<Object[]> getAverageExecutionTimeByAgent(@Param("since") Instant since);
}
