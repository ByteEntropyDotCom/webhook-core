## Webhook-Core
A high-performance, resilient, and secure Webhook delivery engine built with Spring Boot 3.2+, Java 21 Virtual Threads, and Spring WebFlux.

This microservice handles the registration of merchant webhooks and ensures the reliable delivery of system events with built-in cryptographic signing and automatic exponential backoff retries.

## 🌟 Key Features
* Virtual Thread Per Task: Utilizes Project Loom (Virtual Threads) to handle high-concurrency outgoing dispatches without the memory overhead of traditional platform threads.

* *MAC-SHA256 Security: Every outgoing request includes an X-ByteEntropy-Signature header, allowing receivers to verify data integrity and origin using a shared secret.

* Intelligent Retries: Built-in RetryService featuring Exponential Backoff, ensuring the system remains a good citizen when merchant servers are struggling.

* Reactive & Non-Blocking: Powered by WebClient for efficient, asynchronous I/O.

* Idempotency Ready: Payloads include unique eventId keys to prevent duplicate processing on the receiver's end.

## 🏗 Architecture
The system follows a non-blocking execution flow:

1. API Layer: Receives a registration or test trigger.

2. Service Layer: Hands off the task to a Virtual Thread.

3. Security Layer: Serializes the JSON and signs it with a merchant-specific secretKey.

4. Network Layer: Dispatches the request via WebClient.

5. Recovery Layer: If the destination is down, the event is rescheduled for retry with an increased delay.

## 🛠 Tech Stack
* Runtime: Java 21

* Framework: Spring Boot 3.2.x (WebFlux)

* Database: H2 (In-memory for development)

* Persistence: Spring Data JPA / Hibernate

* Concurrency: Java Virtual Threads (Project Loom)

## 🚦 Getting Started
Prerequisites

1. JDK 21 or higher
2. Maven 3.9+

### Configuration

Update the src/main/resources/application.properties to tune the retry logic:

```
Properties
fintech.webhook.retry.max-attempts=5
fintech.webhook.retry.initial-backoff-seconds=10
fintech.webhook.retry.max-backoff-seconds=3600
```

### Running the App

```
Bash
mvn clean install
mvn spring-boot:run
```

## 🧪 API Usage Examples
1. Register a Webhook

```
Bash
curl -X POST http://localhost:8085/api/v1/webhooks/registrations \
-H "Content-Type: application/json" \
-d '{
  "clientId": "merchant-001",
  "targetUrl": "https://webhook.site/your-uuid",
  "eventType": "PAYMENT_SUCCESS",
  "secretKey": "ultra-secret-key"
}'
```


2. Trigger a Test Dispatch

```
Bash
curl -X POST http://localhost:8085/api/v1/webhooks/registrations/{id}/test-dispatch
```

## 🔒 Security Verification
The receiver should verify the signature as follows:

1. Take the raw JSON body received.

2. Compute the HMAC-SHA256 hash using the secretKey.

3. Compare the result with the X-ByteEntropy-Signature header.

## 📈 Performance Notes
By enabling spring.threads.virtual.enabled=true, this service can theoretically handle thousands of concurrent outgoing requests on modest hardware, as Virtual Threads are unmounted from the carrier thread during blocking I/O operations (like waiting for a merchant's server to respond).