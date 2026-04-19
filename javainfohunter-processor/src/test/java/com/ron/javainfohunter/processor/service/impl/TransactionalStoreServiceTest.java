package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.ai.service.EmbeddingService;
import com.ron.javainfohunter.entity.News;
import com.ron.javainfohunter.entity.RawContent;
import com.ron.javainfohunter.processor.dto.ProcessedContentMessage;
import com.ron.javainfohunter.repository.NewsRepository;
import com.ron.javainfohunter.repository.RawContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionalStoreServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RawContentRepository rawContentRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    void storeProcessedContent_shouldCallUpdateStatusViaTransactionTemplate() {
        TransactionalStoreService service = new TransactionalStoreService(
                embeddingService, rawContentRepository, newsRepository, transactionTemplate);

        RawContent rawContent = RawContent.builder()
                .id(1L)
                .contentHash("hash1")
                .rawContent("content")
                .build();

        when(rawContentRepository.findByContentHash("hash1")).thenReturn(Optional.of(rawContent));
        when(embeddingService.embed(anyString())).thenReturn(new float[1024]);

        // Make TransactionTemplate.executeWithoutResult run the callback
        doAnswer(inv -> {
            org.springframework.transaction.support.TransactionCallbackWithoutResult callback = inv.getArgument(0);
            callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        ProcessedContentMessage message = new ProcessedContentMessage();
        message.setContentHash("hash1");
        message.setTitle("Test");

        // Trigger failure in newsRepository.save to exercise the error path
        when(newsRepository.save(any(News.class)))
                .thenThrow(new RuntimeException("Simulated failure"));

        assertThrows(RuntimeException.class,
                () -> service.storeProcessedContent(message, (news, msg) -> {}));

        // Verify TransactionTemplate was used for the status update (not @Transactional proxy)
        verify(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void constructor_shouldAcceptTransactionTemplate() {
        TransactionalStoreService service = new TransactionalStoreService(
                embeddingService, rawContentRepository, newsRepository, transactionTemplate);
        assertNotNull(service);
    }
}
