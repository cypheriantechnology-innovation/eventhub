package com.eventhub.unit.security;

import com.eventhub.infrastructure.security.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowRateLimiterUnitTest {

    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowRateLimiter();
        rateLimiter.reset();
    }

    @Test
    @DisplayName("Requisições dentro do limite configurado devem ser permitidas")
    void shouldAllowRequestsUnderThreshold() {
        String key = "client-ip-1";
        int maxRequests = 5;
        long windowMs = 10_000L;

        for (int i = 0; i < maxRequests; i++) {
            var result = rateLimiter.tryAcquire(key, maxRequests, windowMs);
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.retryAfterSeconds()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Requisição excedendo o limite deve ser bloqueada com tempo de espera positivo")
    void shouldBlockWhenExceedingLimit() {
        String key = "client-ip-2";
        int maxRequests = 3;
        long windowMs = 10_000L;

        for (int i = 0; i < maxRequests; i++) {
            assertThat(rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()).isTrue();
        }

        var blockedResult = rateLimiter.tryAcquire(key, maxRequests, windowMs);
        assertThat(blockedResult.isAllowed()).isFalse();
        assertThat(blockedResult.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
        assertThat(blockedResult.retryAfterSeconds()).isLessThanOrEqualTo(11);
    }

    @Test
    @DisplayName("Chaves diferentes devem manter contadores e janelas independentes")
    void shouldIsolateDifferentKeys() {
        int maxRequests = 2;
        long windowMs = 10_000L;

        assertThat(rateLimiter.tryAcquire("key-A", maxRequests, windowMs).isAllowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("key-A", maxRequests, windowMs).isAllowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("key-A", maxRequests, windowMs).isAllowed()).isFalse();

        // key-B não deve ser afetada pelo bloqueio de key-A
        assertThat(rateLimiter.tryAcquire("key-B", maxRequests, windowMs).isAllowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("key-B", maxRequests, windowMs).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Reset deve limpar todo o estado interno e permitir novas requisições")
    void shouldResetInternalState() {
        String key = "reset-client";
        int maxRequests = 1;
        long windowMs = 10_000L;

        assertThat(rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()).isTrue();
        assertThat(rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()).isFalse();

        rateLimiter.reset();

        assertThat(rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Janela expirada deve purgar timestamps antigos e permitir novas requisições")
    void shouldEvictExpiredTimestamps() throws InterruptedException {
        String key = "evict-client";
        int maxRequests = 1;
        long windowMs = 50L; // Janela curta de 50ms

        assertThat(rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()).isTrue();
        assertThat(rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()).isFalse();

        Thread.sleep(60);

        var result = rateLimiter.tryAcquire(key, maxRequests, windowMs);
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.retryAfterSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve garantir thread-safety sob concorrência multi-thread")
    void shouldBeThreadSafeUnderConcurrency() throws InterruptedException {
        String key = "concurrent-client";
        int maxRequests = 50;
        long windowMs = 60_000L;
        int totalThreads = 100;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    if (rateLimiter.tryAcquire(key, maxRequests, windowMs).isAllowed()) {
                        allowedCount.incrementAndGet();
                    } else {
                        rejectedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(allowedCount.get()).isEqualTo(maxRequests);
        assertThat(rejectedCount.get()).isEqualTo(totalThreads - maxRequests);
    }
}
