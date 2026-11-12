package com.example.animalpictures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Prints where the app can be opened, which matters when PortFallback moved it off port 8080. */
@Component
public class StartupBanner implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupBanner.class);

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        log.info("Snuggle Post is running at http://localhost:{} (API docs: http://localhost:{}/swagger-ui.html)",
                port, port);
    }
}
