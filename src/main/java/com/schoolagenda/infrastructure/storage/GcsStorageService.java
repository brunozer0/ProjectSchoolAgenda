package com.schoolagenda.infrastructure.storage;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;


@Service
public class GcsStorageService implements StorageService {

    private final Storage storage;

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    public GcsStorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public String upload(String key, InputStream content, String contentType) {

        BlobId blobId = BlobId.of(bucketName, key);

        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, content);

        return key;
    }

    @Override
    public void delete(String key) {
        storage.delete(bucketName, key);
    }
}
