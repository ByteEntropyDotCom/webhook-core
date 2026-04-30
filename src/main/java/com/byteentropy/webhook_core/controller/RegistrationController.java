package com.byteentropy.webhook_core.controller;

import com.byteentropy.webhook_core.domain.WebhookEvent;
import com.byteentropy.webhook_core.domain.WebhookRegistration;
import com.byteentropy.webhook_core.repository.RegistrationRepository;
import com.byteentropy.webhook_core.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/registrations")
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    
    private final RegistrationRepository repository;
    private final DeliveryService deliveryService;

    public RegistrationController(RegistrationRepository repository, DeliveryService deliveryService) {
        this.repository = repository;
        this.deliveryService = deliveryService;
    }

    /**
     * Data Carrier for the Registration Request
     */
    public record RegisterRequest(
        String clientId,
        String targetUrl,
        String eventType,
        String secretKey
    ) {}

    @PostMapping
    public ResponseEntity<WebhookRegistration> register(@RequestBody RegisterRequest request) {
        log.info("API: Registering webhook for client {} on event {}", request.clientId(), request.eventType());

        WebhookRegistration registration = new WebhookRegistration(
            UUID.randomUUID(),
            request.clientId(),
            request.targetUrl(),
            request.eventType(),
            request.secretKey()
        );

        WebhookRegistration saved = repository.save(registration);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<List<WebhookRegistration>> getByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(repository.findByClientId(clientId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * TEST TRIGGER: Simulates an internal system event.
     * Note: We now include the eventId INSIDE the payload for merchant idempotency.
     */
    @PostMapping("/{id}/test-dispatch")
    public ResponseEntity<Map<String, String>> testDispatch(@PathVariable UUID id) {
        WebhookRegistration reg = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        log.info("API: Manual test trigger for registration ID: {}", id);

        String uniqueEventId = UUID.randomUUID().toString();

        WebhookEvent testEvent = new WebhookEvent(
            uniqueEventId,
            reg.getEventType(),
            reg.getTargetUrl(),
            Map.of(
                "eventId", uniqueEventId, // Merchant uses this to prevent double-processing
                "amount", 1250.50,
                "currency", "USD",
                "status", "SETTLED",
                "timestamp", System.currentTimeMillis()
            ),
            1, // Starting with attempt 1
            reg.getSecretKey()
        );

        // Dispatches via Virtual Thread immediately
        deliveryService.sendWebhook(testEvent);

        return ResponseEntity.ok(Map.of(
            "message", "Dispatch initiated",
            "eventId", uniqueEventId,
            "target", reg.getTargetUrl(),
            "status", "Check application logs for delivery/retry progress"
        ));
    }
}