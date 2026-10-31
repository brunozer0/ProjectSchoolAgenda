package com.schoolagenda.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class StorageProperties {

    @Value("${gcp.storage.bucket}")
    private String bucket;
}
