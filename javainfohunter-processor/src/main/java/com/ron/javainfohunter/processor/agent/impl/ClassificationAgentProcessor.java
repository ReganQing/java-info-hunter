package com.ron.javainfohunter.processor.agent.impl;

import com.ron.javainfohunter.ai.service.ChatService;
import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.dto.RawContentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classification Agent Processor implementation.
 * <p>
 * This processor categorizes RSS content and assigns relevant tags using AI.
 * It processes content metadata and body to determine the appropriate category
 * from the predefined list and extracts meaningful tags.
 * </p>
 *
 * <p><b>Supported Categories:</b></p>
 * <ul>
 *   <li>Technology</li>
 *   <li>Business</li>
 *   <li>Science</li>
 *   <li>Health</li>
 *   <li>Politics</li>
 *   <li>Sports</li>
 *   <li>Entertainment</li>
 *   <li>World</li>
 *   <li>Environment</li>
 *   <li>Other</li>
 * </ul>
 *
 * <p><b>Configuration:</b></p>
 * <pre>
 * javainfohunter:
 *   processor:
 *     agents:
 *       classification:
 *         enabled: true
 *         timeout: 30000
 * </pre>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 * @see com.ron.javainfohunter.processor.agent.AgentProcessor
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "javainfohunter.processor.agents.classification",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ClassificationAgentProcessor implements AgentProcessor {

    /**
     * Supported content categories.
     */
    private static final String[] CATEGORIES = {
        "Technology",
        "Business",
        "Science",
        "Health",
        "Politics",
        "Sports",
        "Entertainment",
        "World",
        "Environment",
        "Other"
    };

    /**
     * Maximum content length to send to the AI (truncates larger content).
     */
    private static final int MAX_CONTENT_LENGTH = 3000;

    /**
     * Chat service for AI classification operations.
     */
    private final ChatService chatService;

    /**
     * Processor configuration properties.
     */
    private final ProcessorProperties properties;

    /**
     * System prompt for the classification agent.
     */
    private static final String SYSTEM_PROMPT = """
            You are a professional content classification expert.

            Your responsibilities:
            - Analyze article content and determine the primary category
            - Extract relevant tags and keywords from the content
            - Consider the title, author, and original classification if provided
            - Assign exactly one category from the supported list
            - Provide 5-10 relevant tags

            Supported Categories:
            """ + String.join(", ", CATEGORIES) + """

            Output format (strictly follow this):
            Category: [exact category name from the list above]

            Tags: [tag1, tag2, tag3, tag4, tag5]

            Guidelines:
            - Category MUST be one of the supported categories listed above
            - Tags should be lowercase, comma-separated, without hash symbols
            - Tags should represent: main topics, entities mentioned, themes
            - Prioritize specific, meaningful tags over generic ones
            """;

    /**
     * Process raw content with the classification agent.
     * <p>
     * This method builds a classification prompt with the content metadata
     * and sends it to the AI service. The response is parsed to extract
     * category and tags.
     * </p>
     *
     * @param content the raw content message to process
     * @return agent result containing category and tags, or error details
     */
    @Override
    public AgentResult process(RawContentMessage content) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Starting classification for content: {}", content.getTitle());

            // Build classification prompt
            String prompt = buildClassificationPrompt(content);

            // Call AI service for classification
            String response = chatService.chat(SYSTEM_PROMPT, prompt);

            // Parse the response for category and tags
            Map<String, Object> classificationData = parseClassificationResponse(
                response,
                content
            );

            log.debug("Classification completed for content: {} - Category: {}, Tags: {}",
                content.getTitle(), classificationData.get("category"),
                Arrays.toString((String[]) classificationData.get("tags")));

            return AgentResult.success(
                AgentResult.AgentType.CLASSIFICATION,
                content.getContentHash(),
                classificationData,
                System.currentTimeMillis() - startTime
            );

        } catch (Exception e) {
            log.error("Error during classification processing for content: {}",
                content.getTitle(), e);

            // Return fallback result with original category
            return AgentResult.success(
                AgentResult.AgentType.CLASSIFICATION,
                content.getContentHash(),
                getFallbackResult(content),
                System.currentTimeMillis() - startTime
            );
        }
    }

    @Override
    public AgentResult.AgentType getAgentType() {
        return AgentResult.AgentType.CLASSIFICATION;
    }

    /**
     * Build the classification prompt from raw content.
     * <p>
     * Includes content title, author, original category, existing tags,
     * and truncated content body.
     * </p>
     *
     * @param content the raw content message
     * @return formatted classification prompt
     */
    private String buildClassificationPrompt(RawContentMessage content) {
        StringBuilder prompt = new StringBuilder();

        // Add content metadata
        prompt.append("Title: ").append(nullableToString(content.getTitle())).append("\n");

        if (content.getAuthor() != null && !content.getAuthor().isEmpty()) {
            prompt.append("Author: ").append(content.getAuthor()).append("\n");
        }

        if (content.getCategory() != null && !content.getCategory().isEmpty()) {
            prompt.append("Original Category: ").append(content.getCategory()).append("\n");
        }

        if (content.getTags() != null && content.getTags().length > 0) {
            prompt.append("Original Tags: ").append(Arrays.toString(content.getTags())).append("\n");
        }

        if (content.getLink() != null) {
            prompt.append("URL: ").append(content.getLink()).append("\n");
        }

        // Add RSS source info
        if (content.getRssSourceName() != null) {
            prompt.append("Source: ").append(content.getRssSourceName()).append("\n");
        }

        // Add content body (truncated)
        String rawContent = content.getRawContent();
        if (rawContent != null && !rawContent.isEmpty()) {
            String truncatedContent = rawContent.length() > MAX_CONTENT_LENGTH
                ? rawContent.substring(0, MAX_CONTENT_LENGTH) + "..."
                : rawContent;
            prompt.append("\nContent:\n").append(truncatedContent).append("\n");
        }

        prompt.append("\nPlease classify this content and provide tags.");

        return prompt.toString();
    }

    /**
     * Parse the classification AI response.
     * <p>
     * Extracts category and tags from the AI response. Falls back to
     * original content metadata if parsing fails.
     * </p>
     *
     * @param response the AI response string
     * @param content the original content (for fallback)
     * @return map containing "category" and "tags"
     */
    private Map<String, Object> parseClassificationResponse(String response, RawContentMessage content) {
        String category = null;
        List<String> tags = new ArrayList<>();

        // Initialize with original values as fallback
        String fallbackCategory = content.getCategory();
        if (fallbackCategory == null || fallbackCategory.isEmpty()) {
            fallbackCategory = "Other";
        }

        List<String> fallbackTags = new ArrayList<>();
        if (content.getTags() != null && content.getTags().length > 0) {
            fallbackTags = Arrays.asList(content.getTags());
        }

        if (response != null && !response.isEmpty()) {
            // Try to extract category - looks for "Category: [name]"
            Pattern categoryPattern = Pattern.compile("Category:\\s*([^\\n\\[\\]]+)", Pattern.CASE_INSENSITIVE);
            Matcher categoryMatcher = categoryPattern.matcher(response);
            if (categoryMatcher.find()) {
                String extractedCategory = categoryMatcher.group(1).trim();
                // Validate category against allowed list
                for (String allowedCategory : CATEGORIES) {
                    if (allowedCategory.equalsIgnoreCase(extractedCategory)) {
                        category = allowedCategory;
                        break;
                    }
                }
            }

            // Try to extract tags - looks for "Tags: [tag1, tag2, ...]" or "Tags: tag1, tag2"
            Pattern tagsPattern = Pattern.compile(
                "Tags:\\s*\\[(.*?)\\]|Tags:\\s*([^\\n\\[\\]]+(?:,\\s*[^\\n\\[\\]]+)*)",
                Pattern.CASE_INSENSITIVE
            );
            Matcher tagsMatcher = tagsPattern.matcher(response);
            if (tagsMatcher.find()) {
                String tagsStr = tagsMatcher.group(1) != null ? tagsMatcher.group(1) : tagsMatcher.group(2);
                if (tagsStr != null) {
                    // Parse tags - comma-separated
                    String[] parsedTags = tagsStr.split(",");
                    tags = new ArrayList<>();
                    for (String tag : parsedTags) {
                        tag = tag.trim();
                        if (!tag.isEmpty()) {
                            // Remove leading # if present
                            if (tag.startsWith("#")) {
                                tag = tag.substring(1);
                            }
                            tags.add(tag);
                        }
                    }
                    // Limit to 10 tags
                    if (tags.size() > 10) {
                        tags = tags.subList(0, 10);
                    }
                }
            }

            // If no category found in structured format, try to find it in the response
            if (category == null || category.isEmpty()) {
                for (String allowedCategory : CATEGORIES) {
                    if (response.toLowerCase().contains(allowedCategory.toLowerCase())) {
                        category = allowedCategory;
                        break;
                    }
                }
            }
        }

        // Use fallbacks if parsing failed
        if (category == null || category.isEmpty()) {
            category = fallbackCategory;
        }

        if (tags.isEmpty() && !fallbackTags.isEmpty()) {
            tags = new ArrayList<>(fallbackTags);
        }

        // Build result map
        return Map.of(
            "category", category,
            "tags", tags.toArray(new String[0])
        );
    }

    /**
     * Get fallback classification result using original content metadata.
     *
     * @param content the raw content message
     * @return map with original category and tags
     */
    private Map<String, Object> getFallbackResult(RawContentMessage content) {
        String category = content.getCategory();
        if (category == null || category.isEmpty()) {
            category = "Other";
        }

        String[] tags = content.getTags();
        if (tags == null) {
            tags = new String[0];
        }

        return Map.of(
            "category", category,
            "tags", tags
        );
    }

    /**
     * Convert a potentially null string to a safe string representation.
     *
     * @param value the string value
     * @return the string or empty string if null
     */
    private String nullableToString(String value) {
        return value != null ? value : "";
    }

}
