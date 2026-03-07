package com.ron.javainfohunter.ai.agent.specialized;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 趋势追踪协调者 Agent
 * <p>
 * 专门用于热点话题追踪的协调者 Agent，协调以下 Workers：
 * 1. CrawlerAgent - 爬取最新内容
 * 2. AnalysisAgent - 分析内容趋势
 * 3. AlertAgent - 发送趋势告警
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class TrendingCoordinatorAgent extends CoordinatorAgent {

    /**
     * 默认 Workers 列表
     */
    private static final String[] DEFAULT_WORKERS = {
        "crawler-agent",
        "analysis-agent",
        "alert-agent"
    };

    /**
     * 构造函数
     */
    public TrendingCoordinatorAgent() {
        super("TrendingCoordinator");
        // 设置默认 Workers
        setWorkers(List.of(DEFAULT_WORKERS));
    }

    /**
     * 构造函数（自定义名称）
     *
     * @param name Agent 名称
     */
    public TrendingCoordinatorAgent(String name) {
        super(name);
        // 设置默认 Workers
        setWorkers(List.of(DEFAULT_WORKERS));
    }

    /**
     * 执行趋势追踪任务
     *
     * @param topic 追踪的话题
     * @return 追踪结果
     */
    public String trackTrending(String topic) {
        log.info("Starting trending tracking for topic: {}", topic);

        // 构建任务描述
        String taskDescription = String.format(
            "Track trending topic: %s. " +
            "1. Crawl latest content about this topic. " +
            "2. Analyze trends and patterns. " +
            "3. Send alerts if significant trends detected.",
            topic
        );

        // 运行协调流程
        String result = run(taskDescription);

        log.info("Trending tracking completed for topic: {}", topic);
        return result;
    }

    /**
     * 获取追踪摘要
     *
     * @return 摘要信息
     */
    public String getTrackingSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Trending Tracking Summary:\n");
        sb.append("Workers: ").append(getWorkers()).append("\n");
        sb.append("Results received: ").append(getWorkerResults().size()).append("\n");

        if (!getWorkerResults().isEmpty()) {
            sb.append("\nWorker Details:\n");
            getWorkerResults().forEach(result -> {
                sb.append(String.format("- %s: success=%b\n",
                    result.getWorkerId(), result.isSuccess()));
            });
        }

        return sb.toString();
    }
}
