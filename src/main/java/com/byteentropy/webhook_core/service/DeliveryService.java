package com.byteentropy.webhook_core.service;

import com.byteentropy.webhook_core.domain.WebhookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private final WebClient webClient;
    private final ExecutorService executor;
    private final RetryService retryService;
    private final ObjectMapper objectMapper;

    public DeliveryService(WebClient.Builder webClientBuilder, 
                           ExecutorService executor, 
                           @Lazy RetryService retryService,
                           ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.executor = executor;
        this.retryService = retryService;
        this.objectMapper = objectMapper;
    }

    public void sendWebhook(WebhookEvent event) {
        executor.submit(() -> {
            try {
                log.info("Attempt #{} - Dispatching event {} to {}", 
                        event.attempt(), event.eventId(), event.targetUrl());

                // 1. Serialize payload to JSON
                String jsonBody = objectMapper.writeValueAsString(event.payload());

                // 2. Generate HMAC Signature using the merchant's unique secretKey
                String signature = generateHmac(jsonBody, event.secretKey());

                // 3. POST with security headers
                webClient.post()
                        .uri(event.targetUrl())
                        .header("Content-Type", "application/json")
                        .header("X-ByteEntropy-Signature", signature)
                        .bodyValue(jsonBody)
                        .retrieve()
                        .toBodilessEntity()
                        .doOnSuccess(response -> 
                            log.info("Successfully delivered event {}", event.eventId()))
                        .doOnError(error -> {
                            log.error("Failed to deliver event {}: {}", event.eventId(), error.getMessage());
                            retryService.scheduleRetry(event);
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("Critical error in delivery for event {}: {}", event.eventId(), e.getMessage());
                retryService.scheduleRetry(event);
            }
        });
    }

    private String generateHmac(String data, String key) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }
}