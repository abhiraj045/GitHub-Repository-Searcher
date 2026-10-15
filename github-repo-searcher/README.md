# GitHub Repository Searcher

Spring Boot REST API that searches GitHub repositories through the GitHub REST API, stores them in
PostgreSQL (insert or update, never duplicate) and serves the stored data with filters.

- **Live API:** https://github-repository-searcher-bku5.onrender.com
- **Swagger UI:** https://github-repository-searcher-bku5.onrender.com/swagger-ui/index.html
- **Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, PostgreSQL (Neon), springdoc-openapi, JUnit 5, Mockito, H2 (tests), Docker

> The live app runs on a free instance that sleeps when idle, so the first request can take up to a minute.

## Run locally
1. Install JDK 17+, Maven and PostgreSQL.
2. Create the database: `CREATE DATABASE github_searcher;`
3. Set environment variables (defaults in brackets):
   `DB_URL` [jdbc:postgresql://localhost:5432/github_searcher], `DB_USERNAME` [postgres],
   `DB_PASSWORD` [postgres], `GITHUB_TOKEN` [optional, raises GitHub's rate limit].
4. Start the app: `mvn spring-boot:run`
5. Open Swagger UI: http://localhost:8080/swagger-ui/index.html
6. Run the tests: `mvn clean test`

Tables are created automatically on startup.

## Endpoints

### POST /api/github/search
Fetches repositories from GitHub and saves or updates them.
```json
{ "query": "springboot", "language": "Java", "sort": "stars" }
```
`query` is required. `language` is optional. `sort` is `stars`, `forks` or `updated` (default `stars`).
`perPage` is optional (1 to 100, default 30).

### GET /api/github/repositories
```
GET /api/github/repositories?language=Java&minStars=100&sort=stars
```
All parameters are optional. `sort` is `stars` (default), `forks` or `updated`, always highest first.

## Errors
Every error returns `{ "timestamp", "status", "error", "message" }`.

| Case | Status |
|---|---|
| Blank query, invalid sort, bad minStars, malformed JSON | 400 |
| GitHub rate limit | 429 (message includes the reset time) |
| GitHub rejects the query | 400 |
| GitHub unreachable or unavailable | 503 |
| GitHub returns an invalid response or unexpected status | 502 |
| No results found | 200 with an empty list |

## Design
- Layers: controller, service, client (interface and implementation), mapper, repository.
- The GitHub repository id is the primary key, so saving uses one `findAllById` plus one `saveAll`.
- Optional filters are JPA Specifications. Indexes exist on `language` and `stars`.
- One `@RestControllerAdvice` turns every failure into a clear JSON error.

## Tests
- Service logic (Mockito), controller (MockMvc), GitHub client (MockRestServiceServer, no real network),
  repository filters and upsert (H2).

## Deployment
Dockerized (see `Dockerfile`) and deployed on Render with a Neon PostgreSQL database.
Configuration is passed through the environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `GITHUB_TOKEN`.
