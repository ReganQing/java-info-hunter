package com.ron.javainfohunter.processor.service.impl;

import com.ron.javainfohunter.processor.agent.AgentProcessor;
import com.ron.javainfohunter.processor.config.ProcessorProperties;
import com.ron.javainfohunter.processor.dto.AgentResult;
import com.ron.javainfohunter.processor.service.ResultAggregator;
import com.ron.javainfohunter.dto.RawContentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ContentRoutingServiceImplTest {

    @Mock
    private ResultAggregator resultAggregator;

    @Mock
    private AgentProcessor mockProcessor;

    private ProcessorProperties properties;
    private ContentRoutingServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        properties = new ProcessorProperties();
        service = new ContentRoutingServiceImpl(properties, List.of(), resultAggregator);

        // Set mock processor agent type via reflection on the mock
    }

    @Test
    void shutdown_shouldBeCalledOnDestroy() throws Exception {
        ExecutorService executor = getExecutorViaReflection(service);
        assertNotNull(executor);
        assertFalse(executor.isShutdown());

        // Invoke @PreDestroy method
        service.shutdown();

        assertTrue(executor.isShutdown());
    }

    private ExecutorService getExecutorViaReflection(ContentRoutingServiceImpl target) throws Exception {
        Field field = ContentRoutingServiceImpl.class.getDeclaredField("executor");
        field.setAccessible(true);
        return (ExecutorService) field.get(target);
    }
}
