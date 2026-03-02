package com.ron.javainfohunter.ai.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 原始内容 DTO
 *
 * 用于阶段3: Fanout Exchange
 * 爬虫爬取的原始内容，需要广播给多个 Agent 处理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawContent {
    /**
     * 文章 URL
     */
    private String url;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
}
