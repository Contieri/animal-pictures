package com.example.animalpictures.picture;

import com.example.animalpictures.client.AnimalPictureClient;
import com.example.animalpictures.client.FetchedPicture;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    /** Stored pictures, newest first. {@code animalType} may be null for all animals. */
    @Transactional(readOnly = true)
    public Page<PictureSummary> list(AnimalType animalType, int page, int size) {
        return repository.findSummaries(animalType, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    /** How many pictures are stored per animal, plus the total. */
    @Transactional(readOnly = true)
    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0;
        for (AnimalType type : AnimalType.values()) {
            long count = repository.countByAnimalType(type);
            counts.put(type.key(), count);
            total += count;
        }
        counts.put("total", total);
        return counts;
    }

    @Transactional(readOnly = true)
    public Picture getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PictureNotFoundException("No picture with id " + id));
    }
}
