package com.byteentropy.webhook_core.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class WebhookRegistration {
    @Id
    private UUID id;
    private String clientId;
    private String targetUrl;
    private String eventType;
    private String secretKey; // For HMAC signing

    public WebhookRegistration() {}

    public WebhookRegistration(UUID id, String clientId, String targetUrl, String eventType, String secretKey) {
        this.id = id;
        this.clientId = clientId;
        this.targetUrl = targetUrl;
        this.eventType = eventType;
        this.secretKey = secretKey;
    }

    // Standard getters
    public UUID getId() { return id; }
    public String getClientId() { return clientId; }
    public String getTargetUrl() { return targetUrl; }
    public String getEventType() { return eventType; }
    public String getSecretKey() { return secretKey; }
}