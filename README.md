# GoComet Ride-Hailing System

A ride-hailing platform (Uber/Ola style) built with Spring Boot, React, PostgreSQL, and Redis.

## Quick Start

### Docker (recommended)
```bash
docker-compose up --build
```
- Frontend: http://localhost:3000
- API: http://localhost:9000/v1

### Local Development

#### Prerequisites
- Java 21+
- PostgreSQL 15+
- Redis 7+
- Node.js 18+

#### Run
```bash
# Terminal 1 — Backend (port 9000)
cd core-api
./mvnw spring-boot:run

# Terminal 2 — Frontend (port 5173)
cd core-web
npm install
npm run dev
```

---

## Architecture

### Tech Stack
- **Backend**: Kotlin 2.1.10, Spring Boot 3.4.4, MyBatis, JWT
- **Database**: PostgreSQL (transactional data, audit trail)
- **Cache/Geo**: Redis (driver locations via GEO, dispatch pub/sub, ride expansion queue)
- **Frontend**: React 19, Vite, Leaflet.js (OpenStreetMap)
- **Monitoring**: New Relic Java Agent + Micrometer

### High-Level Design

```
┌──────────┐     ┌──────────┐     ┌──────────────────────────────────┐
│  Rider   │────>│  Nginx/  │────>│  Spring Boot Instance(s)         │
│  (React) │<────│  LB      │<────│                                  │
├──────────┤     │          │     │  ┌─────────┐  ┌───────────────┐  │
│  Driver  │────>│          │────>│  │ REST API │  │ SSE Emitters  │  │
│  (React) │<────│          │<────│  └────┬────┘  └───────┬───────┘  │
└──────────┘     └──────────┘     │       │               │          │
                                  │  ┌────▼────┐  ┌───────▼───────┐  │
                                  │  │Services │  │DispatchService│  │
                                  │  └────┬────┘  └───────┬───────┘  │
                                  └───────┼───────────────┼──────────┘
                                          │               │
                                  ┌───────▼───┐   ┌───────▼───────┐
                                  │PostgreSQL  │   │    Redis      │
                                  │(rides,     │   │(GEO locations,│
                                  │ users,     │   │ pub/sub,      │
                                  │ payments)  │   │ queue, sets)  │
                                  └────────────┘   └───────────────┘
```

### Low-Level Design

#### State Machines

```
Ride:   REQUESTED ──> ACCEPTED ──> IN_PROGRESS ──> COMPLETED
              \                                      /
               └──────────> CANCELLED <─────────────┘

Driver: OFFLINE <──> AVAILABLE ──> ON_TRIP ──> AVAILABLE
```

#### Event-Driven Matching (no server-assigned matching)

1. Rider creates ride → stored as REQUESTED in PostgreSQL
2. `RideDispatchService` publishes event to Redis Pub/Sub channel
3. All server instances receive event, find nearby available drivers via Redis GEORADIUS
4. Push updated ride list to each driver's SSE connection
5. Driver picks a ride and accepts → atomic transaction (first wins, others get 409)
6. Ride removed from all other drivers' lists via Pub/Sub

#### Redis Usage

| Pattern | Key | Purpose |
|---------|-----|---------|
| GEO Set | `driver:locations` | Sub-ms driver proximity lookups (GEOADD/GEORADIUS) |
| Set | `driver:available` | Track which drivers are online |
| Pub/Sub | `ride:dispatch` | Cross-instance event dispatch for SSE |
| Sorted Set | `ride:pending_queue` | Delayed queue for radius expansion (score = timestamp) |
| String | `ride:queue_lock` | Distributed lock for queue processing (SETNX + TTL) |

#### Surge Pricing

Based on available driver count within 5km of pickup:
- 0 drivers → 2.0x
- 1-2 drivers → 1.5x
- 3-5 drivers → 1.2x
- 6+ drivers → 1.0x (no surge)

Uses only Redis GEO data — no SQL queries.

#### Radius Expansion

Rides with no takers get progressively wider search radius:
- Created at 5km → after 30s expands to 10km → 15km → 20km (max)
- Managed by a Redis sorted set queue with distributed locking
- Each expansion re-dispatches to drivers in the wider radius
- Rider sees the search circle grow on the map in real-time via SSE

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/auth/register` | Register rider or driver |
| POST | `/v1/auth/login` | Login |
| POST | `/v1/rides` | Create ride request |
| GET | `/v1/rides/active` | Get rider's active/unpaid ride |
| GET | `/v1/rides/{id}` | Get ride details |
| POST | `/v1/rides/{id}/cancel` | Cancel ride |
| GET | `/v1/rides/{id}/events` | SSE stream for ride updates |
| GET | `/v1/rides/{id}/driver-location` | Poll driver location |
| PUT | `/v1/drivers/me/location` | Update driver location |
| PUT | `/v1/drivers/me/status` | Go online/offline |
| GET | `/v1/drivers/me/status` | Sync driver state |
| GET | `/v1/drivers/me/active-ride` | Get driver's active ride |
| GET | `/v1/drivers/me/rides/stream` | SSE stream for available rides |
| POST | `/v1/drivers/me/rides/{id}/accept` | Accept a ride |
| POST | `/v1/drivers/me/rides/{id}/decline` | Decline a ride |
| POST | `/v1/trips/{rideId}/start` | Start trip |
| POST | `/v1/trips/{rideId}/end` | End trip + fare calculation |
| POST | `/v1/payments` | Process payment (PSP stub) |

---

## Requirement Coverage

### 1. Business Logic
- All core APIs implemented with validation
- Trip lifecycle: REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED/CANCELLED
- State machine enforcement prevents invalid transitions
- Soft deletes via `delete_info` JSONB column on all tables
- Idempotency: duplicate ride creation blocked, concurrent accept returns 409
- Edge cases: timeouts (radius expansion), declined offers (re-dispatch), stale state recovery on reload

### 2. Scalability & Reliability
- Driver locations in Redis GEO (handles 200k+ updates/sec)
- Redis Pub/Sub for cross-instance SSE dispatch (stateless horizontal scaling)
- Redis sorted set queue with distributed lock for ride expansion
- PostgreSQL for transactional data, Redis for hot-path reads
- All components stateless — state lives in PostgreSQL and Redis

### 3. New Relic Monitoring
- Java agent integrated for transaction tracing and SQL query analysis
- Micrometer registry exports JVM/HTTP metrics
- API latencies tracked per endpoint

### 4. API Latency Optimization
- Redis GEO replaces Haversine SQL joins for driver lookups (50ms → <1ms)
- Composite indexes on ride status + location columns
- Redis-first reads for driver locations with PostgreSQL fallback
- Bounding box pre-filter for spatial queries

### 5. Atomicity & Consistency
- `@Transactional` on all multi-write operations
- Concurrent ride acceptance: first driver wins, others get 409 Conflict
- Redis available set kept in sync with PostgreSQL driver status
- Payment enforcement: unpaid rides block new ride creation

### 6. Frontend with Live Updates
- Rider: map with pickup/dropoff markers, SSE for status updates, driver location polling, expanding search radius circle
- Driver: SSE-pushed ride list, accept/decline per ride, trip simulation with animated marker
- State recovery on page reload for both rider and driver

### Future Scope
- PostGIS for production-grade spatial queries
- WebSocket upgrade from SSE for bidirectional communication
- Real PSP integration (Razorpay/Stripe) replacing PspStub
- Multi-region deployment with region-local writes
- Rate limiting via Redis (currently per-instance in-memory)
- Ride tier support (economy/premium/luxury)
- Driver ratings and feedback
- Push notifications via FCM/APNs
