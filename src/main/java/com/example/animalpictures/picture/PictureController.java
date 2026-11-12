package com.example.animalpictures.picture;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pictures")
public class PictureController {

    private final PictureService service;

    public PictureController(PictureService service) {
        this.service = service;
    }

    /** Fetches {@code count} random pictures of the animal and stores them. */
    @PostMapping("/{animal}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PictureResponse> fetchAndSave(
            @PathVariable String animal,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10) int count) {
        return service.fetchAndSave(AnimalType.from(animal), count).stream()
                .map(PictureResponse::from)
                .toList();
    }

    /** The last stored picture of the animal, as an image. */
    @GetMapping("/{animal}/latest")
    public ResponseEntity<byte[]> latest(@PathVariable String animal) {
        return image(service.getLatest(AnimalType.from(animal)));
    }

    /** Metadata (JSON) of the last stored picture of the animal. */
    @GetMapping("/{animal}/latest/metadata")
    public PictureResponse latestMetadata(@PathVariable String animal) {
        return PictureResponse.from(service.getLatest(AnimalType.from(animal)));
    }

    /** A stored picture by id, as an image. */
    @GetMapping("/{id:\\d+}/image")
    public ResponseEntity<byte[]> imageById(@PathVariable long id) {
        return image(service.getById(id));
    }

    private static ResponseEntity<byte[]> image(Picture picture) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.getContentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Picture-Id", String.valueOf(picture.getId()))
                .body(picture.getImageData());
    }
}
