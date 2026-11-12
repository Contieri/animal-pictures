package com.example.animalpictures.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.animalpictures.picture.AnimalType;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the real HTTP client against a tiny local server, so no internet access is needed. */
class AnimalPictureClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // place.dog style: a PNG file sent with a JPEG content type
        server.createContext("/dog", exchange -> reply(exchange, 200, "image/jpeg", ImageTypesTest.PNG));
        // a redirect before the image, as some image services do
        server.createContext("/cat", exchange -> {
            exchange.getResponseHeaders().add("Location", "/cat-final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/cat-final", exchange -> reply(exchange, 200, "image/jpeg", ImageTypesTest.JPEG));
        server.createContext("/bear", exchange -> reply(exchange, 503, "text/plain", "down".getBytes()));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private AnimalPictureClient client(Map<String, String> sources) {
        return new AnimalPictureClient(new AnimalPicturesProperties(sources, 200, 300, Duration.ofSeconds(5)));
    }

    @Test
    void downloadsAPictureAndDetectsTheRealFormat() {
        AnimalPictureClient client = client(Map.of("dog", baseUrl + "/dog?w={width}&h={height}"));

        FetchedPicture picture = client.fetch(AnimalType.DOG);

        assertThat(picture.data()).isEqualTo(ImageTypesTest.PNG);
        assertThat(picture.contentType()).isEqualTo("image/png");
        assertThat(picture.sourceUrl()).matches(baseUrl + "/dog\\?w=\\d{3}&h=\\d{3}");
    }

    @Test
    void followsRedirects() {
        AnimalPictureClient client = client(Map.of("cat", baseUrl + "/cat"));

        assertThat(client.fetch(AnimalType.CAT).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void reportsAFailingSource() {
        AnimalPictureClient client = client(Map.of("bear", baseUrl + "/bear"));

        assertThatThrownBy(() -> client.fetch(AnimalType.BEAR))
                .isInstanceOf(PictureFetchException.class)
                .hasMessageContaining("bear");
    }

    @Test
    void fillsInRandomSizesWithinTheConfiguredRange() {
        AnimalPictureClient client = client(Map.of("bear", "https://example.test/{width}/{height}"));

        for (int i = 0; i < 50; i++) {
            String[] parts = client.buildUrl(AnimalType.BEAR).split("/");
            int width = Integer.parseInt(parts[3]);
            int height = Integer.parseInt(parts[4]);
            assertThat(width).isBetween(200, 300);
            assertThat(height).isBetween(200, 300);
        }
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String contentType,
            byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
