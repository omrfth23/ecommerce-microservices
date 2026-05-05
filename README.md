# 🛒 E-Commerce Microservices Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=for-the-badge&logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-231F20?style=for-the-badge&logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?style=for-the-badge&logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker)

**Production-ready microservices architecture built with Spring Boot 3.2, Apache Kafka, and hybrid database strategy.**

[Architecture](#-architecture) · [Services](#-services) · [Tech Stack](#-tech-stack) · [Getting Started](#-getting-started) · [API Reference](#-api-reference) · [Design Decisions](#-design-decisions)

</div>

---

## 📌 Project Overview

This project demonstrates a **cloud-native e-commerce platform** built with microservices principles. It showcases real-world patterns including event-driven communication, service discovery, API gateway routing, and polyglot persistence — all containerized with Docker Compose.

The system handles the complete order lifecycle: from product browsing to order placement, automated payment processing, and customer notification — fully decoupled through Apache Kafka events.

---

## 🏗 Architecture

### High-Level System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client / Frontend                        │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTP
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway  :8080                            │
│            Spring Cloud Gateway + Route Filtering                │
│         CORS · Circuit Breaker · Request/Response Logging        │
└──────┬──────────────────┬──────────────────┬────────────────────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Product    │  │    Order     │  │   Payment    │
│   Service   │  │   Service    │  │   Service    │
│   :8081      │  │   :8082      │  │   :8083      │
│              │  │              │  │              │
│   MongoDB    │  │  PostgreSQL  │  │  PostgreSQL  │
│ products_db  │  │  orders_db   │  │ payments_db  │
└──────────────┘  └──────┬───────┘  └──────┬───────┘
                         │                  │
                         │  Kafka Producer  │  Kafka Consumer
                         ▼                  ▼
              ┌─────────────────────────────────────┐
              │           Apache Kafka               │
              │                                      │
              │  [order-placed]  [payment-completed] │
              └──────────────────────┬──────────────┘
                                     │ Kafka Consumer
                                     ▼
                          ┌──────────────────────┐
                          │  Notification Service │
                          │        :8084          │
                          │  Email / SMS / Push   │
                          └──────────────────────┘

              ┌─────────────────────────────────┐
              │    Service Discovery  :8761      │
              │      Netflix Eureka Server       │
              │  All services register here      │
              └─────────────────────────────────┘
```

### Event-Driven Order Flow

```
User places order
      │
      ▼
[Order Service] ──── saves to PostgreSQL
      │
      │ publishes: order-placed event
      ▼
[Apache Kafka] ──── topic: order-placed
      │
      │ consumes: OrderPlacedEvent
      ▼
[Payment Service] ──── processes payment
      │               saves to PostgreSQL
      │
      │ publishes: payment-completed event
      ▼
[Apache Kafka] ──── topic: payment-completed
      │
      │ consumes: PaymentCompletedEvent
      ▼
[Notification Service] ──── logs & notifies customer
```

---

## 🔧 Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| **service-discovery** | 8761 | — | Eureka server, service registry |
| **api-gateway** | 8080 | — | Routing, CORS, Circuit Breaker, fallback |
| **product-service** | 8081 | MongoDB | Product CRUD, stock management |
| **order-service** | 8082 | PostgreSQL | Order lifecycle, Kafka producer |
| **payment-service** | 8083 | PostgreSQL | Payment processing, Kafka consumer & producer |
| **notification-service** | 8084 | — | Event consumer, customer notifications |

---

## 💻 Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language (LTS, Virtual Threads ready) |
| Spring Boot | 3.2.3 | Application framework |
| Spring Cloud Gateway | 2023.0.0 | API Gateway, routing |
| Spring Cloud Netflix Eureka | 2023.0.0 | Service discovery & registration |
| Spring Data JPA | 3.2.3 | ORM for relational data |
| Spring Data MongoDB | 3.2.3 | ODM for document data |
| Spring Kafka | 3.1.x | Kafka producer/consumer integration |
| Lombok | latest | Boilerplate reduction |
| Bean Validation | 3.x | Request validation |

### Infrastructure
| Technology | Version | Purpose |
|---|---|---|
| Apache Kafka | 7.5.0 | Async event streaming |
| Zookeeper | 7.5.0 | Kafka cluster coordination |
| PostgreSQL | 16 | Relational data (orders, payments) |
| MongoDB | 7.0 | Document store (products) |
| Docker Compose | — | Local orchestration |

---



## 🗄 Database Design

### Why Hybrid Persistence?

This project intentionally uses **two different databases** to demonstrate polyglot persistence — choosing the right tool for each domain:

**MongoDB → Product Service**
- Products have flexible, nested attributes (specifications, variants, images)
- Schema-less design allows adding new product types without migrations
- Rich query support for category filtering and text search

**PostgreSQL → Order & Payment Services**
- Orders and payments require **ACID transactions** — money cannot be lost
- Relational integrity between order status and payment records
- JPA auditing with `@PrePersist` / `@PreUpdate` lifecycle hooks

### PostgreSQL Schema

```sql
-- orders_db
CREATE TABLE orders (
    id            VARCHAR(36) PRIMARY KEY,
    customer_id   VARCHAR(36) NOT NULL,
    product_id    VARCHAR(36) NOT NULL,
    quantity      INTEGER     NOT NULL,
    total_amount  NUMERIC     NOT NULL,
    status        VARCHAR(30) NOT NULL,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

-- payments_db
CREATE TABLE payments (
    id           VARCHAR(36) PRIMARY KEY,
    order_id     VARCHAR(36) NOT NULL,
    customer_id  VARCHAR(36) NOT NULL,
    amount       NUMERIC     NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMP
);
```

### MongoDB Document

```json
{
  "_id": "ObjectId",
  "name": "Laptop Pro 15",
  "description": "High performance laptop",
  "price": 15000.00,
  "stockQuantity": 50,
  "category": "Electronics",
  "active": true
}
```

---

## 📨 Kafka Event Schema

### `order-placed` topic

```json
{
  "orderId":     "550e8400-e29b-41d4-a716-446655440000",
  "customerId":  "cust-001",
  "productId":   "prod-abc123",
  "quantity":    2,
  "totalAmount": 30000.00
}
```

### `payment-completed` topic

```json
{
  "paymentId":  "pay-xyz789",
  "orderId":    "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "cust-001",
  "amount":     30000.00,
  "status":     "COMPLETED"
}
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose

### 1 — Clone the repository

```bash
git clone https://github.com/yourusername/ecommerce-microservices.git
cd ecommerce-microservices
```

### 2 — Start infrastructure

```bash
docker-compose up -d
```

This starts:
- Apache Kafka on `localhost:9092`
- Zookeeper on `localhost:2181`
- PostgreSQL on `localhost:5432` (creates `orders_db` and `payments_db` automatically)
- MongoDB on `localhost:27017`

Verify containers are healthy:

```bash
docker-compose ps
```

### 3 — Build all modules

```bash
mvn clean install -DskipTests
```

### 4 — Start services in order

Open a separate terminal for each service:

```bash
# Terminal 1 — Service Discovery (start first, others depend on it)
cd service-discovery && mvn spring-boot:run

# Terminal 2 — API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 3 — Product Service
cd product-service && mvn spring-boot:run

# Terminal 4 — Order Service
cd order-service && mvn spring-boot:run

# Terminal 5 — Payment Service
cd payment-service && mvn spring-boot:run

# Terminal 6 — Notification Service
cd notification-service && mvn spring-boot:run
```

### 5 — Verify

| URL | Description |
|---|---|
| `http://localhost:8761` | Eureka dashboard — all services should appear here |
| `http://localhost:8080/actuator` | Gateway health & routes |

---

## 📡 API Reference

All requests go through the API Gateway on port `8080`.

### Product Service

```bash
# Create product
POST http://localhost:8080/api/products
Content-Type: application/json

{
  "name": "Laptop Pro 15",
  "description": "High performance laptop",
  "price": 15000.00,
  "stockQuantity": 50,
  "category": "Electronics"
}

# Get all products
GET http://localhost:8080/api/products

# Get product by ID
GET http://localhost:8080/api/products/{id}

# Check stock
GET http://localhost:8080/api/products/{id}/stock?quantity=2
```

### Order Service

```bash
# Place order (triggers full payment & notification flow)
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "customerId": "cust-001",
  "productId": "your-product-id",
  "quantity": 1,
  "totalAmount": 15000.00
}

# Get order by ID
GET http://localhost:8080/api/orders/{id}

# Get orders by customer
GET http://localhost:8080/api/orders/customer/{customerId}
```

### Payment Service

```bash
# Get payments by order
GET http://localhost:8080/api/payments/order/{orderId}

# Get payments by customer
GET http://localhost:8080/api/payments/customer/{customerId}
```

---

## 🎯 Design Decisions

### 1. Event-Driven Architecture over REST Chaining

Instead of Order Service calling Payment Service directly via HTTP, an `OrderPlacedEvent` is published to Kafka. This provides:

- **Loose coupling** — services don't know about each other
- **Resilience** — Payment Service can be down; events are persisted in Kafka and consumed when it recovers
- **Scalability** — multiple consumers can subscribe to the same topic independently

### 2. API Gateway as Single Entry Point

All client traffic enters through the gateway which handles:
- Dynamic routing via Eureka service discovery (`lb://service-name`)
- Circuit Breaker with meaningful fallback responses
- CORS policy enforcement in one place
- Request/response header enrichment

### 3. Separate Databases per Service

Each service owns its data exclusively. No service queries another service's database directly. Cross-service data needs go through the API or events — this is a core microservices principle enforced by design.

### 4. UUID as Primary Key

Both PostgreSQL services use `@GeneratedValue(strategy = GenerationType.UUID)` instead of auto-increment integers. This allows IDs to be generated without a database round-trip and avoids ID conflicts if services are ever merged or migrated.

---


<div align="center">
Built with Spring Boot 3.2 · Java 21 · Apache Kafka · Docker
</div>