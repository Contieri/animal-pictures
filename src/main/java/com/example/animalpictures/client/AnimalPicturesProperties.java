package com.example.animalpictures.client;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code animal-pictures.*} in application.properties.
 *
 * @param sources  URL template per animal ("cat", "dog", "bear"); {width} and {height} are filled in randomly
 * @param minSize  smallest random width/height requested from the source
 * @param maxSize  largest random width/height requested from the source
 * @param timeout  connect and read timeout for the source APIs
 */
@ConfigurationProperties(prefix = "animal-pictures")
public record AnimalPicturesProperties(
        Map<String, String> sources, int minSize, int maxSize, Duration timeout) {
}
