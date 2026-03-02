package com.ron.javainfohunter.ai.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新闻 DTO
 *
 * 用于阶段4: Topic Exchange
 * 带有类别信息的新闻对象，根据路由键分发到不同的处理队列
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class News {
    /**
     * 一级类别（如: tech, finance, sports）
     */
    private String category;

    /**
     * 二级类别（如: ai, stock, nba）
     */
    private String subcategory;

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 新闻内容
     */
    private String content;
}
