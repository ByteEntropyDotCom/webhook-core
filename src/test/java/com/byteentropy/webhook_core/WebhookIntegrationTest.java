package com.byteentropy.webhook_core;

import com.byteentropy.webhook_core.controller.RegistrationController;
import com.byteentropy.webhook_core.domain.WebhookRegistration;
import com.byteentropy.webhook_core.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebhookIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RegistrationRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void testFullWebhookLifecycle() {
        // 1. USE CASE: Register a Webhook
        RegistrationController.RegisterRequest request = new RegistrationController.RegisterRequest(
                "test-client",
                "http://localhost:9999/callback",
                "PAYMENT_SUCCESS",
                "secret-123"
        );

        ResponseEntity<WebhookRegistration> regResponse = restTemplate.postForEntity(
                "/api/v1/webhooks/registrations", 
                request, 
                WebhookRegistration.class
        );

        assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID registrationId = regResponse.getBody().getId();
        assertThat(registrationId).isNotNull();

        // 2. USE CASE: Retrieve Registration
        ResponseEntity<WebhookRegistration[]> listResponse = restTemplate.getForEntity(
                "/api/v1/webhooks/registrations/test-client", 
                WebhookRegistration[].class
        );
        assertThat(listResponse.getBody()).hasSize(1);

        // 3. USE CASE: Trigger Test Dispatch
        // This confirms the controller can handle the trigger request
        ResponseEntity<Object> triggerResponse = restTemplate.postForEntity(
                "/api/v1/webhooks/registrations/" + registrationId + "/test-dispatch",
                null,
                Object.class
        );
        
        assertThat(triggerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testDeleteWebhook() {
        // Setup: Save a registration
        WebhookRegistration reg = new WebhookRegistration(
                UUID.randomUUID(), "client-to-delete", "http://url.com", "EVENT", "secret"
        );
        repository.save(reg);

        // Action: Delete
        restTemplate.delete("/api/v1/webhooks/registrations/" + reg.getId());

        // Verify: Not found in DB
        assertThat(repository.findById(reg.getId())).isEmpty();
    }
}