package com.example.animalpictures.picture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "picture", indexes = @Index(name = "idx_picture_animal_type", columnList = "animalType"))
public class Picture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnimalType animalType;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, length = 500)
    private String sourceUrl;

    @Column(nullable = false)
    private long sizeBytes;

    @Lob
    @Column(nullable = false)
    private byte[] imageData;

    @Column(nullable = false)
    private Instant createdAt;

    protected Picture() {
        // required by JPA
    }

    public Picture(AnimalType animalType, String contentType, String sourceUrl, byte[] imageData, Instant createdAt) {
        this.animalType = animalType;
        this.contentType = contentType;
        this.sourceUrl = sourceUrl;
        this.imageData = imageData;
        this.sizeBytes = imageData.length;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public AnimalType getAnimalType() {
        return animalType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
