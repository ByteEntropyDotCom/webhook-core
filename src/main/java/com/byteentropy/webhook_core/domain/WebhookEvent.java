package com.byteentropy.webhook_core.domain;

import java.util.Map;

public record WebhookEvent(
    String eventId,
    String eventType,
    String targetUrl,
    Map<String, Object> payload,
    int attempt, // This creates the method event.attempt()
    String secretKey
) {}