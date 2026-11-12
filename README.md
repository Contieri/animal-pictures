# Animal Pictures

A small Spring Boot microservice that fetches random pictures of cats, dogs and bears, stores them in a database, and serves the last stored picture of each animal. It includes a simple web UI.

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation) |
| Build | Maven, through the included Maven Wrapper (`./mvnw`) |
| Database | H2, embedded and stored in a file: nothing to install |
| Container | Dockerfile (multi-stage) + `docker-compose.yml` |

## Picture sources

| Animal | Source |
|---|---|
| Cat | [cataas.com](https://cataas.com) |
| Dog | [place.dog](https://place.dog) |
| Bear | [placebear.com](https://placebear.com) |

`placekitten.com` was offline when this was built (HTTP 521), so cats come from **Cat as a Service**, which is listed in [public-apis](https://github.com/public-apis/public-apis). Each request asks for a random width and height between 200 and 500 px, so every picture is different. The sources are configurable in `src/main/resources/application.properties`.

## Running it

### Option 1: without Docker

Requires **Java 21** (`java -version`). Maven is not needed: the wrapper downloads the right version.

```bash
./mvnw spring-boot:run
```

Or build the jar and run it:

```bash
./mvnw clean package
java -jar target/animal-pictures-0.0.1-SNAPSHOT.jar
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

The database file is created in `./data/` next to where you start the app.

### Option 2: with Docker

Requires **Docker** with Compose. Java is not needed: the image is built inside Docker.

```bash
docker compose up --build
```

The database lives in the `picture-data` volume, so pictures survive restarts. To stop and delete everything, including stored pictures:

```bash
docker compose down -v
```

Either way, the app runs at **http://localhost:8080**.

## Using it

### Web UI

Open **http://localhost:8080**, choose an animal and a number, and click **Fetch and save**. **Show last saved** shows the last stored picture of the selected animal.

### REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/pictures/{animal}?count=N` | Fetches `N` (1-10, default 1) random pictures and stores them. Returns their metadata. |
| `GET` | `/api/pictures/{animal}/latest` | The last stored picture of that animal, as an image. |
| `GET` | `/api/pictures/{animal}/latest/metadata` | Metadata (JSON) of that picture. |
| `GET` | `/api/pictures/{id}/image` | Any stored picture by id, as an image. |

`{animal}` is `cat`, `dog` or `bear` (case-insensitive).

Examples:

```bash
# Fetch and store 3 dog pictures
curl -X POST "http://localhost:8080/api/pictures/dog?count=3"

# Download the last stored dog picture
curl -o dog.jpg http://localhost:8080/api/pictures/dog/latest
```

Example response of the `POST`:

```json
[
  {
    "id": 1,
    "animal": "dog",
    "contentType": "image/png",
    "sizeBytes": 152340,
    "sourceUrl": "https://place.dog/312/245",
    "createdAt": "2026-11-12T14:20:31.123Z",
    "imageUrl": "/api/pictures/1/image"
  }
]
```

Errors are returned as [RFC 9457 problem details](https://www.rfc-editor.org/rfc/rfc9457):

| Status | When |
|---|---|
| `400` | Unknown animal, or `count` outside 1-10 |
| `404` | No picture of that animal stored yet |
| `502` | The picture source failed. Nothing from that request is stored. |

### Looking at the database

The H2 console runs at http://localhost:8080/h2-console. Use JDBC URL `jdbc:h2:file:./data/animal-pictures` (or `jdbc:h2:file:/app/data/animal-pictures` in Docker), user `sa` and an empty password.

## Tests

```bash
./mvnw test
```

The tests do not need internet access:

- **Unit tests** for the animal type parsing, the image format detection and the service, with the picture client mocked.
- **HTTP client tests** that run the real client against a small local HTTP server. They cover redirects, failing sources and a wrong `Content-Type`.
- **Integration tests** that start the whole application with an in-memory H2 database and call the REST API through MockMvc. Only the internet-facing client is mocked.

## Design notes

- **Layers:** `PictureController` (HTTP and validation) → `PictureService` (business logic) → `AnimalPictureClient` (source APIs) and `PictureRepository` (database).
- **Images are stored in the database** as bytes, together with their content type, source URL, size and timestamp. Pictures stay available even if a source goes offline.
- **All or nothing:** a request downloads every picture before saving, so a failing source does not leave a partial batch.
- **Real image format:** place.dog sends PNG files labelled `image/jpeg`. The client reads the first bytes of the file to find the real type, so browsers display it correctly.
- **"Latest"** is the highest id for that animal. Ids always grow, so this is reliable even when two pictures share a timestamp.
- **Container:** the multi-stage Dockerfile builds with the same Maven Wrapper and runs the jar on a JRE-only image as a non-root user.

## What I would add with more time

- **Resilience:** retries with backoff and a circuit breaker per source (Resilience4j), and fetching several pictures in parallel.
- **Storage:** move image bytes to object storage (S3, MinIO) and keep only metadata in a PostgreSQL database for production use, with Flyway migrations instead of `ddl-auto`.
- **API:** a paginated list of stored pictures, deleting pictures, and OpenAPI/Swagger documentation.
- **Operations:** Spring Boot Actuator health checks (also used as the Docker health check), metrics, and a Helm chart.
- **Tests:** contract tests against the real sources run separately from the unit tests, and a CI pipeline (GitHub Actions) that runs the tests and builds the image.
