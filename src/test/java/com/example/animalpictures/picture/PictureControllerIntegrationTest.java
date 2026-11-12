package com.example.animalpictures.picture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.animalpictures.client.AnimalPictureClient;
import com.example.animalpictures.client.FetchedPicture;
import com.example.animalpictures.client.PictureFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Full application with the real database (in-memory H2); only the internet-facing client is mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class PictureControllerIntegrationTest {

    private static final byte[] FIRST = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};
    private static final byte[] SECOND = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 2};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PictureRepository repository;

    @MockitoBean
    private AnimalPictureClient client;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void fetchesSavesAndReturnsTheLatestPicture() throws Exception {
        when(client.fetch(AnimalType.DOG)).thenReturn(
                new FetchedPicture(FIRST, "image/jpeg", "https://place.dog/200/200"),
                new FetchedPicture(SECOND, "image/jpeg", "https://place.dog/300/300"));

        mvc.perform(post("/api/pictures/dog").param("count", "2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].animal").value("dog"))
                .andExpect(jsonPath("$[0].contentType").value("image/jpeg"))
                .andExpect(jsonPath("$[1].sourceUrl").value("https://place.dog/300/300"));

        mvc.perform(get("/api/pictures/dog/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(SECOND));

        mvc.perform(get("/api/pictures/DOG/latest/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://place.dog/300/300"))
                .andExpect(jsonPath("$.sizeBytes").value(4));
    }

    @Test
    void latestIsKeptPerAnimal() throws Exception {
        when(client.fetch(AnimalType.CAT)).thenReturn(new FetchedPicture(FIRST, "image/jpeg", "https://cataas.com/cat"));
        when(client.fetch(AnimalType.BEAR)).thenReturn(new FetchedPicture(SECOND, "image/png", "https://placebear.com/1/1"));

        mvc.perform(post("/api/pictures/cat")).andExpect(status().isCreated());
        mvc.perform(post("/api/pictures/bear")).andExpect(status().isCreated());

        mvc.perform(get("/api/pictures/cat/latest"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FIRST));
    }

    @Test
    void servesAPictureById() throws Exception {
        when(client.fetch(AnimalType.CAT)).thenReturn(new FetchedPicture(FIRST, "image/jpeg", "https://cataas.com/cat"));
        String body = mvc.perform(post("/api/pictures/cat")).andReturn().getResponse().getContentAsString();
        String imageUrl = com.jayway.jsonpath.JsonPath.read(body, "$[0].imageUrl");

        mvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Picture-Id"))
                .andExpect(content().bytes(FIRST));
    }

    @Test
    void listsStoredPicturesNewestFirstWithPagination() throws Exception {
        when(client.fetch(AnimalType.DOG)).thenReturn(new FetchedPicture(FIRST, "image/jpeg", "https://place.dog/1/1"));
        when(client.fetch(AnimalType.CAT)).thenReturn(new FetchedPicture(SECOND, "image/png", "https://cataas.com/cat"));
        mvc.perform(post("/api/pictures/dog").param("count", "3")).andExpect(status().isCreated());
        mvc.perform(post("/api/pictures/cat").param("count", "2")).andExpect(status().isCreated());

        mvc.perform(get("/api/pictures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.items[0].animal").value("cat"))
                .andExpect(jsonPath("$.items[4].animal").value("dog"));

        mvc.perform(get("/api/pictures").param("animal", "dog").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].animal").value("dog"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/api/pictures/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cat").value(2))
                .andExpect(jsonPath("$.dog").value(3))
                .andExpect(jsonPath("$.bear").value(0))
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void listRejectsInvalidFilters() throws Exception {
        mvc.perform(get("/api/pictures").param("animal", "cow")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/pictures").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenNoPictureIsStored() throws Exception {
        mvc.perform(get("/api/pictures/bear/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Picture not found"));
    }

    @Test
    void returns400ForAnUnknownAnimal() throws Exception {
        mvc.perform(post("/api/pictures/cow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown animal type 'cow'. Supported values: cat, dog, bear"));
    }

    @Test
    void returns400ForACountOutOfRange() throws Exception {
        mvc.perform(post("/api/pictures/dog").param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("count: must be greater than or equal to 1"));
        mvc.perform(post("/api/pictures/dog").param("count", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("count: must be less than or equal to 10"));
        mvc.perform(post("/api/pictures/dog").param("count", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void returns502AndStoresNothingWhenTheSourceFails() throws Exception {
        when(client.fetch(any())).thenThrow(new PictureFetchException("Could not fetch a dog picture"));

        mvc.perform(post("/api/pictures/dog").param("count", "3"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Picture source unavailable"));

        mvc.perform(get("/api/pictures/dog/latest")).andExpect(status().isNotFound());
    }
}
