# slit-server

Spring Boot 4 backend for **Slit** — a receipt scanning and expense splitting app.

---

## Quick start

```bash
docker compose up --build
```

To wipe all data and start completely fresh:

```bash
docker compose down -v
docker compose up --build
```

---

## Local URLs

| Service        | URL                                              | Credentials       |
|----------------|--------------------------------------------------|-------------------|
| **API**        | http://localhost:8080                            | —                 |
| **Swagger UI** | http://localhost:8080/swagger-ui.html            | —                 |
| **pgAdmin**    | http://localhost:5050                            | admin / admin     |
| **Jaeger UI**  | http://localhost:16686                           | —                 |
| **Prometheus** | http://localhost:9090                            | —                 |
| **Grafana**    | http://localhost:3000                            | admin / admin     |

---

## API overview

All endpoints except auth and Swagger require a `Bearer <token>` header.

### Auth — `/api/auth`
| Method | Path               | Body                              | Description          |
|--------|--------------------|-----------------------------------|----------------------|
| POST   | `/register`        | `email, password, displayName`    | Register new user    |
| POST   | `/login`           | `email, password`                 | Login, returns JWT   |

### Users — `/api/users`
| Method | Path               | Description                        |
|--------|--------------------|------------------------------------|
| GET    | `/me`              | Current user profile               |
| GET    | `/me/credits`      | Remaining scan credits             |

### Scan — `/api/scan`
| Method | Path               | Description                        |
|--------|--------------------|------------------------------------|
| POST   | `/use`             | Deduct one scan credit             |

### Receipts — `/api/receipts`
| Method | Path               | Description                        |
|--------|--------------------|-------------------------------------|
| GET    | `/`                | List all receipts for current user |
| POST   | `/`                | Save a new receipt                 |
| GET    | `/{id}`            | Get single receipt                 |
| PUT    | `/{id}`            | Update receipt                     |
| DELETE | `/{id}`            | Delete receipt                     |

### Groups — `/api/groups`
| Method | Path                         | Description                    |
|--------|------------------------------|--------------------------------|
| GET    | `/`                          | List groups current user is in |
| POST   | `/`                          | Create a group                 |
| GET    | `/{id}`                      | Get group with members         |
| DELETE | `/{id}`                      | Delete group (creator only)    |
| POST   | `/{id}/members?email=`       | Add member by email            |
| DELETE | `/{id}/members/{memberId}`   | Remove member                  |

### Expenses — `/api/groups/{groupId}/expenses`
| Method | Path               | Description                        |
|--------|--------------------|-------------------------------------|
| GET    | `/`                | List expenses for a group          |
| POST   | `/`                | Create expense (equal or custom split) |
| DELETE | `/{expenseId}`     | Delete expense                     |

---

## Project structure

```
src/main/java/slit/slitserver/
├── config/
│   └── SecurityConfig.java          # JWT filter chain, BCrypt, CORS
├── controller/
│   ├── AuthController.java          # /api/auth/**
│   ├── ExpenseController.java       # /api/groups/{id}/expenses
│   ├── GroupController.java         # /api/groups/**
│   ├── ReceiptController.java       # /api/receipts/**
│   └── UserController.java          # /api/users/me, /api/scan/use
├── dto/
│   ├── auth/                        # RegisterRequest, LoginRequest, AuthResponse
│   ├── expense/                     # ExpenseRequest, ExpenseResponse, ExpenseSplitRequest
│   ├── group/                       # GroupRequest, GroupResponse, GroupMemberResponse
│   └── receipt/                     # ReceiptRequest, ReceiptResponse, ReceiptItemRequest/Response
├── entity/
│   ├── User.java
│   ├── SlitGroup.java
│   ├── GroupMember.java             # Composite PK (groupId + userId)
│   ├── Receipt.java
│   ├── ReceiptItem.java
│   ├── Expense.java
│   └── ExpenseSplit.java
├── exception/
│   ├── ApiException.java            # Typed HTTP error
│   └── GlobalExceptionHandler.java  # @RestControllerAdvice
├── repository/                      # Spring Data JPA interfaces
├── security/
│   ├── JwtUtil.java                 # JJWT 0.12 token generation & validation
│   ├── JwtAuthFilter.java           # OncePerRequestFilter — extracts Bearer token
│   └── UserDetailsServiceImpl.java  # Loads user by UUID string
└── service/
    ├── AuthService.java             # Register (BCrypt) + login (JWT)
    ├── ExpenseService.java          # Equal / custom splits
    ├── GroupService.java            # Group CRUD + membership
    └── ReceiptService.java          # Receipt CRUD with items

src/main/resources/
├── application.properties           # Base config (works locally against localhost:5432)
├── application-docker.properties    # Docker overrides (Jaeger endpoint, ddl-auto=none)
├── logback-spring.xml               # Plain text locally, JSON in Docker (for Promtail)
└── db/migration/
    └── V1__init.sql                 # Flyway — creates all tables
```

---

## Observability stack

| Tool           | Purpose                                                  |
|----------------|----------------------------------------------------------|
| **Prometheus** | Scrapes `/actuator/prometheus` every 10 s               |
| **Grafana**    | Dashboards — Prometheus + Loki + Jaeger pre-wired        |
| **Loki**       | Log aggregation                                          |
| **Promtail**   | Reads Docker container logs, parses JSON, indexes traceId|
| **Jaeger**     | Distributed tracing via OpenTelemetry (OTLP HTTP)        |

Traces flow: `app → Jaeger (port 4318)`. In Grafana → Explore → Loki, filter by `traceId` and click through to Jaeger.

---

## Tech stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Language     | Java 21                             |
| Framework    | Spring Boot 4 / Spring Security 7   |
| ORM          | Hibernate 7 / Spring Data JPA       |
| Database     | PostgreSQL 17                       |
| Migrations   | Flyway 10                           |
| Auth         | JWT (JJWT 0.12) + BCrypt            |
| Tracing      | OpenTelemetry → Jaeger              |
| Metrics      | Micrometer → Prometheus             |
| Logs         | Logback + Logstash encoder → Loki   |
| Docs         | springdoc-openapi (Swagger UI)      |
| Build        | Maven (mvnw wrapper)                |
| Container    | Docker + Docker Compose             |
| CI           | GitHub Actions                      |
