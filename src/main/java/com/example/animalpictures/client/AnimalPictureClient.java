package com.example.animalpictures.client;

import com.example.animalpictures.picture.AnimalType;
import java.net.http.HttpClient;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Downloads a random picture of an animal from the configured source API. */
@Component
public class AnimalPictureClient {

    private final AnimalPicturesProperties properties;
    private final RestClient restClient;

    public AnimalPictureClient(AnimalPicturesProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "animal-pictures/1.0")
                .build();
    }

    public FetchedPicture fetch(AnimalType animalType) {
        String url = buildUrl(animalType);
        ResponseEntity<byte[]> response;
        try {
            response = restClient.get()
                    .uri(url)
                    .accept(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.ALL)
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (RestClientException e) {
            throw new PictureFetchException("Could not fetch a " + animalType.key() + " picture from " + url, e);
        }

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new PictureFetchException("The " + animalType.key() + " source returned an empty response: " + url);
        }
        MediaType declared = response.getHeaders().getContentType();
        String contentType = ImageTypes.detect(body, declared);
        if (contentType == null) {
            throw new PictureFetchException("The " + animalType.key() + " source did not return an image: " + url);
        }
        return new FetchedPicture(body, contentType, url);
    }

    String buildUrl(AnimalType animalType) {
        String template = properties.sources().get(animalType.key());
        if (template == null) {
            throw new IllegalStateException("No source configured for animal-pictures.sources." + animalType.key());
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int width = random.nextInt(properties.minSize(), properties.maxSize() + 1);
        int height = random.nextInt(properties.minSize(), properties.maxSize() + 1);
        return template.replace("{width}", String.valueOf(width)).replace("{height}", String.valueOf(height));
    }
}
