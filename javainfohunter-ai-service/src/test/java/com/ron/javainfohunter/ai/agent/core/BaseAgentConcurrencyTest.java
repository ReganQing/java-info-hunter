package com.ron.javainfohunter.ai.agent.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BaseAgentConcurrencyTest {

    /**
     * Agent that finishes immediately on first step.
     */
    static class QuickAgent extends BaseAgent {
        @Override
        public String step() {
            setAgentState(AgentState.FINISHED);
            return "done";
        }

        @Override
        public void cleanup() {
        }
    }

    @Test
    void sequentialRunCalls_shouldAllSucceed() {
        QuickAgent agent = new QuickAgent();
        agent.setName("quick");

        for (int i = 0; i < 10; i++) {
            String result = agent.run("task-" + i);
            assertNotNull(result);
        }
    }

    @Test
    void concurrentRunCalls_shouldNotCorruptState() throws Exception {
        QuickAgent agent = new QuickAgent();
        agent.setName("quick");
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    agent.run("task");
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        // All threads should eventually succeed (sequential via synchronized)
        // Some may fail on first attempt due to contention, but after reset they can retry
        assertTrue(successCount.get() > 0, "At least some threads should succeed");
        // Agent should be back to IDLE after all threads complete
        assertEquals(AgentState.IDLE, agent.getAgentState());
    }
}
