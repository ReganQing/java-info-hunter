package com.ron.javainfohunter.ai.tool.impl;

import com.ron.javainfohunter.ai.tool.annotation.Tool;
import com.ron.javainfohunter.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文本摘要工具
 * <p>
 * 基于句子重要性提取摘要
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class TextSummarizationTool {

    /**
     * 提取文本摘要（基于句子重要性）
     *
     * @param text       原始文本
     * @param maxSummary 摘要最大句子数（默认3句）
     * @return 摘要文本
     */
    @Tool(description = "从长文本中提取关键句子生成摘要")
    public String extractSummary(
            @ToolParam("要摘要的文本内容") String text,
            @ToolParam("摘要的最大句子数（默认3）") int maxSummary) {

        if (text == null || text.trim().isEmpty()) {
            return "文本为空";
        }

        try {
            // 分割句子（按中文句号、问号、感叹号）
            String[] sentences = text.split("[。？！!？\\n]");
            sentences = Arrays.stream(sentences)
                    .map(String::trim)
                    .filter(s -> s.length() > 10) // 过滤短句
                    .toArray(String[]::new);

            if (sentences.length == 0) {
                return "无法提取摘要（文本过短）";
            }

            if (sentences.length <= maxSummary) {
                return String.join("。", sentences) + "。";
            }

            // 计算句子重要性（词频）
            Map<String, Integer> wordFreq = calculateWordFrequency(text);

            // 为每个句子打分
            Map<String, Double> sentenceScores = new HashMap<>();
            for (String sentence : sentences) {
                double score = 0;
                for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
                    if (sentence.contains(entry.getKey())) {
                        score += entry.getValue();
                    }
                }
                // 归一化：除以句子长度，避免偏向长句
                sentenceScores.put(sentence, score / sentence.length());
            }

            // 选择得分最高的句子
            String[] summary = sentenceScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(maxSummary)
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new);

            // 按原文顺序排列
            StringBuilder result = new StringBuilder();
            for (String sentence : sentences) {
                if (Arrays.asList(summary).contains(sentence)) {
                    result.append(sentence).append("。");
                }
            }

            return result.toString();
        } catch (Exception e) {
            log.error("摘要生成失败", e);
            return "摘要生成失败：" + e.getMessage();
        }
    }

    /**
     * 计算词频
     */
    private Map<String, Integer> calculateWordFrequency(String text) {
        // 简单分词（按空格和常见中文分隔符）
        String[] words = text.split("[\\s\\p{Punct}]+");

        return Arrays.stream(words)
                .filter(w -> w.length() > 1) // 过滤单字
                .filter(w -> !isStopWord(w))  // 过滤停用词
                .collect(Collectors.toMap(
                        w -> w,
                        w -> 1,
                        Integer::sum
                ));
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        String[] stopWords = {
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
                "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
                "你", "会", "着", "没有", "看", "好", "自己", "这", "the", "a", "an"
        };
        return Arrays.asList(stopWords).contains(word.toLowerCase());
    }

    /**
     * 生成关键词列表
     *
     * @param text    文本内容
     * @param topN    返回前 N 个关键词
     * @return 关键词列表
     */
    @Tool(description = "从文本中提取关键词")
    public String extractKeywords(
            @ToolParam("文本内容") String text,
            @ToolParam("返回关键词数量（默认10）") int topN) {

        if (text == null || text.trim().isEmpty()) {
            return "文本为空";
        }

        try {
            Map<String, Integer> wordFreq = calculateWordFrequency(text);

            String[] keywords = wordFreq.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new);

            return String.join("、", keywords);
        } catch (Exception e) {
            log.error("关键词提取失败", e);
            return "关键词提取失败：" + e.getMessage();
        }
    }
}
