package com.example.animalpictures.picture;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PictureRepository extends JpaRepository<Picture, Long> {

    /** The most recently stored picture of an animal (ids grow with every insert). */
    Optional<Picture> findFirstByAnimalTypeOrderByIdDesc(AnimalType animalType);
}
