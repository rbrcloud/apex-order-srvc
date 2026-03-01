# Apex Order Service

Order management microservice for placing and tracking buy/sell stock orders. Part of the **Apex Tracker** ecosystem; persists orders to PostgreSQL and publishes order events to Apache Kafka for downstream consumers.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Build](#build)
- [Run](#run)
- [Docker](#docker)
- [API Reference](#api-reference)
- [Events](#events)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Local Development](#local-development)

---

## Overview

This service exposes a REST API to place equity orders (buy/sell). Each accepted order is:

1. Persisted in PostgreSQL in the `orders` schema.
2. Published as an event to Kafka on the `order.placed.event` topic.

Downstream services (e.g. portfolio, analytics) consume these events. The service can be configured to call a market-data service for validation or pricing if needed.

---

## Tech Stack

| Technology        | Version / Notes                    |
|-------------------|------------------------------------|
| **Java**          | 21                                 |
| **Spring Boot**   | 3.4.2 (Web, Data JPA)              |
| **PostgreSQL**    | Persistence (schema: `orders`)     |
| **Apache Kafka**  | Event publishing                   |
| **Flyway**        | Optional DB migrations             |
| **Lombok**        | 1.18.30                            |
| **H2**            | Test scope only (in-memory tests)  |

The project depends on **apex-shared-lib** (e.g. shared DTOs/enums) from JitPack.

---

## Prerequisites

- **JDK 21**
- **Maven 3.6+** (or use `./mvnw`)
- **PostgreSQL** (e.g. host `apex-postgres`, port `5432`, database and schema `orders` created)
- **Kafka** (e.g. broker `apex-kafka:9092`)

For running tests only, no Docker or external DB/Kafka is required (H2 and disabled Kafka autoconfig are used).

---

## Configuration

Main settings live in `src/main/resources/application.yml`. Key options:

| Setting | Default / Example | Description |
|--------|--------------------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://${DB_HOST}/${DB_NAME}?currentSchema=orders&sslmode=require&channel_binding=require` | PostgreSQL JDBC URL; use placeholders for env-based config. |
| `spring.datasource.username` | `${DB_USERNAME}` | DB user. |
| `spring.datasource.password` | `${DB_PASSWORD}` | DB password. |
| `spring.datasource.hikari.maximum-pool-size` | `5` | Connection pool size. |
| `spring.flyway.enabled` | `true` | Enable Flyway migrations. |
| `spring.flyway.default-schema` | `orders` | Schema for migrations. |
| `spring.jpa.hibernate.ddl-auto` | `update` | Create/update tables from entities. |
| `spring.kafka.bootstrap-servers` | `apex-kafka:9092` | Kafka broker list. |
| `server.port` | `8080` | HTTP server port. |
| `market-data-srvc.url` | `http://market-data-srvc:8080` | Base URL for market data service (if used). |

**Environment variables** (recommended for non-default setups):

- `DB_HOST` – PostgreSQL host (and port if in URL).
- `DB_NAME` – Database name.
- `DB_USERNAME` – Database user.
- `DB_PASSWORD` – Database password.

Override any property via environment variables or a profile-specific file (e.g. `application-local.yml`) for local development.

---

## Build

```bash
./mvnw clean package
```

Or with system Maven:

```bash
mvn clean package
```

This produces the runnable JAR under `target/` (and a `-lib` classifier JAR as configured in the POM). To skip tests:

```bash
mvn clean package -DskipTests
```

---

## Run

**Using Maven:**

```bash
./mvnw spring-boot:run
```

**Using the packaged JAR:**

```bash
java -jar target/apex-order-srvc-0.0.1-SNAPSHOT.jar
```

The service listens on **port 8080** by default. Ensure PostgreSQL and Kafka are reachable (or use a local profile that points to your local instances).

---

## Docker

A multi-stage Dockerfile is provided: build with Maven and run with Eclipse Temurin 21 JRE.

**Build image:**

```bash
docker build -t apex-order-srvc .
```

**Run container** (example; set env vars or use compose):

```bash
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal:5432 \
  -e DB_NAME=apex_tracker \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=secret \
  apex-order-srvc
```

The Dockerfile excludes tests during build for speed; the `.dockerignore` omits `.env`, `.git`, `target/`, and `*.md` from the build context.

---

## API Reference

Base path: `/api/v1/order`.

### Place order

**POST** `/api/v1/order`

Accepts an order payload, persists it, and publishes an `OrderPlacedEvent` to Kafka. Responds with `202 Accepted` and no response body.

**Request body (JSON):**

```json
{
  "userId": 1,
  "ticker": "AAPL",
  "quantity": 10,
  "price": 150.00,
  "orderSide": "BUY"
}
```

| Field      | Type    | Required | Description                    |
|-----------|---------|----------|--------------------------------|
| `userId`  | long    | Yes      | User identifier.               |
| `ticker`  | string  | Yes      | Stock symbol (e.g. AAPL).      |
| `quantity`| integer | Yes      | Number of shares.              |
| `price`   | number  | Yes      | Price per share.               |
| `orderSide` | string | Yes      | `BUY` or `SELL`.               |

**Response:** `202 Accepted` (empty body).

**Notes:** The service sets initial order status (e.g. `SUBMITTED`) and assigns `orderId` and `placedAt` when publishing the event.

---

## Events

After an order is saved, the service publishes to the Kafka topic **`order.placed.event`**.

- **Key:** ticker (string).
- **Value:** JSON-serialized event (e.g. `OrderPlacedEvent`) including `orderId` and `placedAt` set by the service.
- **Header:** `__TypeId__: orderPlacedEvent` (for consumer deserialization with type mapping `orderPlacedEvent:com.rbrcloud.ordersrvc.dto.OrderPlacedEvent`).

Producers use `JsonSerializer`; ensure consumers use a compatible deserializer and the same type-id mapping if they rely on type headers.

---

## Testing

```bash
./mvnw test
```

Tests use the **test** profile:

- **H2** in-memory database (PostgreSQL compatibility mode), with `init-schema.sql` creating the `orders` schema.
- **Kafka autoconfiguration disabled** so unit tests don’t require a running broker.
- `KafkaTemplate` is mocked in `OrderSrvcApplicationTests` so the context loads without Kafka.

Relevant files:

- `src/test/resources/application-test.yml` – test datasource and Kafka exclusion.
- `src/test/resources/init-schema.sql` – schema setup for H2.

---

## Project Structure

```
src/main/java/com/rbrcloud/ordersrvc/
├── OrderSrvcApplication.java       # Application entry point
├── config/
│   └── KafkaProducerConfig.java   # Kafka producer factory & JsonSerializer type mapping
├── controller/
│   └── OrderController.java       # REST API (POST /api/v1/order)
├── entity/
│   └── Order.java                 # JPA entity (orders.order table)
├── repository/
│   └── OrderRepository.java       # Spring Data JPA repository
└── service/
    ├── KafkaProducerService.java  # Generic Kafka send helper
    └── OrderService.java          # Order persistence and OrderPlacedEvent publishing
```

DTOs and enums (e.g. `OrderPlacedEvent`, `OrderSide`, `OrderStatus`) may live in this package or in the **apex-shared-lib** dependency. The Kafka type mapping references `com.rbrcloud.ordersrvc.dto.OrderPlacedEvent`.

---

## Local Development

1. **Database:** Run PostgreSQL (e.g. Docker) and create database and schema `orders`, or let Hibernate `ddl-auto` create tables.
2. **Kafka:** Run Kafka (e.g. Docker) and ensure `bootstrap-servers` in config or env points to it.
3. **Profile:** Use an `application-local.yml` or env vars to set `spring.datasource.url` (and optionally `spring.kafka.bootstrap-servers`) to your local host/ports (e.g. `localhost:5432`, `localhost:9092`).
4. **Secrets:** Keep credentials in environment variables or a local config file that is not committed (e.g. `.env` is in `.gitignore`).

For a quick run without external services, use the **test** profile and run tests only; the main application still expects PostgreSQL and Kafka when run with the default profile.
