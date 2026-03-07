package com.ron.javainfohunter.e2e;

import com.ron.javainfohunter.entity.RssSource;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.repository.RssSourceRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PostgreSQL database operations using Testcontainers.
 * Tests database connectivity, CRUD operations, and relationships.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Entity persistence with auto-generated ID</li>
 *   <li>Field values are correctly stored and retrieved</li>
 *   <li>Relationships between entities (RSS Source -> Raw Content)</li>
 *   <li>Query methods work correctly</li>
 *   <li>Update and delete operations</li>
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("PostgreSQL Integration Tests")
public class PostgreSQLIntegrationTest extends BaseExternalServiceTest {

    @Autowired
    private RssSourceRepository rssSourceRepository;

    @Autowired
    private RawContentRepository rawContentRepository;

    @AfterEach
    void tearDown() {
        // Clean up test data to ensure test isolation
        rawContentRepository.deleteAll();
        rssSourceRepository.deleteAll();
    }

    @Test
    @DisplayName("Should connect to PostgreSQL successfully")
    void shouldConnectToPostgreSQL() {
        // Verify repositories are injected and database is accessible
        assertThat(rssSourceRepository).isNotNull();
        assertThat(rawContentRepository).isNotNull();
        assertThat(rssSourceRepository.count()).isZero(); // Verify clean state
    }

    @Test
    @DisplayName("Should create and retrieve RSS source")
    void shouldCreateAndRetrieveRssSource() {
        // Arrange
        RssSource source = RssSource.builder()
                .url("https://example.com/feed.xml")
                .name("Test Feed")
                .category("technology")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .build();

        // Act
        RssSource saved = rssSourceRepository.save(source);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUrl()).isEqualTo("https://example.com/feed.xml");
        assertThat(saved.getName()).isEqualTo("Test Feed");

        Optional<RssSource> found = rssSourceRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Feed");
    }

    @Test
    @DisplayName("Should find active RSS sources")
    void shouldFindActiveRssSources() {
        // Arrange
        RssSource activeSource = RssSource.builder()
                .url("https://active.com/feed.xml")
                .name("Active Feed")
                .category("news")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .build();

        RssSource inactiveSource = RssSource.builder()
                .url("https://inactive.com/feed.xml")
                .name("Inactive Feed")
                .category("news")
                .isActive(false)
                .crawlIntervalSeconds(3600)
                .build();

        rssSourceRepository.save(activeSource);
        rssSourceRepository.save(inactiveSource);

        // Act
        List<RssSource> activeSources = rssSourceRepository.findByIsActiveTrue();

        // Assert
        assertThat(activeSources).hasSize(1);
        assertThat(activeSources.get(0).getName()).isEqualTo("Active Feed");
    }

    @Test
    @DisplayName("Should create raw content with RSS source relationship")
    void shouldCreateRawContentWithRelationship() {
        // Arrange
        RssSource source = RssSource.builder()
                .url("https://example.com/feed.xml")
                .name("Test Feed")
                .category("technology")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .build();
        RssSource savedSource = rssSourceRepository.save(source);

        RawContent content = RawContent.builder()
                .rssSource(savedSource)
                .title("Test Article")
                .link("https://example.com/article1")
                .contentHash("abc123")
                .rawContent("Test content here")
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build();

        // Act
        RawContent savedContent = rawContentRepository.save(content);

        // Assert
        assertThat(savedContent.getId()).isNotNull();
        assertThat(savedContent.getRssSource()).isNotNull();
        assertThat(savedContent.getRssSource().getId()).isEqualTo(savedSource.getId());
        assertThat(savedContent.getTitle()).isEqualTo("Test Article");
    }

    @Test
    @DisplayName("Should find raw content by processing status")
    void shouldFindRawContentByStatus() {
        // Arrange
        RssSource source = RssSource.builder()
                .url("https://example.com/feed.xml")
                .name("Test Feed")
                .category("technology")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .build();
        RssSource savedSource = rssSourceRepository.save(source);

        RawContent pendingContent = RawContent.builder()
                .rssSource(savedSource)
                .title("Pending Article")
                .link("https://example.com/article1")
                .contentHash("abc123")
                .rawContent("Test content")
                .processingStatus(RawContent.ProcessingStatus.PENDING)
                .build();

        RawContent processedContent = RawContent.builder()
                .rssSource(savedSource)
                .title("Processed Article")
                .link("https://example.com/article2")
                .contentHash("def456")
                .rawContent("Test content 2")
                .processingStatus(RawContent.ProcessingStatus.COMPLETED)
                .build();

        rawContentRepository.save(pendingContent);
        rawContentRepository.save(processedContent);

        // Act
        List<RawContent> pendingContentList = rawContentRepository.findByProcessingStatus(
            RawContent.ProcessingStatus.PENDING
        );

        // Assert
        assertThat(pendingContentList).hasSize(1);
        assertThat(pendingContentList.get(0).getTitle()).isEqualTo("Pending Article");
    }

    @Test
    @DisplayName("Should update RSS source last crawled timestamp")
    void shouldUpdateLastCrawledTimestamp() {
        // Arrange
        RssSource source = RssSource.builder()
                .url("https://example.com/feed.xml")
                .name("Test Feed")
                .category("technology")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .build();
        RssSource savedSource = rssSourceRepository.save(source);

        Instant beforeUpdate = Instant.now();

        // Act
        savedSource.setLastCrawledAt(Instant.now());
        rssSourceRepository.save(savedSource);

        // Assert
        Optional<RssSource> updated = rssSourceRepository.findById(savedSource.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getLastCrawledAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should delete RSS source and handle cascade")
    void shouldDeleteRssSource() {
        // Arrange
        RssSource source = RssSource.builder()
                .url("https://example.com/feed.xml")
                .name("Test Feed")
                .category("technology")
                .isActive(true)
                .crawlIntervalSeconds(3600)
                .build();
        RssSource savedSource = rssSourceRepository.save(source);

        // Act
        rssSourceRepository.delete(savedSource);

        // Assert
        Optional<RssSource> deleted = rssSourceRepository.findById(savedSource.getId());
        assertThat(deleted).isEmpty();
    }
}
