# oraPay Server

> **Enterprise-Grade FinTech Wallet & Double-Entry Ledger System**  
> *Architected as an Event-Driven Modular Monolith with Spring Boot 3, Java 21, and Full-Stack Observability.*

---

## 📌 Architectural Overview

**oraPay** is designed to address critical financial engineering challenges: preventing double-spending under high concurrency, guaranteeing deterministic deadlock avoidance, enforcing exactly-once idempotency semantics, and maintaining balanced double-entry accounting with zero float precision errors.

The project doubles as an infrastructure testing ground that evolves across three stages:
1. **Phase 1 (Local):** 3-Tier Container Architecture (Nginx &rarr; App &rarr; PostgreSQL) with Prometheus + Grafana telemetry in Docker Compose.
2. **Phase 2 (Local K8s):** Container orchestration in a multi-node Kind cluster deployed using Helm Charts.
3. **Phase 3 (Cloud):** Automated AWS infrastructure provisioning via Terraform and GitOps CI/CD pipelines.

---

## 🛠 Tech Stack

- **Core Framework:** Spring Boot 3.3.x, Java 21 (Groovy DSL Gradle Wrapper)
- **Modularity & Events:** Spring Modulith (`@TransactionalEventListener`, in-process domain events)
- **Security:** Spring Security 6, Stateless JWT (JJWT) with Refresh Token rotation
- **Database & Persistence:** PostgreSQL 16, Spring Data JPA, Flyway Database Migrations
- **Reverse Proxy & Ingress:** Nginx (Rate-limiting, SSL termination, reverse proxy routing)
- **Observability:** Prometheus, Micrometer Metrics Registry, Grafana Dashboards, Node Exporter
- **Testing:** JUnit 5, Testcontainers (PostgreSQL), Spring Security Test

---

## 🏛 Key Engineering Patterns

### 1. Deterministic Lock Ordering
To eliminate database deadlocks during peer-to-peer transfers, locks on wallet entities (`SELECT ... FOR UPDATE`) are acquired strictly in lexicographical/sorted primary key order before balance manipulation.

### 2. Zero-Float Monetary Storage
Monetary amounts are encapsulated in the immutable `MonetaryAmount` Value Object and persisted strictly as minor units (`BIGINT` cents/kobo) with `CHECK (balance >= 0)` constraints.

### 3. Exactly-Once Idempotency
All mutation endpoints mandate an `Idempotency-Key` header. Requests are intercepted by an `IdempotencyFilter`, caching completed responses and preventing replay attacks.

### 4. Pluggable Split Payment Engine
Uses the **Strategy Pattern** (`PercentageSplitStrategy`, `FixedFeeSplitStrategy`) with remainder allocation to ensure:
$$\text{Total Debit} = \sum \text{Credit Legs}$$

### 5. Decoupled Ledger via Domain Events
Money transfers and split payments publish internal events (`TransferCompletedEvent`, `SplitPaymentCompletedEvent`). The `ledger` module listens asynchronously to write immutable double-entry journal records without tight coupling.

---

## 📂 Project Structure

```text
orapay-server/
├── build.gradle                          # Root Groovy Gradle build configuration
├── settings.gradle                       # Single-project settings
├── Dockerfile                            # Multi-stage container build
├── docker-compose.yml                    # Local 3-tier & observability stack
├── observability/
│   ├── prometheus/prometheus.yml         # Metric scrape definitions (:8080/actuator/prometheus)
│   ├── grafana/                          # Dashboards & automated data source provisioning
│   └── nginx/nginx.conf                  # Edge rate-limiting & reverse proxy rules
└── src/
    ├── main/
    │   ├── java/com/orapay/
    │   │   ├── common/                   # Shared kernel (MonetaryAmount, Idempotency, Events)
    │   │   ├── security/                 # JWT Provider, Security Filters & Auth Principal
    │   │   ├── auth/                     # Token lifecycle & authentication endpoints
    │   │   ├── user/                     # User identity, KYC tiers & phone normalization
    │   │   ├── wallet/                   # Balances, 10-digit phone alias & sorted locking
    │   │   ├── transfer/                 # Idempotent P2P transfers & transaction state
    │   │   ├── split/                    # Strategy-driven multi-party split payments
    │   │   └── ledger/                   # Immutable double-entry journal & discrepancy audits
    │   └── resources/
    │       ├── application.yml           # Base application configuration
    │       ├── application-docker.yml    # Docker container profile
    │       └── db/migration/             # Flyway SQL schema versions (V1..V5)
    └── test/java/com/orapay/             # Modulith boundaries & high-concurrency race condition tests
```

---

## 🚀 API Surface Summary

| Method | Endpoint | Description | Auth / Headers |
|---|---|---|---|
| `POST` | `/api/auth/register` | User registration & wallet auto-provisioning | Public |
| `POST` | `/api/auth/login` | Phone & PIN authentication (issues JWT + Refresh) | Public |
| `POST` | `/api/auth/refresh` | Rotates expired access tokens | Public |
| `GET` | `/api/wallets/{identifier}` | Fetches balance & status by UUID or Phone | `Bearer <JWT>` |
| `POST` | `/api/transfers` | Atomic P2P fund transfer | `Bearer <JWT>`, `Idempotency-Key` |
| `POST` | `/api/payments/split` | Multi-party split settlement checkout | `Bearer <JWT>`, `Idempotency-Key` |
| `GET` | `/api/wallets/{id}/ledger` | Paginated double-entry ledger audit trail | `Bearer <JWT>` |
| `GET` | `/actuator/prometheus` | Raw metrics scrape target | Scrape credentials |
| `GET` | `/health/live`, `/health/ready` | Liveness & readiness probes | Public |

---

## 🚦 Getting Started Locally

### Prerequisites
- **JDK 21**
- **Docker & Docker Compose**

### 1. Build Application Jar
```bash
./gradlew clean build -x test
```

### 2. Start Phase 1 Docker Environment
Spin up PostgreSQL, Nginx, Prometheus, Grafana, and the oraPay backend service:
```bash
docker compose up --build -d
```

### 3. Access Services
- **Nginx Entry Point:** `http://localhost:80`
- **Backend Actuator / Metrics:** `http://localhost:8080/actuator/prometheus`
- **Grafana Dashboards:** `http://localhost:3000` *(Default: admin / admin)*
- **Prometheus UI:** `http://localhost:9090`

---

## 🧪 Running Concurrency & Modular Boundary Tests

Execute Spring Modulith module verification and high-concurrency race condition tests:
```bash
./gradlew test
```

---

## 📜 License
Internal & Confidential. Engineered for Cloud & Distributed Systems Mastery.
