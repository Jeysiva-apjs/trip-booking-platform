# Trip Booking Platform

Book a flight seat and a hotel room together. Either both are booked, or neither is.

![Java](https://img.shields.io/badge/Java-17-007396) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F) ![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0-6DB33F) ![Kafka](https://img.shields.io/badge/Kafka-choreography%20saga-231F20) ![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)


## What it does

- Books a flight seat and a hotel room as one unit — no half-booked trips.
- Stops two people from taking the same seat, using optimistic locking (`@Version`). The loser gets `409 Conflict`.
- Charges the payment separately over Kafka. If the charge fails, the seat and room are released automatically.
- Runs behind a gateway, with Eureka for service discovery and Zipkin for tracing.


## Tech stack

Java 17 · Spring Boot 3.5 · Spring Cloud 2025.0 (Eureka, Gateway MVC) · MySQL 8 + Spring Data JPA · Apache Kafka · Micrometer + OpenTelemetry → Zipkin · springdoc-openapi · Maven multi-module


## Architecture

Four services. The client only ever talks to the gateway.

```
client
  │
  ▼
gateway (8090) ──▶ booking (8080) ◀──Kafka──▶ payment (8081)
                        │                          │
                     MySQL                       MySQL
                   `booking`                   `payment`

discovery (8761)  — every service registers here, so nothing
                    is wired to a fixed host:port

zipkin (9411)     — every service sends traces here
```

| Service | Port | Database | What it does |
|---|---|---|---|
| discovery | 8761 | — | Eureka registry |
| gateway | 8090 | — | Single entry point; routes by service name |
| booking | 8080 | `booking` | Flights, hotels, trips, users |
| payment | 8081 | `payment` | Charges the trip, keeps its own records |

Each service owns its own database. Neither can read the other's tables.


## How a booking works

Booking and payment talk through two Kafka topics: `payment-requests` and `payment-results`.

```
POST /api/trips {seatId, roomId, userId}
   │
   │  booking-service:
   │    1. reserve the seat and the room   (one transaction)
   │    2. save the trip as PENDING
   │    3. send PaymentRequested  ──────▶  payment-service charges the card
   │                                              │
   ◀── 202 Accepted (PENDING)                     │
                                                  │
        PaymentResult  ◀──────────────────────────┘
              │
              ├─ approved  →  trip CONFIRMED
              └─ declined  →  release seat + room, trip CANCELLED
```

Payment happens outside the booking transaction, so a slow charge never holds database rows open. If the same result arrives twice, the second one is ignored — the trip is already settled.


## API

Everything goes through the gateway on `:8090`. No auth — every path is open.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/flights` · `/{id}` · `/{id}/seats` | |
| POST | `/api/flights` | creates a flight and its seats |
| GET | `/api/hotels` · `/{id}` · `/{id}/rooms` | |
| POST | `/api/hotels` | creates a hotel and its rooms |
| POST | `/api/trips` | book a trip — returns `202 PENDING` |
| GET | `/api/trips` · `?userId=` | all trips, or one user's |
| GET · POST | `/api/trips/{id}` · `/{id}/cancel` | |
| GET | `/api/payments/{id}` · `?tripReference=…` | |

Errors: `400` bad input · `404` not found · `409` seat or room already taken.

```bash
curl -X POST http://localhost:8090/api/trips \
  -H 'Content-Type: application/json' \
  -d '{"seatId":1,"roomId":1,"userId":2}'

curl http://localhost:8090/api/trips?userId=2
```


## Project structure

```
trip-booking-platform/       root pom
├── docker/                  Dockerfile per service + docker-compose.yml
├── discovery-service/       Eureka server
├── gateway-service/         routing only
├── booking-service/
│   └── com.jeysiva.booking
│       ├── user/    users (no passwords — a trip just holds a user id)
│       ├── flight/  flight → seats
│       ├── hotel/   hotel  → rooms
│       ├── trip/    trip = seat + room + user
│       ├── saga/    Kafka events, publisher, result listener
│       └── common/  exceptions, error handler, OpenAPI, seeder
└── payment-service/         charges and payment records
```

Code is grouped by feature, not by layer — changing how trips book touches one folder, not five.


## Running it

With Docker (builds from source, nothing needed on the host):

```bash
docker compose -f docker/docker-compose.yml up --build
```

Gateway on `:8090`, Eureka on `:8761`, Zipkin on `:9411`. Booking and payment are not published on purpose.

By hand — needs JDK 17, MySQL 8, Kafka and Zipkin (`./kafka-up.sh`, `./zipkin-up.sh`). Start discovery first:

```bash
./mvnw test                                    # build + test everything

./mvnw -pl discovery-service spring-boot:run   # 8761
./mvnw -pl payment-service   spring-boot:run   # 8081
./mvnw -pl booking-service   spring-boot:run   # 8080
./mvnw -pl gateway-service   spring-boot:run   # 8090
```
