package com.ron.javainfohunter.ai.tool.observation;

/**
 * 错误类型分类
 * <p>
 * 用于 Agent 判断错误是否可重试以及应采取何种恢复策略。
 * </p>
 */
public enum ErrorType {
    /** 网络错误（可重试） */
    NETWORK,
    /** 解析错误（不可重试） */
    PARSE,
    /** 输入验证失败（不可重试） */
    VALIDATION,
    /** 执行超时（可重试） */
    TIMEOUT,
    /** 速率限制（可重试，需等待） */
    RATE_LIMIT,
    /** 致命错误（不可重试） */
    FATAL
}
