package com.example.animalpictures.picture;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PictureRepository extends JpaRepository<Picture, Long> {

    /** The most recently stored picture of an animal (ids grow with every insert). */
    Optional<Picture> findFirstByAnimalTypeOrderByIdDesc(AnimalType animalType);

    /** Metadata of stored pictures, optionally for one animal, without loading the image bytes. */
    @Query(value = """
            select new com.example.animalpictures.picture.PictureSummary(
                p.id, p.animalType, p.contentType, p.sizeBytes, p.sourceUrl, p.createdAt)
            from Picture p
            where (:animalType is null or p.animalType = :animalType)
            """,
            countQuery = """
            select count(p) from Picture p
            where (:animalType is null or p.animalType = :animalType)
            """)
    Page<PictureSummary> findSummaries(@Param("animalType") AnimalType animalType, Pageable pageable);

    long countByAnimalType(AnimalType animalType);
}
