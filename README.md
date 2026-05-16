# Payment Service

The Payment Service owns payment records and transaction history for bookings.
It does not call Booking synchronously; when a payment is processed it publishes a
`PaymentCompleted` RabbitMQ event and Booking confirms the reservation from that
event.

## Main Endpoints

All endpoints require a gateway-issued JWT when called directly.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/payments` | Create a payment |
| `GET` | `/payments/{id}` | Get one payment |
| `POST` | `/payments/{id}/process` | Complete payment and publish `PaymentCompleted` |
| `POST` | `/payments/{id}/refund` | Refund a completed payment |
| `GET` | `/bookings/{bookingId}/payment` | Get latest payment for a booking |
| `GET` | `/payments/booking/{bookingId}` | List payment records for a booking |
| `GET` | `/actuator/health` | Check service health |

## Run With The Full System

From the infra folder:

```bash
cd ../infra
docker compose up --build
```

This starts Payment with PostgreSQL, RabbitMQ, Booking, the API Gateway, and the
frontend.

## Local Service

The service runs on:

```text
http://localhost:8082
```

Swagger:

```text
http://localhost:8082/swagger-ui.html
```

Health:

```bash
curl http://localhost:8082/actuator/health
```

## Important Environment Variables

| Variable | Default | Meaning |
| --- | --- | --- |
| `SERVER_PORT` | `8082` | Service port |
| `SPRING_DATASOURCE_URL` | local payment DB | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `payment_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `payment_pass` | DB password |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `SPRING_RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `JWT_ISSUER` | `event-ticketing-gateway` | Expected JWT issuer |
| `JWT_SECRET` | development secret | Shared JWT signing secret |
| `PAYMENT_EVENTS_EXCHANGE` | `payment-events` | RabbitMQ exchange for payment events |

## Tests

```bash
./mvnw test
```
