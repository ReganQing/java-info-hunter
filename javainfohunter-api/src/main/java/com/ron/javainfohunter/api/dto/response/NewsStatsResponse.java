package com.ron.javainfohunter.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NewsStatsResponse {
    private List<CategoryStats> categoryStats;
    private List<SentimentStats> sentimentStats;
    private long totalPublished;

    @Data
    @Builder
    public static class CategoryStats {
        private String category;
        private long count;
    }

    @Data
    @Builder
    public static class SentimentStats {
        private String sentiment;
        private long count;
        private Double avgScore;
    }
}
