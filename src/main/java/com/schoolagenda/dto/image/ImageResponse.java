package com.schoolagenda.dto.image;

import com.schoolagenda.domain.entity.Image;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageResponse {

    private Long id;
    private String filename;
    private String contentType;
    private Long size;

    private String storageKey;
    private String url;

    public static ImageResponse from(Image image, String bucketName) {
        ImageResponse dto = new ImageResponse();
        dto.setId(image.getId());
        dto.setFilename(image.getFilename());
        dto.setContentType(image.getContentType());
        dto.setSize(image.getSize());
        dto.setStorageKey(image.getStorageKey());

        dto.setUrl(String.format(
                "https://storage.googleapis.com/%s/%s",
                bucketName,
                image.getStorageKey()
        ));

        return dto;
    }
}