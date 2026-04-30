package com.byteentropy.webhook_core.repository;

import com.byteentropy.webhook_core.domain.WebhookRegistration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<WebhookRegistration, UUID> {
    
    // Custom finder to get all webhooks for a specific merchant/client
    List<WebhookRegistration> findByClientId(String clientId);
    
    // Custom finder to find where to send a specific event type (e.g., PAYMENT_SETTLED)
    List<WebhookRegistration> findByEventType(String eventType);
}