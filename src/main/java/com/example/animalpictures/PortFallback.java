package com.example.animalpictures;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * If the configured HTTP port (default 8080) is already taken, for example by another copy of this app,
 * moves to the next free port instead of failing with "Port 8080 was already in use".
 *
 * <p>Runs before the web server starts. Settings:
 * <ul>
 *   <li>{@code animal-pictures.port-fallback.enabled} (default {@code true})</li>
 *   <li>{@code animal-pictures.port-fallback.attempts}: how many ports to try after the configured one (default 10)</li>
 * </ul>
 */
public class PortFallback implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    static final String PROPERTY_SOURCE_NAME = "portFallback";
    private static final Logger log = LoggerFactory.getLogger(PortFallback.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        apply(event.getEnvironment());
    }

    /** Runs after Spring's logging is set up, so the warning below is printed normally. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    void apply(ConfigurableEnvironment environment) {
        if (!environment.getProperty("animal-pictures.port-fallback.enabled", Boolean.class, true)) {
            return;
        }
        int requested = environment.getProperty("server.port", Integer.class, 8080);
        if (requested <= 0) {
            return; // 0 means "any free port" and is handled by the web server itself
        }
        if (isAvailable(requested)) {
            return;
        }

        int attempts = environment.getProperty("animal-pictures.port-fallback.attempts", Integer.class, 10);
        int chosen = findAvailablePort(requested + 1, attempts);
        if (chosen < 0) {
            log.error("Port {} is already in use and ports {}-{} are taken too. Stop the other program or start "
                    + "with --server.port=<free port>.", requested, requested + 1, requested + attempts);
            return; // the web server will fail with its usual message
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of("server.port", chosen)));
        log.warn("Port {} is already in use (is another copy of this app running?). Using port {} instead.",
                requested, chosen);
    }

    /** @return the first free port in [from, from + attempts), or -1 when none is free */
    static int findAvailablePort(int from, int attempts) {
        for (int port = from; port < from + attempts && port <= 65535; port++) {
            if (isAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

    /** Tries to listen on the port on all interfaces, the same way Tomcat will. */
    static boolean isAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
