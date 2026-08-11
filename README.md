# Trip Booking Platform

A **Trip = one flight seat + one hotel room**, booked together as a single all-or-nothing operation.

![Java](https://img.shields.io/badge/Java-17-007396) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F) ![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0-6DB33F) ![Kafka](https://img.shields.io/badge/Kafka-choreography%20saga-231F20) ![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)


## Key Features

- Built a microservices-based trip booking platform using **Java 17**, **Spring Boot 3**, **MySQL**, and **Apache Kafka**.
- Implemented **atomic flight + hotel booking**, ensuring either both the flight seat and hotel room are reserved or neither is.
- Used **optimistic locking** with `@Version` to prevent concurrent bookings for the same flight seat, returning **HTTP 409 Conflict** when a booking conflict occurs.
- Implemented payment processing using a **Kafka-based Saga pattern**, automatically releasing the reserved flight seat and hotel room when payment fails.
- Added **Spring Cloud Gateway** and **Netflix Eureka** for API routing and service discovery.
- Integrated **OpenTelemetry** with **Zipkin** for distributed tracing and observability.


## Tech stack

**Java 17** · **Spring Boot 3.5** · **Spring Cloud 2025.0** (Eureka, Gateway MVC) · **MySQL 8** with Spring Data JPA · **Apache Kafka** · **Micrometer + OpenTelemetry → Zipkin** · **springdoc-openapi** · Maven multi-module


## Architecture

```
                       ┌───────────────────┐
                       │ discovery  (8761) │  Eureka — everyone registers here
                       └─────────▲─────────┘
   client                        │
     │                           │ register / resolve
     ▼                           │
┌──────────────────┐             │
│ gateway   (8090) │─────────────┘   single entry point
│ routes by name   │
└────────┬─────────┘
         │ lb://booking-service
         ▼
┌──────────────────────┐  payment-requests  ┌──────────────────────┐
│ booking      (8080)  │ ─────────────────▶ │ payment      (8081)  │
│ flights/hotels/trips │                    │ charge + audit trail │
│ MySQL `booking`      │ ◀───────────────── │ MySQL `payment`      │
│                      │  payment-results   │                      │
└──────────────────────┘                    └──────────────────────┘
          │                                            │
          └─────────── spans ────▶ Zipkin (9411) ◀─────┘
                one traceId across HTTP *and* both Kafka hops
```

| Service | Port | Database | Responsibility |
|---|---|---|---|
| **discovery** | 8761 | — | Eureka registry — nothing is addressed by fixed host:port |
| **gateway** | 8090 | — | Single entry point; routes by service-id |
| **booking** | 8080 | `booking` | Flights, hotels, trips, users — owns the saga's booking side |
| **payment** | 8081 | `payment` | Charges on a Kafka event, keeps its own audit trail |

Each service has its own schema and credentials — neither can read the other's tables.


## How a booking works

```
POST /api/trips {seatId, roomId, userId}
  │
  ├─ 1. reserve seat + room       ← ONE local transaction, @Version guards the race
  ├─ 2. save trip as PENDING
  ├─ 3. publish PaymentRequested ──────────▶ payment-service charges, saves an audit row
  │
  └─ 202 Accepted (PENDING)      ◀────────── publishes PaymentResult
                                                   │
        ┌──────────────────────────────────────────┘
        ▼
   approved ─▶ trip CONFIRMED
   declined ─▶ COMPENSATE: release seat + room, trip CANCELLED
```

Payment never runs inside the booking transaction, so a slow or failed charge can't hold database rows open. The result listener is **idempotent** — a redelivered event finds the trip already settled and does nothing.


## API

All requests go through the gateway on `:8090`.

Every path below is open — see [Security](#security).

| Method | Path | Notes |
|---|---|---|
| GET | `/api/flights` · `/{id}` · `/{id}/seats` | |
| POST | `/api/flights` | creates a flight + its seats |
| GET | `/api/hotels` · `/{id}` · `/{id}/rooms` | |
| POST | `/api/hotels` | creates a hotel + its rooms |
| POST | `/api/trips` — **book a trip**, `202 PENDING` | body carries `userId` |
| GET | `/api/trips` · `?userId=` | all trips, or one user's |
| GET · POST | `/api/trips/{id}` · `/{id}/cancel` | |
| GET | `/api/payments/{id}` · `?tripReference=…` | |

`400` validation · `404` not found · `409` seat/room taken or lost the race.

```bash
curl -X POST http://localhost:8090/api/trips \
  -H 'Content-Type: application/json' \
  -d '{"seatId":1,"roomId":1,"userId":2}'

curl http://localhost:8090/api/trips?userId=2
```


## Project structure

```
trip-booking-platform/       root aggregator pom
├── docker/                  Dockerfile per service + docker-compose.yml
├── discovery-service/       Eureka server
├── gateway-service/         routing only
├── booking-service/
│   └── com.jeysiva.booking
│       ├── user/    User accounts (no credentials — a trip's owner is just an id)
│       ├── flight/  Flight → Seats
│       ├── hotel/   Hotel  → Rooms
│       ├── trip/    Trip = seat + room, owned by a user
│       ├── saga/    Kafka events, publisher, result listener
│       └── common/  exceptions, error handler, OpenAPI, seeder
└── payment-service/         charge + audit trail
```

Packaged **by feature, not by layer** — a change to "how trips book" touches one folder, not five.


## Running it

Everything at once, needing only Docker — the images build from source, so nothing has to be built on the host first:

```bash
docker compose -f docker/docker-compose.yml up --build
```

Gateway on `:8090`, Eureka on `:8761`, Zipkin on `:9411`; booking and payment are deliberately unpublished.

Or run it by hand — needs JDK 17, MySQL 8, Kafka and Zipkin (`./kafka-up.sh`, `./zipkin-up.sh`). Start discovery first:

```bash
./mvnw test                                    # build + test everything

./mvnw -pl discovery-service spring-boot:run   # 8761
./mvnw -pl payment-service   spring-boot:run   # 8081
./mvnw -pl booking-service   spring-boot:run   # 8080
./mvnw -pl gateway-service   spring-boot:run   # 8090
```


