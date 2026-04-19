package com.ron.javainfohunter.ai.tool.impl;

import com.ron.javainfohunter.ai.tool.annotation.Tool;
import com.ron.javainfohunter.ai.tool.annotation.ToolParam;
import com.ron.javainfohunter.ai.tool.observation.ErrorContext;
import com.ron.javainfohunter.ai.tool.observation.ErrorType;
import com.ron.javainfohunter.ai.tool.observation.ToolObservation;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * HTML 解析工具
 * <p>
 * 使用 Jsoup 解析 HTML 内容并提取文本，返回结构化 ToolObservation。
 * </p>
 *
 * @author Ron
 * @since 1.0.0
 */
@Slf4j
@Component
public class HtmlParserTool {

    /**
     * 解析 HTML 并提取正文
     *
     * @param html HTML 内容
     * @return 结构化观测结果
     */
    @Tool(description = "解析 HTML 内容并提取正文文本，去除广告和无关内容")
    public ToolObservation parseHtml(@ToolParam("HTML 内容") String html) {
        if (html == null || html.isBlank()) {
            return ToolObservation.failure("HTML 内容为空",
                new ErrorContext(ErrorType.VALIDATION, "html is null or blank",
                    "HTML 内容不能为空", false, "提供有效的 HTML 内容"));
        }

        try {
            Document doc = Jsoup.parse(html);

            // 移除常见广告和导航元素
            doc.select("script, style, nav, footer, aside, .ad, .advertisement, .banner").remove();

            // 提取标题
            String title = doc.title();
            if (title == null || title.isEmpty()) {
                title = doc.select("h1").first() != null ? doc.select("h1").first().text() : "无标题";
            }

            // 提取正文
            String text = doc.body().text();

            // 清理多余空白
            text = text.replaceAll("\\s+", " ").trim();

            String details = String.format("【标题】%s\n\n【正文】%s", title, text);

            return ToolObservation.success(
                "解析完成：标题 '%s'，正文 %d 字".formatted(title, text.length()),
                details
            );
        } catch (Exception e) {
            log.error("HTML 解析失败", e);
            return ToolObservation.failure("HTML 解析失败",
                new ErrorContext(ErrorType.PARSE, e.getMessage(),
                    "HTML 解析过程中发生错误", false, "检查 HTML 内容格式"));
        }
    }

    /**
     * 从 HTML 中提取文章元数据
     *
     * @param html HTML 内容
     * @return 结构化观测结果
     */
    @Tool(description = "从 HTML 中提取文章元数据（标题、作者、发布时间等）")
    public ToolObservation extractMetadata(@ToolParam("HTML 内容") String html) {
        if (html == null || html.isBlank()) {
            return ToolObservation.failure("HTML 内容为空",
                new ErrorContext(ErrorType.VALIDATION, "html is null or blank",
                    "HTML 内容不能为空", false, "提供有效的 HTML 内容"));
        }

        try {
            Document doc = Jsoup.parse(html);

            StringBuilder metadata = new StringBuilder();
            metadata.append("【文章元数据】\n");

            // 标题
            String title = doc.title();
            if (!title.isEmpty()) {
                metadata.append("标题：").append(title).append("\n");
            }

            // 作者（常见选择器）
            String[] authorSelectors = {"author", ".author", "by-author", "[itemprop='author']"};
            for (String selector : authorSelectors) {
                String author = doc.select(selector).text();
                if (!author.isEmpty()) {
                    metadata.append("作者：").append(author).append("\n");
                    break;
                }
            }

            // 发布时间
            String[] timeSelectors = {"time", ".time", ".date", "[itemprop='datePublished']"};
            for (String selector : timeSelectors) {
                String time = doc.select(selector).text();
                if (!time.isEmpty()) {
                    metadata.append("发布时间：").append(time).append("\n");
                    break;
                }
            }

            // 描述
            String description = doc.select("meta[name='description']").attr("content");
            if (!description.isEmpty()) {
                metadata.append("描述：").append(description).append("\n");
            }

            // 关键词
            String keywords = doc.select("meta[name='keywords']").attr("content");
            if (!keywords.isEmpty()) {
                metadata.append("关键词：").append(keywords).append("\n");
            }

            return ToolObservation.success(
                "元数据提取完成：标题 '%s'".formatted(title),
                metadata.toString()
            );
        } catch (Exception e) {
            log.error("元数据提取失败", e);
            return ToolObservation.failure("元数据提取失败",
                new ErrorContext(ErrorType.PARSE, e.getMessage(),
                    "元数据提取过程中发生错误", false, "检查 HTML 内容格式"));
        }
    }

    /**
     * 提取 HTML 中的所有链接
     *
     * @param html HTML 内容
     * @return 结构化观测结果
     */
    @Tool(description = "提取 HTML 中的所有链接 URL")
    public ToolObservation extractLinks(@ToolParam("HTML 内容") String html) {
        if (html == null || html.isBlank()) {
            return ToolObservation.failure("HTML 内容为空",
                new ErrorContext(ErrorType.VALIDATION, "html is null or blank",
                    "HTML 内容不能为空", false, "提供有效的 HTML 内容"));
        }

        try {
            Document doc = Jsoup.parse(html);
            var links = doc.select("a[href]");

            StringBuilder result = new StringBuilder();
            result.append("【链接列表】\n");

            links.stream()
                    .limit(20) // 限制最多20个链接
                    .forEach(link -> {
                        String url = link.attr("abs:href");
                        String text = link.text().trim();
                        result.append(String.format("- %s: %s\n",
                                text.isEmpty() ? url : text, url));
                    });

            if (links.size() > 20) {
                result.append(String.format("... 还有 %d 个链接\n", links.size() - 20));
            }

            return ToolObservation.success(
                "提取了 %d 个链接".formatted(Math.min(links.size(), 20)),
                result.toString()
            );
        } catch (Exception e) {
            log.error("链接提取失败", e);
            return ToolObservation.failure("链接提取失败",
                new ErrorContext(ErrorType.PARSE, e.getMessage(),
                    "链接提取过程中发生错误", false, "检查 HTML 内容格式"));
        }
    }

    /**
     * 清理文本中的 HTML 标签
     *
     * @param html HTML 内容
     * @return 结构化观测结果
     */
    @Tool(description = "移除所有 HTML 标签，保留纯文本内容")
    public ToolObservation stripHtml(@ToolParam("HTML 内容") String html) {
        if (html == null || html.isBlank()) {
            return ToolObservation.failure("HTML 内容为空",
                new ErrorContext(ErrorType.VALIDATION, "html is null or blank",
                    "HTML 内容不能为空", false, "提供有效的 HTML 内容"));
        }

        try {
            Document doc = Jsoup.parse(html);
            String text = doc.text().replaceAll("\\s+", " ").trim();

            return ToolObservation.success(
                "HTML 标签清理完成，纯文本 %d 字".formatted(text.length()),
                text
            );
        } catch (Exception e) {
            log.error("HTML 清理失败", e);
            return ToolObservation.failure("HTML 清理失败",
                new ErrorContext(ErrorType.PARSE, e.getMessage(),
                    "HTML 清理过程中发生错误", false, "检查 HTML 内容格式"));
        }
    }
}
