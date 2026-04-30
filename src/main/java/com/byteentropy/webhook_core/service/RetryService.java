package com.byteentropy.webhook_core.service;

import com.byteentropy.webhook_core.config.WebhookProperties;
import com.byteentropy.webhook_core.domain.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RetryService {
    private static final Logger log = LoggerFactory.getLogger(RetryService.class);
    
    private final DeliveryService deliveryService;
    private final ExecutorService virtualThreadExecutor;
    private final WebhookProperties properties;

    public RetryService(DeliveryService deliveryService, 
                        ExecutorService virtualThreadExecutor,
                        WebhookProperties properties) {
        this.deliveryService = deliveryService;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.properties = properties;
    }

    public void scheduleRetry(WebhookEvent event) {
        // CHANGED: event.attemptCount() -> event.attempt()
        int nextAttempt = event.attempt() + 1;

        if (nextAttempt > properties.maxAttempts()) {
            log.error("CRITICAL: Max retry attempts ({}) reached for event {}. Giving up.", 
                    properties.maxAttempts(), event.eventId());
            return;
        }

        long backoff = calculateBackoff(nextAttempt);
        
        log.info("Scheduling retry #{} for event {} in {} seconds", 
                nextAttempt, event.eventId(), backoff);

        virtualThreadExecutor.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(backoff);
                
                // Constructing the new event with incremented attempt
                WebhookEvent retryEvent = new WebhookEvent(
                    event.eventId(),
                    event.eventType(),
                    event.targetUrl(),
                    event.payload(),
                    nextAttempt,
                    event.secretKey()
                );

                deliveryService.sendWebhook(retryEvent);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry interrupted for event {}", event.eventId());
            }
        });
    }

    private long calculateBackoff(int attempt) {
        // Standard exponential backoff: (2^attempt) * initialDelay
        long exponentialBackoff = (long) Math.pow(2, attempt) * properties.initialBackoffSeconds();
        return Math.min(exponentialBackoff, properties.maxBackoffSeconds());
    }
}