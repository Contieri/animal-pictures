package com.example.animalpictures.picture;

import com.example.animalpictures.client.AnimalPictureClient;
import com.example.animalpictures.client.FetchedPicture;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PictureService {

    private final AnimalPictureClient client;
    private final PictureRepository repository;
    private final Clock clock;

    @Autowired
    public PictureService(AnimalPictureClient client, PictureRepository repository) {
        this(client, repository, Clock.systemUTC());
    }

    PictureService(AnimalPictureClient client, PictureRepository repository, Clock clock) {
        this.client = client;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Downloads {@code count} pictures and stores them. All pictures are downloaded first,
     * so a failing source stores nothing instead of leaving a partial batch.
     */
    @Transactional
    public List<Picture> fetchAndSave(AnimalType animalType, int count) {
        List<Picture> pictures = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            FetchedPicture fetched = client.fetch(animalType);
            pictures.add(new Picture(animalType, fetched.contentType(), fetched.sourceUrl(), fetched.data(),
                    Instant.now(clock)));
        }
        return repository.saveAll(pictures);
    }

    @Transactional(readOnly = true)
    public Picture getLatest(AnimalType animalType) {
        return repository.findFirstByAnimalTypeOrderByIdDesc(animalType)
                .orElseThrow(() -> new PictureNotFoundException(
                        "No " + animalType.key() + " picture stored yet. Fetch some first with POST /api/pictures/"
                                + animalType.key() + "?count=1"));
    }

    @Transactional(readOnly = true)
    public Picture getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PictureNotFoundException("No picture with id " + id));
    }
}
