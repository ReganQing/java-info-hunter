package com.ron.javainfohunter.ai.tool.observation;

/**
 * 结构化错误上下文
 * <p>
 * 为每个错误路径提供：根因提示、安全重试指令和明确的停止条件。
 * </p>
 *
 * @param rootCause   技术层面的错误原因
 * @param userMessage Agent 可理解的错误描述
 * @param retryable   是否可安全重试
 * @param retryHint   重试建议（如 "等待 5 秒后重试"），null 表示不应重试
 */
public record ErrorContext(
    ErrorType type,
    String rootCause,
    String userMessage,
    boolean retryable,
    String retryHint
) {}
