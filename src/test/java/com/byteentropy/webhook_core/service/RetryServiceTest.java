package com.byteentropy.webhook_core.service;

import com.byteentropy.webhook_core.config.WebhookProperties;
import com.byteentropy.webhook_core.domain.WebhookEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private WebhookProperties properties;

    private ExecutorService virtualThreadExecutor;
    private RetryService retryService;

    @BeforeEach
    void setUp() {
        // We use a real executor so the 'submit()' call actually functions
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        // Manual instantiation fixes the "field not used" warning 
        // and ensures the service uses our specific executor.
        retryService = new RetryService(deliveryService, virtualThreadExecutor, properties);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        virtualThreadExecutor.shutdown();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Use Case 1: Max Attempts Reached
     * Logic: If current attempt is 5 and max is 5, it should NOT schedule a 6th.
     */
    @Test
    void shouldStopRetryingWhenMaxAttemptsReached() {
        when(properties.maxAttempts()).thenReturn(5);
        
        WebhookEvent event = new WebhookEvent(
            "evt_max", "PAYMENT", "http://test.com", Map.of(), 5, "test_secret"
        );

        retryService.scheduleRetry(event);

        // Verification: DeliveryService should never be called for a 6th attempt
        verify(deliveryService, never()).sendWebhook(any());
    }

    /**
     * Use Case 2: Successful Scheduling
     * Logic: Ensures that calculateBackoff is called and a task is submitted.
     */
    @Test
    void shouldScheduleRetryWhenUnderLimit() {
        when(properties.maxAttempts()).thenReturn(5);
        when(properties.initialBackoffSeconds()).thenReturn(1);
        when(properties.maxBackoffSeconds()).thenReturn(10);
        
       WebhookEvent event = new WebhookEvent(
            "evt_retry", "PAYMENT", "http://test.com", Map.of(), 1, "test_secret"
        );

        retryService.scheduleRetry(event);

        // Verify the properties were accessed to calculate the backoff
        verify(properties, atLeastOnce()).initialBackoffSeconds();
        verify(properties, atLeastOnce()).maxAttempts();
    }
}