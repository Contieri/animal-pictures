# Animal Pictures

A small Spring Boot microservice that fetches random pictures of cats, dogs and bears, stores them in a database, and serves the last stored picture of each animal.

It ships with **Snuggle Post**, a web front end for a (fictional) business that sends cute animal pictures as emotional support.

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

### If port 8080 is already in use

Without Docker, the app does not fail when port 8080 is taken (for example by another copy of it). It moves to the next free port, up to 8090, and says so in the log:

```
WARN  Port 8080 is already in use (is another copy of this app running?). Using port 8081 instead.
INFO  Snuggle Post is running at http://localhost:8081 (API docs: http://localhost:8081/swagger-ui.html)
```

Several copies started from the same folder share the same database (H2 `AUTO_SERVER` mode), so they all see the same pictures. To choose a port yourself, start with `--server.port=9090`; to turn the fallback off, set `animal-pictures.port-fallback.enabled=false`.

With Docker the app always listens on 8080 inside its container. If 8080 is taken on your machine, change the left side of the port mapping in `docker-compose.yml`, for example `"8081:8080"`.

## Using it

### Web UI: Snuggle Post

Open **http://localhost:8080**.

- **Get your comfort:** choose a cat, dog or bear and how many pictures (1-10), then click **Send me comfort**. Placeholder cards and a spinner show while the pictures are on their way. **See my latest …** shows the last stored picture of the chosen animal.
- **Your comfort collection:** every stored picture, newest first, with tabs per animal (with counts) and a **Show more** button that loads the next page.
- **Full-size viewer:** click any picture to see it full size with its source URL, the date it was saved, its file size, dimensions and format. Use the arrows or the ← → keys to browse; Esc closes it.

The UI is plain HTML, CSS and JavaScript in `src/main/resources/static`, served by Spring Boot. It works on phones too.

### REST API

**Interactive docs:** open **http://localhost:8080/swagger-ui.html** (also linked as **API docs** in the site menu) to read and try every endpoint from the browser. The OpenAPI description is at `/v3/api-docs` (JSON) and `/v3/api-docs.yaml`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/pictures/{animal}?count=N` | Fetches `N` (1-10, default 1) random pictures and stores them. Returns their metadata. |
| `GET` | `/api/pictures/{animal}/latest` | The last stored picture of that animal, as an image. |
| `GET` | `/api/pictures/{animal}/latest/metadata` | Metadata (JSON) of that picture. |
| `GET` | `/api/pictures/{id}/image` | Any stored picture by id, as an image. |
| `GET` | `/api/pictures?animal=&page=0&size=24` | Stored pictures (metadata), newest first. `animal` is optional; `size` is 1-100. |
| `GET` | `/api/pictures/counts` | Number of stored pictures per animal, e.g. `{"cat":4,"dog":3,"bear":2,"total":9}`. |

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

The H2 console runs at http://localhost:8080/h2-console. Use JDBC URL `jdbc:h2:file:./data/animal-pictures;AUTO_SERVER=TRUE` (or `jdbc:h2:file:/app/data/animal-pictures;AUTO_SERVER=TRUE` in Docker), user `sa` and an empty password.

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
- **The gallery never loads image bytes to list pictures:** the list query selects only metadata into a `PictureSummary`, and each image is fetched by the browser on its own.
- **"Latest"** is the highest id for that animal. Ids always grow, so this is reliable even when two pictures share a timestamp.
- **Container:** the multi-stage Dockerfile builds with the same Maven Wrapper and runs the jar on a JRE-only image as a non-root user.

## What I would add with more time

- **Resilience:** retries with backoff and a circuit breaker per source (Resilience4j), and fetching several pictures in parallel.
- **Storage:** move image bytes to object storage (S3, MinIO) and keep only metadata in a PostgreSQL database for production use, with Flyway migrations instead of `ddl-auto`.
- **API:** deleting pictures and HTTP caching with ETags for stored images.
- **Operations:** Spring Boot Actuator health checks (also used as the Docker health check), metrics, and a Helm chart.
- **Tests:** contract tests against the real sources run separately from the unit tests, and a CI pipeline (GitHub Actions) that runs the tests and builds the image.
