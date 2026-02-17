# apex-order-srvc

Order management service for buying and selling stocks. Part of the Apex tracker ecosystem.

## Tech stack

- **Java 21**
- **Spring Boot 3.4.2** (Web, Data JPA)
- **PostgreSQL** (persistence)
- **Apache Kafka** (event publishing)
- **Lombok**

## Prerequisites

- JDK 21
- Maven 3.6+
- PostgreSQL (e.g. `apex-postgres:5432`, schema `orders`)
- Kafka (e.g. `apex-kafka:9092`)

## Build

```bash
mvn clean package
```

## Run

```bash
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/apex-order-srvc-0.0.1-SNAPSHOT.jar
```

The service listens on **port 8080** by default.

## Configuration

Key settings in `src/main/resources/application.yml`:

| Setting | Default | Description |
|--------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://apex-postgres:5432/apex_tracker?currentSchema=orders` | PostgreSQL connection |
| `spring.kafka.bootstrap-servers` | `apex-kafka:9092` | Kafka brokers |
| `server.port` | `8080` | HTTP port |
| `market-data-srvc.url` | `http://market-data-srvc:8080` | Market data service base URL |

Override via environment variables or a separate profile (e.g. `application-local.yml`) for local development.

## API

### Place order

**POST** `/api/v1/order`

Accepts an order and persists it, then publishes an `OrderPlacedEvent` to Kafka.

**Request body** (JSON):

```json
{
  "userId": 1,
  "ticker": "AAPL",
  "quantity": 10,
  "price": 150.00,
  "orderSide": "BUY"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `userId` | long | User identifier |
| `ticker` | string | Stock symbol |
| `quantity` | integer | Number of shares |
| `price` | decimal | Price per share |
| `orderSide` | string | `BUY` or `SELL` |

**Response:** `202 Accepted` (no body).

## Events

After an order is saved, the service publishes to the **`order.placed.event`** topic with a JSON payload (and `__TypeId__: orderPlacedEvent` header). The event includes `orderId` and `placedAt` set by the service.

## Tests

```bash
mvn test
```

Unit tests use an in-memory H2 database (no Docker required). See `application-test.yml` for test configuration.

## Project layout

```
src/main/java/com/rbrcloud/ordersrvc/
├── OrderSrvcApplication.java    # Entry point
├── controller/
│   └── OrderController.java     # REST API
├── dto/
│   └── OrderPlacedEvent.java    # Request/event DTO
├── entity/
│   └── Order.java               # JPA entity
├── enums/
│   ├── OrderSide.java
│   └── OrderStatus.java
├── repository/
│   └── OrderRepository.java
└── service/
    └── OrderService.java        # Order processing & Kafka publish
```
