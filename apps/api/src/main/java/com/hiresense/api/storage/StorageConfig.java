package com.hiresense.api.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public StorageService storageService(MinioClient client, StorageProperties properties) {
        ensureBucket(client, properties);
        return new MinioStorageService(client, properties.bucket());
    }

    private void ensureBucket(MinioClient client, StorageProperties properties) {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.bucket()).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.bucket()).build());
                log.info("Created storage bucket '{}'", properties.bucket());
            }
        } catch (Exception e) {
            throw new StorageException("Unable to ensure storage bucket exists", e);
        }
    }
}
