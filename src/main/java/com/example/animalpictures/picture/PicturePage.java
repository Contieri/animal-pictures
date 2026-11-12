package com.example.animalpictures.picture;

import java.util.List;
import org.springframework.data.domain.Page;

/** One page of stored pictures, newest first. */
public record PicturePage(List<PictureResponse> items, int page, int size, long totalItems, int totalPages) {

    static PicturePage from(Page<PictureSummary> page) {
        return new PicturePage(
                page.getContent().stream().map(PictureResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
