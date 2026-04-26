package com.ron.javainfohunter.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

/**
 * AI 向量化服务
 * <p>
 * 提供文本向量化功能，用于语义搜索和相似度计算
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * 将文本转向量
     *
     * @param text 输入文本
     * @return 向量数组
     */
    public float[] embed(String text) {
        log.debug("Embedding text: {}", text.substring(0, Math.min(100, text.length())));
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        return response.getResult().getOutput();
    }

    /**
     * 批量将文本转向量
     *
     * @param texts 输入文本列表
     * @return 向量数组列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Embedding {} texts", texts.size());
        return embeddingModel.embed(texts);
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度（0-1之间，1表示完全相同）
     */
    public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
