package com.example.animalpictures.picture;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Pictures", description = "Fetch, store and browse pictures of cats, dogs and bears")
public class PictureController {

    private static final String ANIMAL_DESCRIPTION = "Animal type, case-insensitive";
    private static final String PROBLEM_JSON = "application/problem+json";

    private final PictureService service;

    public PictureController(PictureService service) {
        this.service = service;
    }

    @Operation(summary = "Fetch and store new pictures",
            description = "Downloads `count` random pictures of the animal from its public source and stores them. "
                    + "All pictures are downloaded before saving, so a failing source stores nothing.")
    @ApiResponse(responseCode = "201", description = "Pictures stored; returns their metadata")
    @ApiResponse(responseCode = "400", description = "Unknown animal, or count outside 1-10",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "The picture source failed; nothing was stored",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/{animal}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<PictureResponse> fetchAndSave(
            @Parameter(description = ANIMAL_DESCRIPTION, schema = @Schema(allowableValues = {"cat", "dog", "bear"}),
                    example = "dog")
            @PathVariable String animal,
            @Parameter(description = "How many pictures to fetch", example = "3")
            @RequestParam(defaultValue = "1") @Min(1) @Max(10) int count) {
        return service.fetchAndSave(AnimalType.from(animal), count).stream()
                .map(PictureResponse::from)
                .toList();
    }

    @Operation(summary = "List stored pictures",
            description = "Metadata of stored pictures, newest first, one page at a time. "
                    + "Leave `animal` empty to list every animal. Images are served by each item's `imageUrl`.")
    @ApiResponse(responseCode = "200", description = "One page of pictures")
    @ApiResponse(responseCode = "400", description = "Unknown animal, or page/size out of range",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PicturePage list(
            @Parameter(description = "Only this animal (optional)",
                    schema = @Schema(allowableValues = {"cat", "dog", "bear"}))
            @RequestParam(required = false) String animal,
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Pictures per page")
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        AnimalType animalType = (animal == null || animal.isBlank()) ? null : AnimalType.from(animal);
        return PicturePage.from(service.list(animalType, page, size));
    }

    @Operation(summary = "Count stored pictures", description = "Number of stored pictures per animal, plus the total.")
    @ApiResponse(responseCode = "200", description = "Counts per animal",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"cat\": 4, \"dog\": 3, \"bear\": 2, \"total\": 9}")))
    @GetMapping(value = "/counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> counts() {
        return service.counts();
    }

    @Operation(summary = "Get the last stored picture (image)",
            description = "Returns the image of the most recently stored picture of the animal.")
    @ApiResponse(responseCode = "200", description = "The picture",
            content = {@Content(mediaType = MediaType.IMAGE_JPEG_VALUE), @Content(mediaType = MediaType.IMAGE_PNG_VALUE)})
    @ApiResponse(responseCode = "404", description = "No picture of this animal stored yet",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{animal}/latest")
    public ResponseEntity<byte[]> latest(
            @Parameter(description = ANIMAL_DESCRIPTION, schema = @Schema(allowableValues = {"cat", "dog", "bear"}),
                    example = "dog")
            @PathVariable String animal) {
        return image(service.getLatest(AnimalType.from(animal)));
    }

    @Operation(summary = "Get the last stored picture (metadata)",
            description = "Metadata of the most recently stored picture of the animal: source, date, size and image URL.")
    @ApiResponse(responseCode = "200", description = "The picture's metadata")
    @ApiResponse(responseCode = "404", description = "No picture of this animal stored yet",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(value = "/{animal}/latest/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public PictureResponse latestMetadata(
            @Parameter(description = ANIMAL_DESCRIPTION, schema = @Schema(allowableValues = {"cat", "dog", "bear"}),
                    example = "dog")
            @PathVariable String animal) {
        return PictureResponse.from(service.getLatest(AnimalType.from(animal)));
    }

    @Operation(summary = "Get a stored picture by id (image)")
    @ApiResponse(responseCode = "200", description = "The picture",
            content = {@Content(mediaType = MediaType.IMAGE_JPEG_VALUE), @Content(mediaType = MediaType.IMAGE_PNG_VALUE)})
    @ApiResponse(responseCode = "404", description = "No picture with this id",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id:\\d+}/image")
    public ResponseEntity<byte[]> imageById(
            @Parameter(description = "Picture id", example = "1") @PathVariable long id) {
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
