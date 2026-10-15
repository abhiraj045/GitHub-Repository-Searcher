# GitHub Repository Searcher

Spring Boot REST API that searches GitHub repositories, stores them in PostgreSQL
(insert or update, no duplicates) and serves the stored data with filters.

- **Live API:** <your-render-url>
- **Swagger UI:** <your-render-url>/swagger-ui/index.html
- **Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, PostgreSQL, springdoc-openapi, JUnit 5, Mockito, H2 (tests)

## Run locally
1. Install JDK 17+, Maven and PostgreSQL.
2. Create the database: `CREATE DATABASE github_searcher;`
3. Set environment variables (defaults in brackets):
   `DB_URL` [jdbc:postgresql://localhost:5432/github_searcher], `DB_USERNAME` [postgres],
   `DB_PASSWORD` [postgres], `GITHUB_TOKEN` [optional, raises GitHub rate limit].
4. Start: `mvn spring-boot:run`
5. Open Swagger UI: http://localhost:8080/swagger-ui/index.html
6. Run tests: `mvn clean test`

## Endpoints
### POST /api/github/search
Fetches from GitHub and saves/updates the results.
```json
{ "query": "springboot", "language": "Java", "sort": "stars" }
```
`query` required; `language` optional; `sort` = stars | forks | updated (default stars); `perPage` optional 1-100.

### GET /api/github/repositories
`GET /api/github/repositories?language=Java&minStars=100&sort=stars`
All params optional. `sort` default stars (descending).

## Errors
Every error returns `{ timestamp, status, error, message }`.

| Case | Status |
|---|---|
| Blank query, invalid sort, bad minStars, malformed JSON | 400 |
| GitHub rate limit | 429 (message includes reset time) |
| GitHub rejects query | 400 |
| GitHub unreachable or 503 | 503 |
| GitHub returns invalid response or unexpected status | 502 |
| No results | 200 with empty list |

## Design
- Layers: controller, service, client (interface plus implementation), mapper, repository.
- GitHub repository id is the primary key, so upsert is one batch `findAllById` plus one `saveAll`.
- Filters are JPA Specifications. Indexes exist on `language` and `stars`.
- Central `@RestControllerAdvice` turns every failure into a clear JSON error.

## Tests
Service (Mockito), controller (MockMvc), GitHub client (MockRestServiceServer, no real network),
repository filters and upsert (H2).