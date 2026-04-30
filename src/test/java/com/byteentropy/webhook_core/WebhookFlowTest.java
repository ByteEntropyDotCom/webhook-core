package com.byteentropy.webhook_core;

import com.byteentropy.webhook_core.controller.RegistrationController;
import com.byteentropy.webhook_core.domain.WebhookRegistration;
import com.byteentropy.webhook_core.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebhookFlowTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RegistrationRepository repository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void testRegistrationAndDispatchFlow() {
        // 1. Test POST /registrations
        RegistrationController.RegisterRequest request = new RegistrationController.RegisterRequest(
                "client-1", "http://test.com", "PAYMENT", "secret"
        );

        WebhookRegistration saved = webTestClient.post()
                .uri("/api/v1/webhooks/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WebhookRegistration.class)
                .returnResult().getResponseBody();

        assertThat(saved).isNotNull();
        assertThat(saved.getClientId()).isEqualTo("client-1");

        // 2. Test GET /registrations/{clientId}
        webTestClient.get()
                .uri("/api/v1/webhooks/registrations/client-1")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(WebhookRegistration.class)
                .hasSize(1);

        // 3. Test POST /test-dispatch (The Asynchronous Trigger)
        webTestClient.post()
                .uri("/api/v1/webhooks/registrations/" + saved.getId() + "/test-dispatch")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Dispatch initiated");
    }
}