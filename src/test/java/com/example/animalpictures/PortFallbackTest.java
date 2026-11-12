package com.example.animalpictures;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class PortFallbackTest {

    private final PortFallback fallback = new PortFallback();

    private static ServerSocket occupy() throws IOException {
        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(0)); // any free port, now taken by this test
        return socket;
    }

    private static StandardEnvironment environmentWith(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }

    @Test
    void movesToTheNextFreePortWhenTheConfiguredOneIsTaken() throws IOException {
        try (ServerSocket taken = occupy()) {
            int busyPort = taken.getLocalPort();
            StandardEnvironment environment = environmentWith(Map.of("server.port", busyPort));

            fallback.apply(environment);

            int chosen = environment.getProperty("server.port", Integer.class);
            assertThat(chosen).isGreaterThan(busyPort).isLessThanOrEqualTo(busyPort + 10);
            assertThat(PortFallback.isAvailable(chosen)).isTrue();
        }
    }

    @Test
    void keepsTheConfiguredPortWhenItIsFree() throws IOException {
        int freePort;
        try (ServerSocket probe = occupy()) {
            freePort = probe.getLocalPort();
        } // released again
        StandardEnvironment environment = environmentWith(Map.of("server.port", freePort));

        fallback.apply(environment);

        assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(freePort);
        assertThat(environment.getPropertySources().contains(PortFallback.PROPERTY_SOURCE_NAME)).isFalse();
    }

    @Test
    void doesNothingWhenDisabled() throws IOException {
        try (ServerSocket taken = occupy()) {
            StandardEnvironment environment = environmentWith(Map.of(
                    "server.port", taken.getLocalPort(),
                    "animal-pictures.port-fallback.enabled", "false"));

            fallback.apply(environment);

            assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(taken.getLocalPort());
        }
    }

    @Test
    void leavesRandomPortZeroAlone() {
        StandardEnvironment environment = environmentWith(Map.of("server.port", 0));

        fallback.apply(environment);

        assertThat(environment.getProperty("server.port", Integer.class)).isZero();
    }

    @Test
    void reportsWhenNoPortInTheRangeIsFree() throws IOException {
        try (ServerSocket taken = occupy()) {
            assertThat(PortFallback.findAvailablePort(taken.getLocalPort(), 1)).isEqualTo(-1);
        }
    }
}
