# heroes-spring

Spring Boot (Java) implementation of the Heroes CRUD API used in the [Heroes shootout](https://github.com/sean3z/heroes-shootout) (crocket vs Rocket vs Spring Boot vs Fastify), behind the post *Where the cycles go: C++26, Rust, Java and Node on one CRUD API*.

Spring MVC + JdbcTemplate on virtual threads, Actuator for health and Prometheus metrics, ECS structured logging. See `src/main/java/heroes/App.java`.

## API contract (identical across all four apps)

A hero is `{"id": int, "name": string, "alias": string, "power": string, "mentor": string|null}`.

| Method | Path | Success | Notes |
|---|---|---|---|
| GET | `/heroes` | 200, JSON array | first 50 heroes ordered by id |
| GET | `/heroes/{id}` | 200 | 404 if missing |
| POST | `/heroes` | 201 + `Location: /heroes/{id}` | body without `id` |
| PUT | `/heroes/{id}` | 200 | 404 if missing |
| DELETE | `/heroes/{id}` | 204 | 404 if missing |
| GET | `/metrics` | 200 | Prometheus text format |

Seed data (`init.sql`): Barry Allen, the four Robins and 995 filler rows. `./conformance.sh http://localhost:8000` runs the same 15 checks every app passed before benchmarking.

Production features on in every app: structured JSON request log per request, Prometheus metrics, 10 s request/query timeouts, a 16-connection Postgres pool.

## Run with Docker

```bash
docker network create heroes
docker run -d --name pg --network heroes -e POSTGRES_USER=heroes -e POSTGRES_PASSWORD=heroes -e POSTGRES_DB=heroes \
  -v "$PWD/init.sql:/docker-entrypoint-initdb.d/init.sql:ro" postgres:16
docker build -t heroes-spring .
docker run --rm --network heroes -p 8000:8000 -e SPRING_DATASOURCE_URL=jdbc:postgresql://pg/heroes heroes-spring
./conformance.sh http://localhost:8000
```

The `heroes`/`heroes` credentials are local defaults for the demo only; set your own.

## Configuration

Standard Spring Boot properties, overridable by environment (`src/main/resources/application.properties`):

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost/heroes` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `heroes` / `heroes` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `16` |
| `JAVA_OPTS` | empty |

## Versions (as benchmarked, Oct 15, 2026)

- Spring Boot 3.5.16 (parent POM; starters web, jdbc, actuator)
- Java 25 (Eclipse Temurin 25 JRE), virtual threads enabled
- PostgreSQL 16

## License

MIT (sample code). See [LICENSE](LICENSE).
