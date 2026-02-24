# homelab-monitor

A Spring Boot microservice that monitors Docker container health on a Raspberry Pi 5 homelab, publishes status change events to Apache Kafka, and persists time-series health data to PostgreSQL.

## Architecture

This service is one half of an event-driven monitoring platform. It handles all Docker interaction and data persistence, while a separate [`notification-service`](https://github.com/Ellipsis1/notification-service) consumes Kafka events and handles alerting.

```
Docker Socket
     ↓
homelab-monitor (Spring Boot)
     ↓              ↓
PostgreSQL      Kafka Topic: container-events
                     ↓
            notification-service → Discord
```

## Tech Stack

- **Java 17** with Spring Boot 3.x
- **Apache Kafka** — event publishing for container status changes
- **PostgreSQL 15** — time-series storage for container health snapshots
- **Docker Java library** — Docker API integration via Unix socket
- **Spring Batch** — daily and monthly uptime report aggregation
- **Spring Scheduler** — polling and batch job triggers

## Features

- Polls all Docker containers every 60 seconds via the Docker socket
- Detects meaningful status transitions (`running ↔ exited`) and publishes `ContainerEvent` to Kafka
- Persists health snapshots to PostgreSQL for historical trend analysis
- Exposes REST API for live container status and historical data
- Restart and stop containers remotely via API
- Generates daily uptime reports at 2am, aggregating raw snapshots per container
- Generates monthly reports on the 1st of each month from daily data
- Cleans up raw snapshots older than 7 days to keep the database lean

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/containers` | Live container status (reads Docker directly) |
| GET | `/api/containers/history?hours=24` | Historical snapshots from DB |
| GET | `/api/containers/history/{name}?hours=24` | History for a specific container |
| POST | `/api/containers/restart/{name}` | Restart a container |
| POST | `/api/containers/stop/{name}` | Stop a container |
| GET | `/api/reports?start=2026-01-01&end=2026-01-31` | Daily reports for date range |
| GET | `/api/reports/date/{date}` | Reports for a specific date |
| GET | `/api/reports/container/{name}` | Reports for a specific container |
| GET | `/api/reports/monthly/{year}/{month}` | Monthly report |
| POST | `/api/reports/generate` | Trigger daily report manually |
| POST | `/api/reports/generate/monthly` | Trigger monthly report manually |

## Event Types

Events published to Kafka topic `container-events`:

| Event | Trigger |
|-------|---------|
| `CONTAINER_DOWN` | Container transitions from running to exited |
| `CONTAINER_UP` | Container transitions from exited to running |
| `CONTAINER_RESTARTED` | Manual restart via API |
| `CONTAINER_STOPPED` | Manual stop via API |

## Configuration

All sensitive values are injected via environment variables with local fallbacks:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/homelabmonitor}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

The Docker host is read directly from the `DOCKER_HOST` environment variable, allowing the same image to work on both local Windows dev (`tcp://localhost:2375`) and production Pi (`unix:///var/run/docker.sock`).

## Running Locally

**Prerequisites:** Docker Desktop running with TCP exposed on port 2375.

```bash
mvn spring-boot:run
```

Set environment variables in IntelliJ run configuration or `.env`:
```
DB_PASSWORD=postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/homelabmonitor
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## Docker

Built for ARM64 (Raspberry Pi 5) via Docker Buildx:

```bash
docker buildx build --platform linux/arm64 \
  -t ghcr.io/ellipsis1/homelab-monitor:latest --push .
```

## Deployment

Deployed via Jenkins CI/CD pipeline on push to `main`. Jenkins builds the ARM64 image, pushes to GitHub Container Registry, and SSHs into the Pi to pull and restart the container.

See the full Docker Compose configuration in the [homelab infrastructure repo](https://github.com/Ellipsis1/jellyfin).