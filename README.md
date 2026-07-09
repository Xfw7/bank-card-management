# Bank Card Management System

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

Spring Boot backend for bank card management. Admins manage users and cards; regular users can view their cards, request a block, and transfer money between their own cards.

Auth is JWT-based (`ADMIN` / `USER`). PAN is encrypted at rest (AES-GCM) and never returned in full — only a masked number in responses.

Base URL: `http://localhost:8080/api`

## Quick start

```powershell
copy .env.example .env          # fill in DB_*, JWT_SECRET, AES_SECRET_KEY, ADMIN_PASSWORD
.\run-dev.ps1                   # load .env (PowerShell)
docker compose up -d            # postgres on :5433
mvn spring-boot:run
```

On Linux/macOS, export vars from `.env` yourself (`set -a; source .env; set +a`) — there is no shell script for that yet.

Swagger: http://localhost:8080/api/swagger-ui.html

```powershell
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"YOUR_PASSWORD"}'
```

Expected response:

```json
{
  "accessToken": "eyJhbG...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    "enabled": true,
    "createdAt": "2026-07-09T12:00:00Z"
  }
}
```

Use `Authorization: Bearer <accessToken>` on protected endpoints.

## Contents

- [Stack](#stack)
- [Features](#features)
- [Configuration](#configuration)
- [Running locally](#running-locally)
- [Build](#build)
- [Running tests](#running-tests)
- [API](#api)
- [Database](#database)
- [Docker](#docker)
- [Development notes](#development-notes)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Stack

Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, PostgreSQL 16, Liquibase, MapStruct, jjwt, springdoc (Swagger UI), Maven. Docker Compose is used for Postgres only.

## Features

Admins create users (no self-registration), issue cards, block/activate/delete them, and process block requests.

Users see only their own cards, can filter by status or last four digits, request a block, and transfer between their own active cards.

Transfers use pessimistic locking; cards are locked in ascending id order to avoid deadlocks. Expired cards are marked `EXPIRED` by a nightly scheduler (00:00 UTC). Card deletion is soft (`deleted_at`).

## Configuration

Required in `.env`:

| Variable | Notes |
|----------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5433/bank_cards` |
| `DB_USERNAME`, `DB_PASSWORD` | same creds for docker-compose and the app |
| `JWT_SECRET` | min 32 chars |
| `AES_SECRET_KEY` | 16, 24, or 32 chars (32 is fine) |
| `ADMIN_PASSWORD` | used by `AdminBootstrap` on first run |

Optional: `ADMIN_USERNAME` (default `admin`), `JWT_EXPIRATION_MS`, `SERVER_PORT`, `CORS_ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE`.

Spring settings: `application.yml`, profile overrides in `application-dev.yml` / `application-prod.yml`. In `dev`, missing `JWT_SECRET` / `AES_SECRET_KEY` fall back to dev defaults — don't rely on that outside local work.

## Running locally

Prerequisites: Java 21+, Maven 3.9+, Docker with Compose.

```powershell
git clone https://github.com/Xfw7/bank-card-management.git
cd bank-card-management
copy .env.example .env
# edit .env

.\run-dev.ps1
docker compose up -d
mvn spring-boot:run
```

`run-dev.ps1` must run before `docker compose` and `mvn` — compose reads `DB_USERNAME`/`DB_PASSWORD` from the environment.

### Project layout

Layered packages under `com.example.bankcards`: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `security`, `config`, `exception`, `util`.

Root files: `docker-compose.yml`, `.env.example`, `run-dev.ps1`. Migrations in `src/main/resources/db/migration/`. `docs/openapi.yaml` is still a stub — use Swagger or `/api/v3/api-docs`.

## Build

```bash
mvn clean package
java -jar target/bankcards-0.0.1-SNAPSHOT.jar
```

Env vars must be set for the jar run too.

## Running tests

```bash
mvn test
```

Tests aren't written yet. `src/test/java` is mostly empty.

## API

Base path is `/api`. Swagger: http://localhost:8080/api/swagger-ui.html

Paged lists use `page`, `size`, `sort` (Spring Data defaults).

**Public**

```
POST /auth/login
```

**Admin** (`Authorization: Bearer …`, role `ADMIN`)

```
POST   /admin/users
GET    /admin/users
GET    /admin/users/{id}
PATCH  /admin/users/{id}
DELETE /admin/users/{id}

POST   /admin/cards
GET    /admin/cards                    # ?userId, ?status, ?lastFour
GET    /admin/cards/block-requests
PATCH  /admin/cards/{id}/block
PATCH  /admin/cards/{id}/activate
DELETE /admin/cards/{id}
```

**User** (role `USER`)

```
GET   /cards                           # ?status, ?lastFour
GET   /cards/{id}
PATCH /cards/{id}/block-request

POST  /transfers
GET   /transfers
GET   /transfers/cards/{cardId}
```

Card statuses: `ACTIVE`, `BLOCKED`, `EXPIRED`.

On failure the API returns JSON with a `code` from `ErrorCode` — `INSUFFICIENT_BALANCE`, `CARD_BLOCKED`, `VALIDATION_FAILED`, and so on. Validation errors add an `errors` list with `field` / `message`. Clients never see raw exception text from the server.

```json
{
  "timestamp": "2026-07-09T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "errors": [
    { "field": "pan", "message": "Card number must contain exactly 16 digits" }
  ]
}
```

## Database

PostgreSQL 16, db `bank_cards`, host port `5433`.

Schema is managed by Liquibase (`db/migration/db.changelog-master.yaml` → `v1`..`v4` changelogs). Hibernate validates on startup, doesn't generate DDL.

## Docker

```bash
docker compose up -d    # postgres only
docker compose down
```

The app itself isn't containerized — run it with Maven locally.

## Development notes

- Everything goes through `/api` — easy to miss in curl or a frontend base URL.
- Services use `SecurityUtils.getCurrentUser()`, not `@AuthenticationPrincipal` on method args.
- Entities don't leak past the service layer.
- First admin is created on startup only when `ADMIN_PASSWORD` is set and no admin exists yet.
- `CardRepository.findByIdForUpdate` fetches the card owner in the same query to avoid an extra round trip.
- `prod` profile disables Swagger (`application-prod.yml`).

## Troubleshooting

| Problem | Likely cause |
|---------|----------------|
| App fails on startup with AES/JWT error | `AES_SECRET_KEY` must be 16/24/32 chars; `JWT_SECRET` — min 32 |
| `docker compose up` fails on auth | `.env` not loaded — run `.\run-dev.ps1` first |
| Admin login returns 401 | `ADMIN_PASSWORD` was empty on first start; admin never created — fix `.env`, clear DB or insert admin manually |
| 404 on `/auth/login` | Missing `/api` prefix — use `http://localhost:8080/api/auth/login` |
| 401 on protected endpoints | Token missing, expired, or wrong format — `Authorization: Bearer <token>` |

## License

MIT — see [LICENSE](LICENSE).
