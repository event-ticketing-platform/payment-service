# Payment Service

The Payment Service manages payment records for bookings.

It creates payment requests, stores payment status, marks payments as completed or failed, and can notify the Booking Service when a payment is completed.

## What It Does

- Creates a payment for a booking.
- Stores amount, currency, method, status, and timestamps.
- Lists payments by booking id.
- Completes a payment.
- Fails a payment.
- Calls the Booking Service confirmation endpoint when a payment is completed.

For the current demo, the Booking Service callback can run in mock mode so payment can be tested independently.

## Main Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/payments` | Create a payment |
| `GET` | `/payments/{id}` | Get one payment |
| `GET` | `/payments/booking/{bookingId}` | List payments for a booking |
| `POST` | `/payments/{id}/complete` | Mark a payment as completed |
| `POST` | `/payments/{id}/fail` | Mark a payment as failed |
| `GET` | `/actuator/health` | Check if service is running |

## Run With Docker Compose

From this folder:

```bash
docker compose up --build
```

The service runs on:

```text
http://localhost:8082
```

PostgreSQL runs on host port:

```text
5434
```

## Run With The Full System

From the infra folder:

```bash
cd ../infra
docker compose up --build
```

This is the recommended way because it also starts the API Gateway, Booking Service, frontend, and databases.

## Swagger

Open:

```text
http://localhost:8082/swagger-ui.html
```

## Health Check

```bash
curl http://localhost:8082/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## Example Payment Request

```bash
curl -X POST http://localhost:8082/payments \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 1,
    "amount": 50.00,
    "currency": "EUR",
    "paymentMethod": "CARD"
  }'
```

## Important Environment Variables

| Variable | Default | Meaning |
| --- | --- | --- |
| `SERVER_PORT` | `8082` | Service port |
| `SPRING_DATASOURCE_URL` | local payment DB | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `payment_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `payment_pass` | DB password |
| `BOOKING_SERVICE_URL` | `http://localhost:8081` | Booking Service URL |
| `BOOKING_SERVICE_MOCK_ENABLED` | `true` | Skip real booking confirmation callback |

## Tests

```bash
./mvnw test
```
