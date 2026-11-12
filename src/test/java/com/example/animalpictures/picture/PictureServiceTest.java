package com.example.animalpictures.picture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.animalpictures.client.AnimalPictureClient;
import com.example.animalpictures.client.FetchedPicture;
import com.example.animalpictures.client.PictureFetchException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PictureServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private AnimalPictureClient client;
    private PictureRepository repository;
    private PictureService service;

    @BeforeEach
    void setUp() {
        client = mock(AnimalPictureClient.class);
        repository = mock(PictureRepository.class);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new PictureService(client, repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void fetchesAndStoresTheRequestedNumberOfPictures() {
        when(client.fetch(AnimalType.BEAR))
                .thenReturn(new FetchedPicture(new byte[] {1, 2, 3}, "image/jpeg", "https://placebear.com/200/300"));

        List<Picture> saved = service.fetchAndSave(AnimalType.BEAR, 3);

        assertThat(saved).hasSize(3).allSatisfy(picture -> {
            assertThat(picture.getAnimalType()).isEqualTo(AnimalType.BEAR);
            assertThat(picture.getContentType()).isEqualTo("image/jpeg");
            assertThat(picture.getSizeBytes()).isEqualTo(3);
            assertThat(picture.getCreatedAt()).isEqualTo(NOW);
        });
        verify(client, times(3)).fetch(AnimalType.BEAR);
    }

    @Test
    void storesNothingWhenTheSourceFails() {
        when(client.fetch(AnimalType.CAT))
                .thenReturn(new FetchedPicture(new byte[] {1}, "image/jpeg", "https://cataas.com/cat"))
                .thenThrow(new PictureFetchException("cataas is down"));

        assertThatThrownBy(() -> service.fetchAndSave(AnimalType.CAT, 2)).isInstanceOf(PictureFetchException.class);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void latestFailsWithAClearMessageWhenNothingIsStored() {
        when(repository.findFirstByAnimalTypeOrderByIdDesc(AnimalType.DOG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(AnimalType.DOG))
                .isInstanceOf(PictureNotFoundException.class)
                .hasMessageContaining("No dog picture stored yet");
    }
}
