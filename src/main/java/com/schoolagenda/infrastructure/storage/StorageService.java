package com.schoolagenda.infrastructure.storage;

import java.io.InputStream;

public interface StorageService {

    String upload(String key, InputStream content, String contentType);

    void delete(String key);

}
