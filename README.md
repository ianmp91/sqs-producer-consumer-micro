# Airlines-B Microservice (Producer/Consumer)

This project is a **Java 25** microservice built with **Spring Boot 4** and **Gradle (Groovy)**. It acts as a bidirectional messaging component within the distributed system, utilizing **AWS SQS** for asynchronous communication.

## 🚀 Tech Stack

* **Language:** Java 25
* **Framework:** Spring Boot 4
* **Build Tool:** Gradle (Groovy DSL)
* **Cloud Messaging:** Spring Cloud AWS SQS
* **Documentation:** SpringDoc OpenAPI (Swagger)
* **Internal Dependency:** `sqs-consumer-producer-lib` (Library A)

## 🔄 Architecture & Messaging Flow

This microservice functions as both a Producer and a Consumer using a Hybrid Encryption scheme (RSA + AES).

### 1. Outbound (Producer)

* **Trigger:** A `@Schedule` task (cron job) or a manual trigger via the Test `@RestController`.
* **Destination:** `cola-aws-sqs-1` (Retrieved from Config Server).
* **Process:**
1. Generates the payload.
2. Fetches **Airport-C's Public Key** via `ExternalConfigServer`.
3. Encrypts the payload using **AES + Public Key (Hybrid)**.
4. Publishes the `MessageDto` to the queue using `sqs-consumer-producer-lib`.


### 2. Inbound (Consumer)

* **Source:** `cola-aws-sqs-2` (Retrieved from Config Server).
* **Process:**
1. Listens for incoming messages.
2. Uses **Airlines-B's Private Key** to decrypt the payload.
3. Processes the response.


## 📦 Data Model

The messaging contract uses the following DTO:

```java
public record MessageDto(
    Map<String, String> metadata, // Timestamp, TraceId, Sender, etc.
    String encryptedPayload,      // Base64 encoded encrypted content
    String keyId,                 // Optional: Identifier for the key used
    String uniqueFlightId
) {}

```

## ⚙️ Configuration & Security

### Config Server Integration

The service uses `RestClient` within `ExternalConfigServer` to communicate with **Config-Server-D**. It fetches:

* **SQS Queue Names:**
* Outbound: `cola-aws-sqs-1`
* Inbound: `cola-aws-sqs-2`


* **Security Keys:**
* **Public Key (Airport-C):** Used for encrypting outgoing messages.


### Encryption Strategy

* **Sending:** Payload is encrypted using AES; the AES key is encrypted using Airport-C's RSA Public Key.
* **Receiving:** The service holds its own **RSA Private Key** to decrypt incoming messages from `cola-aws-sqs-2`.

## 🔌 API Documentation

This service includes **SpringDoc OpenAPI**.
Once the application is running, you can access the Swagger UI at:
`http://localhost:8080/swagger-ui.html` (port may vary based on configuration).

The API exposes endpoints primarily for:

* Testing the producer logic manually (`@RestController`).
* Health checks and metrics.

## 🛠️ Build & Run

```bash
make clean
make build
make bootJar
make bootRun
```